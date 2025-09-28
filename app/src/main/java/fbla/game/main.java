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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

public class main implements KeyListener {

    JFrame frame;
    JPanel panel;
    BufferedImage image;
    BufferedImage background;
    BufferedImage npc;
    int imageX = 50;
    int imageY = 50;
    int xVelocity = 0;
    int yVelocity = 0;
    double movingImageWidth;
    double movingImageHeight;
    boolean messageBoxBeingDisplayed = false;
    List<Entity> entities = new ArrayList<>();
    WavPlayer soundPlayer = new WavPlayer();

    File movingImage = new File(
            "game_resources\\textures\\ios_large_1662954661_image.jpg");
    File backgroundImage = new File(
            "game_resources\\textures\\background.jpg");
    File messageBoxImage = new File(
            "game_resources\\textures\\message_box_bg.jpg");
    File npcImage = new File("C:\\Users\\Bentley\\Documents\\FBLA-game\\game_resources\\textures\\npc.png");

    String[] defaultNPCMessages = {
            "Hello there!",
            "Welcome to the game.",
            "Press E to see a message box.",
            "Use arrow keys to move around.",
            "Press E to close message boxes.",
            "I have nothing to say to you.",
            "Go away.",
            "It's a nice day, isn't it?",
            "Did you know? The Earth revolves around the Sun.",
            "Keep exploring!",
            "I love FBLA!"
    };

