package test;

import main.RoundBorder;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class RoundBorderTest {

    @Test
    public void testPaintBorderDoesNotThrow() {
        RoundBorder border = new RoundBorder(10);
        JPanel panel = new JPanel();
        // Create an offscreen image to obtain a Graphics context.
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();

        // Ensure that calling paintBorder does not throw an exception.
        assertDoesNotThrow(() -> border.paintBorder(panel, g, 0, 0, 100, 100));
        g.dispose();
    }
}
