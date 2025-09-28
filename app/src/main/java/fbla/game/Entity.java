package fbla.game;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JLabel;
import java.awt.*;
import javax.swing.ImageIcon;


class Entity {
        BufferedImage image;
        int x;
        int y;
        double width;
        double height;

        public Entity(BufferedImage image, int x, int y, double width, double height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void draw(Graphics g, JPanel panel) {
            g.drawImage(image, x, y, (int) Math.round(width), (int) Math.round(height), null);
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() { return x; }
        public int getY() { return y; }
    }