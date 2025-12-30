package io.github.brickwall2900.jumpscare;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class CanvasPane extends JPanel {
    private Consumer<Graphics2D> painter;

    public Consumer<Graphics2D> getPainter() {
        return painter;
    }

    public void setPainter(Consumer<Graphics2D> painter) {
        this.painter = painter;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (painter != null) {
            painter.accept((Graphics2D) g);
        }
    }
}