    public main() {
        try {
            image = ImageIO.read(movingImage);
            movingImageWidth = image.getWidth();
            movingImageHeight = image.getHeight();
            background = ImageIO.read(backgroundImage);
            npc = ImageIO.read(npcImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void displayMessageBoxWithMessage(String message) {
        // the box should be added at the top layer of the JFrame
        try {
            messageBoxBeingDisplayed = true;
            JLabel messageBox = new JLabel(new ImageIcon(ImageIO.read(messageBoxImage)));
            messageBox.setLayout(new BorderLayout());
            messageBox.setBounds(frame.getWidth() / 4, frame.getHeight() / 4, frame.getWidth() / 2,
                    frame.getHeight() / 4);
            JLabel messageLabel = new JLabel(message, SwingConstants.CENTER);
            messageLabel.setForeground(Color.WHITE);
            messageBox.add(messageLabel, BorderLayout.CENTER);
            frame.add(messageBox);
            frame.setComponentZOrder(messageBox, 0); // Bring to front
            frame.revalidate();
            frame.repaint();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeAllMessageBoxes() {
        messageBoxBeingDisplayed = false;
        Component[] components = frame.getContentPane().getComponents();
        for (Component component : components) {
            if (component instanceof JLabel) {
                frame.remove(component);
            }
        }
        frame.revalidate();
        frame.repaint();
    }

    public void talkToClosestNPC() {
        if (entities.size() < 2) {
            System.out.println("No NPCs to talk to.");
            return;
        }

        Entity player = entities.get(0);
        Entity closestNPC = null;
        double closestDistance = Double.MAX_VALUE;

        for (int i = 1; i < entities.size(); i++) {
            Entity npc = entities.get(i);
            double distance = Math
                    .sqrt(Math.pow(player.getX() - npc.getX(), 2) + Math.pow(player.getY() - npc.getY(), 2));
            if (distance < closestDistance) {
                closestDistance = distance;
                closestNPC = npc;
            }
        }

        if (closestNPC != null && closestDistance < 500) { // arbitrary distance threshold
            int randomIndex = (int) (Math.random() * defaultNPCMessages.length);
            soundPlayer.play("game_resources\\audio\\talking.wav");
            displayMessageBoxWithMessage(defaultNPCMessages[randomIndex]);
        } else {
            System.out.println("No NPCs nearby to talk to.");
        }
    }

    // draws a grid on the background image for debugging purposes
    // each entity can update its position in increments of 10
    public void drawGrid() {
        Graphics g = background.getGraphics();
        g.setColor(Color.LIGHT_GRAY);
        for (int x = 0; x < background.getWidth(); x += 10) {
            g.drawLine(x, 0, x, background.getHeight());
        }
        for (int y = 0; y < background.getHeight(); y += 10) {
            g.drawLine(0, y, background.getWidth(), y);
        }
        g.dispose();
    }

    public void addEntity(BufferedImage image, int x, int y, double width, double height) {
        Entity entity = new Entity(image, x, y, width, height);
        entities.add(entity);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        int moveAmount = 5; // Reduced move amount for smoother animation

        if (keyCode == KeyEvent.VK_UP && !messageBoxBeingDisplayed) {
            yVelocity = -moveAmount;
        } else if (keyCode == KeyEvent.VK_DOWN && !messageBoxBeingDisplayed) {
            yVelocity = moveAmount;
        } else if (keyCode == KeyEvent.VK_LEFT && !messageBoxBeingDisplayed) {
            xVelocity = -moveAmount;
        } else if (keyCode == KeyEvent.VK_RIGHT && !messageBoxBeingDisplayed) {
            xVelocity = moveAmount;
        } else if (keyCode == KeyEvent.VK_E && !messageBoxBeingDisplayed) {
            talkToClosestNPC();
        } else if (keyCode == KeyEvent.VK_E && messageBoxBeingDisplayed) {
            closeAllMessageBoxes();
            soundPlayer.stop();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_UP && yVelocity < 0 && !messageBoxBeingDisplayed) {
            yVelocity = 0;
        } else if (keyCode == KeyEvent.VK_DOWN && yVelocity > 0 && !messageBoxBeingDisplayed) {
            yVelocity = 0;
        } else if (keyCode == KeyEvent.VK_LEFT && xVelocity < 0 && !messageBoxBeingDisplayed) {
            xVelocity = 0;
        } else if (keyCode == KeyEvent.VK_RIGHT && xVelocity > 0 && !messageBoxBeingDisplayed) {
            xVelocity = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) throws IOException {
        main example = new main();
        example.start();
    }

    private void start() {
        frame = buildFrame();
        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(background, 0, 0, this.getWidth(), this.getHeight(), this); // Draw background
                // g.drawImage(npc, 10, 10, (int)Math.round(npc.getWidth()),
                // (int)Math.round(npc.getHeight()), null);
                // g.drawImage(image, imageX, imageY, (int)Math.round(movingImageHeight),
                // (int)Math.round(movingImageHeight), null);
                for (Entity entity : entities) {
                    entity.draw(g, this);
                }
            }
        };

        // the player entity should always be entity 0
        addEntity(image, imageX, imageY, movingImageHeight, movingImageHeight);
        addEntity(npc, 100, 100, npc.getWidth(), npc.getHeight());

        frame.add(panel);
        panel.setFocusable(true);
        panel.requestFocusInWindow();
        panel.addKeyListener(this);

        Timer timer = new Timer(20, e -> { // Adjust the delay (20 ms) for smoother animation
            imageX += xVelocity;
            imageY += yVelocity;
            entities.get(0).setPosition(imageX, imageY);

            // Keep image within bounds
            if (imageX < 0) {
                imageX = 0;
                entities.get(0).setPosition(imageX, imageY);
            }

            if (imageY < 0) {
                imageY = 0;
                entities.get(0).setPosition(imageX, imageY);
            }

            if (imageX > panel.getWidth() - movingImageWidth) {
                imageX = panel.getWidth() - (int) Math.round(movingImageWidth);
                entities.get(0).setPosition(imageX, imageY);
            }

            if (imageY > panel.getHeight() - movingImageHeight) {
                imageY = panel.getHeight() - (int) Math.round(movingImageHeight);
                entities.get(0).setPosition(imageX, imageY);
            }

            if (!messageBoxBeingDisplayed) {
                // don't repaint the panel if a message box is being displayed
                panel.repaint();
            }
            //System.out.println("player position: (" + entities.get(0).getX() + ", " + entities.get(0).getY() + ")"); // Debugging
                                                                                                                     // output
            //System.out.println("x velocity: " + xVelocity + ", y velocity: " + yVelocity);
        });
        timer.start();
        // drawGrid();

        frame.setVisible(true);
    }

    private JFrame buildFrame() {
        JFrame frame = new JFrame("FBLA Game");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        // frame.setSize(400, 400);
        // frame.getContentPane().setPreferredSize(new Dimension(400, 400));
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        return frame;
    }
}