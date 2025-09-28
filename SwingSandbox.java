//package com.sandbox;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SwingSandbox {

    public static void main(String[] args) throws IOException {
        JFrame frame = buildFrame();

        final BufferedImage image = ImageIO.read(new File("ios_large_1662954661_image.jpg"));

        JPanel pane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, null);
            }
        };


        frame.add(pane);
    }

    public static void moveImage(JFrame frame, ImageIcon image, int x, int y) {
        // Assume the frame uses absolute positioning
        frame.setLayout(null);

        // Wrap the image in a JLabel
        JLabel label = new JLabel(image);

        // Set size of label equal to image size
        label.setBounds(x, y, image.getIconWidth(), image.getIconHeight());

        // Remove any previous labels holding this image
        frame.getContentPane().removeAll();
        
        // Add new label at new position
        frame.add(label);

        // Refresh UI
        frame.revalidate();
        frame.repaint();
    }

    private static JFrame buildFrame() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(200, 200);
        frame.setVisible(true);
        return frame;
    }


}