package io.github.brickwall2900.jumpscare;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

// Inspiration
// https://steamcommunity.com/sharedfiles/filedetails/?id=3481943642
public class JumpscareApp {
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(JumpscareApp.class);

    public static int chance;
    public static Duration interval;
    public static Duration prepTime;
    public static Duration delayTime;

    static {
        try {
            reloadPreferences();
            System.out.printf("CHANCE: %d%n", chance);
            System.out.printf("INTERVAL: %s%n", interval);
            System.out.printf("PREP_TIME: %s%n", prepTime);
            System.out.printf("DELAY_TIME: %s%n", delayTime);
        } catch (BackingStoreException e) {
            e.printStackTrace();

            System.out.println("Loaded default preferences; backing store not available.");
            chance = 10000;
            interval = Duration.ofSeconds(1);
            prepTime = Duration.ofSeconds(5);
            delayTime = Duration.ofSeconds(10);
        }
    }

    private static int lastChance;
    private static Duration lastInterval, lastPrepTime, lastDelayTime;

    private static void reloadPreferences() throws BackingStoreException {
        PREFERENCES.sync();
        loadPreferences();
        reportPreferenceChange();
        savePreferences();
    }

    private static void savePreferences() {
        PREFERENCES.putInt("Chance", chance);
        PREFERENCES.putLong("IntervalSeconds", interval.toSeconds());
        PREFERENCES.putLong("PrepareSeconds", prepTime.toSeconds());
        PREFERENCES.putLong("DelaySeconds", delayTime.toSeconds());
    }

    private static void reportPreferenceChange() {
        if (lastChance != 0 && lastChance != chance) {
            System.out.printf("Updating CHANCE: %d -> %d%n",  lastChance, chance);
        }

        if (lastInterval != null && !lastInterval.equals(interval)) {
            System.out.printf("Updating INTERVAL: %s -> %s%n",  lastInterval, interval);
        }

        if (lastPrepTime != null && !lastPrepTime.equals(prepTime)) {
            System.out.printf("Updating PREP_TIME: %s -> %s%n",  lastPrepTime, prepTime);
        }

        if (lastDelayTime != null && !lastDelayTime.equals(delayTime)) {
            System.out.printf("Updating DELAY_TIME: %s -> %s%n",  lastDelayTime, delayTime);
        }

        lastChance = chance;
        lastInterval = interval;
        lastPrepTime = prepTime;
        lastDelayTime = delayTime;
    }

    private static void loadPreferences() {
        chance = Math.max(PREFERENCES.getInt("Chance", 10000), 1);
        interval = Duration.ofSeconds(Math.max(PREFERENCES.getLong("IntervalSeconds", 1), 1));
        prepTime = Duration.ofSeconds(Math.max(PREFERENCES.getLong("PrepareSeconds", 5), 0));
        delayTime = Duration.ofSeconds(Math.max(PREFERENCES.getLong("DelaySeconds", 10), 0));
    }

    static void main() {
        SecureRandom rand = new SecureRandom();

        try {
            while (true) {
                if (rand.nextInt(chance) == 0) {
                    try {
                        doJumpscare();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                try {
                    reloadPreferences();
                } catch (BackingStoreException e) {
                    e.printStackTrace();
                }
                Thread.sleep(interval);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Clip createClip() {
        try (InputStream stream = new BufferedInputStream(
                Objects.requireNonNull(JumpscareApp.class.getResourceAsStream("scream.wav")));
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

    private static void constructFrames(int frameCount, List<BufferedImage> images) throws IOException {
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

    static void doJumpscare() throws InterruptedException {
        try (Clip clip = createClip()) {
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

            Properties properties = new Properties();
            try (InputStream stream = JumpscareApp.class.getResourceAsStream("jumpscare.properties")) {
                properties.load(stream);
            }

            final int frameCount = properties.getProperty("frameCount") == null
                    ? 0 : Integer.parseInt(properties.getProperty("frameCount"));
            final double frameDelaySeconds = properties.getProperty("frameDelay") == null
                    ? 0.05 : Double.parseDouble(properties.getProperty("frameDelay"));
            Duration frameDelay = Duration.ofNanos((long) (frameDelaySeconds * 1e9));

            constructFrames(frameCount, images);

            Thread.sleep(prepTime);
            screenshots.putAll(screenshotMonitors(devices));

            clip.start();

            jumpscare(frameCount, windows, frameId, frameDelay);
            destroy(windows, images);

            Thread.sleep(delayTime);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
