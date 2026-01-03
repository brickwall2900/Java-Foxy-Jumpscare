package io.github.brickwall2900.jumpscare;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

// Inspiration
// https://steamcommunity.com/sharedfiles/filedetails/?id=3481943642
public class JumpscareApp {
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(JumpscareApp.class);

    public record AppConfig(int chance,
                            Duration interval,
                            Duration prepTime,
                            Duration delayTime,
                            String defaultDefinition) {}

    private static AppConfig reloadPreferences(AppConfig lastConfig) throws BackingStoreException {
        AppConfig config;

        PREFERENCES.sync();
        config = loadPreferences();
        reportPreferenceChange(lastConfig, config);
        savePreferences(config);

        return config;
    }

    private static void savePreferences(AppConfig config) {
        PREFERENCES.putInt("Chance", config.chance);
        PREFERENCES.putLong("IntervalSeconds", config.interval.toSeconds());
        PREFERENCES.putLong("PrepareSeconds", config.prepTime.toSeconds());
        PREFERENCES.putLong("DelaySeconds", config.delayTime.toSeconds());
        PREFERENCES.put("DefaultDefinition", config.defaultDefinition);
    }

    private static void reportPreferenceChange(AppConfig lastConfig, AppConfig currentConfig) {
        if (lastConfig != null && lastConfig.chance != currentConfig.chance) {
            System.out.printf("Updating CHANCE: %d -> %d%n", lastConfig.chance, currentConfig.chance);
        }

        if (lastConfig != null && !lastConfig.interval.equals(currentConfig.interval)) {
            System.out.printf("Updating INTERVAL: %s -> %s%n", lastConfig.interval, currentConfig.interval);
        }

        if (lastConfig != null && !lastConfig.prepTime.equals(currentConfig.prepTime)) {
            System.out.printf("Updating PREP_TIME: %s -> %s%n", lastConfig.prepTime, currentConfig.prepTime);
        }

        if (lastConfig != null && !lastConfig.delayTime.equals(currentConfig.delayTime)) {
            System.out.printf("Updating DELAY_TIME: %s -> %s%n", lastConfig.delayTime, currentConfig.delayTime);
        }

        if (lastConfig != null && !lastConfig.defaultDefinition.equals(currentConfig.defaultDefinition)) {
            System.out.printf("Updating DEFAULT_DEFINITION: %s -> %s%n", lastConfig.defaultDefinition, currentConfig.defaultDefinition);
        }
    }

    private static AppConfig loadPreferences() {
        int chance = Math.max(PREFERENCES.getInt("Chance", 10000), 1);
        Duration interval = Duration.ofSeconds(Math.max(PREFERENCES.getLong("IntervalSeconds", 1), 1));
        Duration prepTime = Duration.ofSeconds(Math.max(PREFERENCES.getLong("PrepareSeconds", 5), 0));
        Duration delayTime = Duration.ofSeconds(Math.max(PREFERENCES.getLong("DelaySeconds", 10), 0));
        String defaultDefinition = PREFERENCES.get("DefaultDefinition", "");

        return new AppConfig(chance, interval, prepTime, delayTime, defaultDefinition);
    }

