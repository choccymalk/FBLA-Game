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
import java.util.ArrayList;
import java.util.List;

public class titleScreen {
    // the class for the title screen
    JFrame frame;
    JPanel panel;
    JLabel title;
    JLabel startButton;
    JLabel exitButton;
    
    public titleScreen() {
        // constructor for the title screen
        frame = new JFrame("FBLA Game");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Color.BLACK);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        try {
            BufferedImage img = ImageIO.read(new File("app/src/main/resources/fbla/game/title.png"));
            title = new JLabel(new ImageIcon(img));
            title.setBounds(150, 50, 500, 200);
            title.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void show() {
        // method to show the title screen
        frame.add(panel);
        frame.setVisible(true);
        frame.repaint();
    }

    public void drawButtons() {
        // method to draw the buttons on the title screen
        startButton = new JLabel("Start Game");
        startButton.setBounds(frame.getWidth()/2, frame.getHeight()/2, 100, 50);
        startButton.setForeground(Color.WHITE);
        startButton.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(startButton);

        exitButton = new JLabel("Exit");
        exitButton.setBounds(350, 440, 100, 50);
        exitButton.setForeground(Color.WHITE);
        exitButton.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(exitButton);

        panel.repaint();
    }

    public void buttonActions() {
        // method to add actions to the buttons
        startButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                frame.dispose();
                startGame();
            }
        });

        exitButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                System.exit(0);
            }
        });
    }

    public void showTitleScreen() {
        // method to show the title screen with buttons and actions
        show();
        drawButtons();
        buttonActions();
    }

    public void startGame() {
        // method to start the game, destroy the title screen panel and run the game
        panel.setVisible(false);
        panel.removeAll();
        main game;
        try {
            game = new main();
            game.start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