    static void main(String[] args) {
        String providedPropertyFile = null;
        if (args.length > 0) {
            providedPropertyFile = args[0];
        }

        SecureRandom rand = new SecureRandom();
        AppConfig config;

        try {
            config = reloadPreferences(null);
            System.out.printf("CHANCE: %d%n", config.chance);
            System.out.printf("INTERVAL: %s%n", config.interval);
            System.out.printf("PREP_TIME: %s%n", config.prepTime);
            System.out.printf("DELAY_TIME: %s%n", config.delayTime);
            System.out.printf("DEFAULT_DEFINITION: %s%n", config.defaultDefinition);
        } catch (BackingStoreException e) {
            e.printStackTrace();

            System.out.println("Loaded default preferences; backing store not available.");
            config = new AppConfig(10000, Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(10), "");
        }

        try {
            while (true) {
                if (rand.nextInt(config.chance) == 0) {
                    try {
                        doJumpscare(config, rand, providedPropertyFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                try {
                    config = reloadPreferences(config);
                } catch (BackingStoreException e) {
                    e.printStackTrace();
                }
                Thread.sleep(config.interval);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void doJumpscare(AppConfig config, Random random, String providedPropertyFile) throws Exception {
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        List<JWindow> windows = new ArrayList<>();

        int[] frameId = new int[]{0};
        List<BufferedImage> images = new ArrayList<>();
        Map<GraphicsDevice, BufferedImage> screenshots = new HashMap<>();

        for (GraphicsDevice device : devices) {
            JWindow frame = createWindow(device);
            windows.add(frame);

            CanvasPane canvas = new CanvasPane();
            frame.getContentPane().add(canvas);

            Consumer<Graphics2D> painter = createPainter(device, screenshots, canvas, frame, images, frameId);
            canvas.setPainter(painter);
        }

        Properties customJumpscare = getCustomJumpscareConfig(config, providedPropertyFile);
        JumpscareDefinition definition = null;
        if (customJumpscare != null) {
            Map<String, JumpscareDefinition> definitions = parseJumpscareConfig(customJumpscare);
            List<JumpscareDefinition> defList = new ArrayList<>(List.copyOf(definitions.values()));
            Collections.shuffle(defList, random);
            double weightSum = defList.stream()
                    .mapToDouble(x -> x.weight)
                    .filter(x -> x > 0)
                    .sum();
            double generatedWeight = random.nextDouble(weightSum);

            if (weightSum <= 0) {
                throw new IllegalStateException("All jumpscare weights are zero or invalid");
            }

            for (JumpscareDefinition def : defList) {
                generatedWeight -= def.weight;
                if (generatedWeight <= 0.0) {
                    definition = def;
                    break;
                }
            }
            if (definition == null) {
                definition = defList.get(random.nextInt(defList.size()));
            }
        }

        int frameCount;
        Duration frameDelay;
        Clip clip;

        if (definition == null) {
            Properties properties = new Properties();
            try (InputStream stream = JumpscareApp.class.getResourceAsStream("jumpscare.properties")) {
                properties.load(stream);
            }
            frameCount = properties.getProperty("frameCount") == null
                    ? 0 : Integer.parseInt(properties.getProperty("frameCount"));
            double frameDelaySeconds = properties.getProperty("frameDelay") == null
                    ? 0.05 : Double.parseDouble(properties.getProperty("frameDelay"));
            frameDelay = Duration.ofNanos((long) (frameDelaySeconds * 1e9));

            constructDefaultFrames(frameCount, images);
        } else {
            JumpscareParseInfo parseInfo = constructFrames(definition, images);
            frameCount = parseInfo.frameCount;
            frameDelay = parseInfo.frameDelay;
        }
        clip = createClip(definition);

        Thread.sleep(config.prepTime);
        screenshots.putAll(screenshotMonitors(devices));

        clip.start();

        jumpscare(frameCount, windows, frameId, frameDelay);
        destroy(windows, images);

        Thread.sleep(config.delayTime);
        clip.stop();
        clip.close();
    }

    private static final class JumpscareDefinition {
        private Duration frameDelay;
        private String framePath;
        private String frameType;
        private String sound;
        private double weight;
    }

    private record JumpscareParseInfo(int frameCount, Duration frameDelay) {}

    private static JumpscareParseInfo constructFrames(JumpscareDefinition definition, List<BufferedImage> images) throws IOException {
        // read files
        Path framePath = Path.of(definition.framePath);
        if (!Files.exists(framePath)) {
            throw new FileNotFoundException("Frame path not found: " + framePath);
        }

        List<Path> fileList;
        try (Stream<Path> stream = Files.list(framePath)) {
            fileList = new ArrayList<>(stream.toList());
        }
        Collections.sort(fileList);

        int frameCount = 0;

        for (Path path : fileList) {
            if (path.getFileName().toString().toLowerCase().endsWith("." + definition.frameType)) {
                frameCount++;
                try (InputStream stream = Files.newInputStream(path)) {
                    images.add(ImageIO.read(Objects.requireNonNull(stream)));
                }
            }
        }

        return new JumpscareParseInfo(frameCount, definition.frameDelay);
    }

    private static Map<String, JumpscareDefinition> parseJumpscareConfig(Properties properties) {
        Map<String, JumpscareDefinition> definitions = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            int period = key.indexOf('.');
            String name = key.substring(0, period);
            JumpscareDefinition def = definitions.computeIfAbsent(name, x -> new JumpscareDefinition());

            String property = key.substring(period + 1);
            String value = properties.getProperty(key);
            switch (property) {
                case "frameDelay" -> def.frameDelay = Duration.ofNanos((long) (Double.parseDouble(value) * 1e9));
                case "framePath" -> def.framePath = value;
                case "frameType" -> def.frameType = value;
                case "sound" -> def.sound = value;
                case "weight" -> def.weight = Double.parseDouble(value);
                case null, default -> throw new IllegalArgumentException("Unknown property: " + property);
            }
        }

        for (Map.Entry<String, JumpscareDefinition> entry : definitions.entrySet()) {
            String name = entry.getKey();
            JumpscareDefinition def = entry.getValue();
            List<String> notDefined = new ArrayList<>();
            if (def.frameDelay == null) notDefined.add("frameDelay");
            if (def.framePath == null) notDefined.add("framePath");
            if (def.frameType == null) notDefined.add("frameType");
            if (def.sound == null) notDefined.add("sound");
            if (!notDefined.isEmpty()) {
                throw new NullPointerException("Error in %1$s while parsing jumpscare.properties: " +
                        "properties %2$s not defined.".formatted(name, notDefined));
            }
        }

        return definitions;
    }

    private static Properties getCustomJumpscareConfig(AppConfig config, String providedFile) throws IOException {
        // 1. command line argument
        if (providedFile != null) {
            Properties properties = new Properties();
            Path path = Path.of(providedFile);
            if (Files.exists(path)) {
                try (InputStream stream = Files.newInputStream(path, StandardOpenOption.READ)) {
                    properties.load(stream);
                }
            }
            return properties;
        }

        // 2. system properties
        if (System.getProperty("jumpscare.definitions") != null) {
            Path path = Path.of(System.getProperty("jumpscare.definitions"));
            if (Files.exists(path)) {
                Properties properties = new Properties();
                try (InputStream stream = Files.newInputStream(path, StandardOpenOption.READ)) {
                    properties.load(stream);
                }
                return properties;
            }
        }

        // 3. jumpscare.properties
        Path path = Path.of("jumpscare.properties");
        if (Files.exists(path)) {
            Properties properties = new Properties();
            try (InputStream stream = Files.newInputStream(path, StandardOpenOption.READ)) {
                properties.load(stream);
            }
            return properties;
        }

        // 4. config
        if (config.defaultDefinition != null && !config.defaultDefinition.isEmpty()) {
            path = Path.of(config.defaultDefinition);
            if (Files.exists(path)) {
                Properties properties = new Properties();
                try (InputStream stream = Files.newInputStream(path, StandardOpenOption.READ)) {
                    properties.load(stream);
                }
                return properties;
            }
        }

        // use default
        return null;
    }

    private static Clip createClip(JumpscareDefinition definition) {
        try (InputStream stream = new BufferedInputStream(
                definition != null
                        ? Files.newInputStream(Path.of(definition.sound))
                        : Objects.requireNonNull(JumpscareApp.class.getResourceAsStream("scream.wav")));
             AudioInputStream audioStream = AudioSystem.getAudioInputStream(stream)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            FloatControl volume = ((FloatControl) (clip.getControl(FloatControl.Type.MASTER_GAIN)));
            volume.setValue(volume.getMaximum());
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<GraphicsDevice, BufferedImage> screenshotMonitors(GraphicsDevice[] devices) {
        Map<GraphicsDevice, BufferedImage> screenshots = new HashMap<>();
        for (GraphicsDevice device : devices) {
            GraphicsConfiguration gc = device.getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            try {
                Robot robot = new Robot();

                BufferedImage screenshot = robot.createScreenCapture(new Rectangle((int) bounds.getMinX(),
                        (int) bounds.getMinY(), (int) bounds.getWidth(), (int) bounds.getHeight()));
                screenshots.put(device, screenshot);
            } catch (AWTException e) {
                e.printStackTrace();
                BufferedImage blank = gc.createCompatibleImage((int) bounds.getWidth(), (int) bounds.getHeight());
                screenshots.put(device, blank);
            }
        }
        return screenshots;
    }

    private static JWindow createWindow(GraphicsDevice device) {
        DisplayMode displayMode = device.getDisplayMode();
        GraphicsConfiguration gc = device.getDefaultConfiguration();

        AffineTransform transform = gc.getDefaultTransform();

        double scaleX = transform.getScaleX();
        double scaleY = transform.getScaleY();

        JWindow window = new JWindow(gc);
        window.setAlwaysOnTop(true);
        window.setLocation(gc.getBounds().x, gc.getBounds().y);
        window.setSize((int) (displayMode.getWidth() / scaleX), (int) (displayMode.getHeight() / scaleY));
        return window;
    }

    private static Consumer<Graphics2D> createPainter(GraphicsDevice device, Map<GraphicsDevice, BufferedImage> screenshots, CanvasPane canvas, JWindow frame, List<BufferedImage> images, int[] frameId) {
        return g2d -> {
            BufferedImage screenshot = screenshots.get(device);
            if (screenshot != null) {
                g2d.drawImage(screenshot, 0, 0, canvas.getWidth(), canvas.getHeight(), frame);
            }
            g2d.drawImage(images.get(frameId[0]), 0, 0, canvas.getWidth(), canvas.getHeight(), frame);
        };
    }

    private static void constructDefaultFrames(int frameCount, List<BufferedImage> images) throws IOException {
        for (int i = 0; i < frameCount; i++) {
            try (InputStream stream = JumpscareApp.class.getResourceAsStream("frames/frame_" + i + ".png")) {
                images.add(ImageIO.read(Objects.requireNonNull(stream)));
            }
        }
    }

    private static void destroy(List<JWindow> windows, List<BufferedImage> images) {
        for (JWindow window : windows) {
            window.dispose();
            for (Component c : window.getComponents()) {
                if (c instanceof CanvasPane canvas) {
                    canvas.setPainter(null);
                }
            }
            window.removeAll();
        }
        for (BufferedImage image : images) {
            image.flush();
        }
        windows.clear();
        images.clear();
    }

    private static void jumpscare(int frameCount, List<JWindow> windows, int[] frameId, Duration frameDelay) throws InterruptedException {
        for (int i = 0; i < frameCount; i++) {
            for (JWindow window : windows) {
                window.setVisible(true);
                window.requestFocus();
                window.setAlwaysOnTop(true);
                window.repaint();
            }
            frameId[0] = i;

            Thread.sleep(frameDelay);
        }
    }
}
