package fbla.game;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class main implements KeyListener {
    
    // Constants
    private static final int MOVE_AMOUNT = 5;
    private static final int TIMER_DELAY = 16; // ~60 FPS
    private static final int TYPEWRITER_DELAY = 75;
    private static final double NPC_INTERACTION_DISTANCE = 500.0; // Reduced for better gameplay
    private static final String RESOURCE_PATH = "C:\\Users\\Bentley\\Documents\\FBLA-game\\FBLA-Game\\game_resources";
    
    // UI Components
    private JFrame frame;
    private GamePanel panel;
    private JLabel currentMessageBox;
    private Timer gameTimer;
    private Timer typewriterTimer;
    
    // Game Resources
    private BufferedImage playerImage;
    private BufferedImage backgroundImage;
    private BufferedImage npcImage;
    private BufferedImage messageBoxImage;
    
    // Game State
    private final List<Entity> entities = new ArrayList<>();
    private final WavPlayer soundPlayer = new WavPlayer();
    private boolean messageBoxDisplayed = false;
    private int playerX = 50;
    private int playerY = 50;
    private int xVelocity = 0;
    private int yVelocity = 0;
    
    // NPC Messages
    private static final String[] NPC_MESSAGES = {
        "Hello there!",
        "Welcome to the FBLA game!",
        "Press E to interact with NPCs.",
        "Use arrow keys to move around.",
        "Press E to close message boxes.",
        "I have nothing else to say.",
        "Go explore the world!",
        "It's a beautiful day, isn't it?",
        "Did you know? FBLA teaches business skills!",
        "Keep exploring and learning!",
        "FBLA competitions are exciting!"
    };
    
    public main() throws IOException {
        loadResources();
        initializeEntities();
    }
    
    private void loadResources() throws IOException {
        Path resourcesPath = Paths.get(RESOURCE_PATH);
        
        try {
            playerImage = ImageIO.read(resourcesPath.resolve("textures/ios_large_1662954661_image.jpg").toFile());
            backgroundImage = ImageIO.read(resourcesPath.resolve("textures/background.jpg").toFile());
            npcImage = ImageIO.read(resourcesPath.resolve("textures/npc.png").toFile());
            messageBoxImage = ImageIO.read(resourcesPath.resolve("textures/message_box_bg.jpg").toFile());
        } catch (IOException e) {
            System.err.println("Failed to load game resources: " + e.getMessage());
            throw e;
        }
    }
    
    private void initializeEntities() {
        // Add player entity (always at index 0)
        entities.add(new Entity(playerImage, playerX, playerY, 
                               playerImage.getHeight(), playerImage.getHeight()));
        
        // Add NPC entities
        entities.add(new Entity(npcImage, 200, 150, npcImage.getWidth(), npcImage.getHeight()));
        //entities.add(new Entity(npcImage, 400, 300, npcImage.getWidth(), npcImage.getHeight()));
    }
    
    public void start() {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
            startGameLoop();
        });
    }
    
    private void createAndShowGUI() {
        frame = createMainFrame();
        panel = new GamePanel();
        
        frame.add(panel);
        panel.setFocusable(true);
        panel.requestFocusInWindow();
        panel.addKeyListener(this);
        
        frame.setVisible(true);
    }
    
    private JFrame createMainFrame() {
        JFrame frame = new JFrame("FBLA Educational Game");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
        return frame;
    }
    
    private void startGameLoop() {
        gameTimer = new Timer(TIMER_DELAY, e -> updateGame());
        gameTimer.start();
    }
    
    private void updateGame() {
        updatePlayerPosition();
        if (!messageBoxDisplayed) {
            panel.repaint();
        }
    }
    
    private void updatePlayerPosition() {
        playerX = Math.max(0, Math.min(playerX + xVelocity, 
                          panel.getWidth() - playerImage.getWidth()));
        playerY = Math.max(0, Math.min(playerY + yVelocity, 
                          panel.getHeight() - playerImage.getHeight()));
        
        entities.get(0).setPosition(playerX, playerY);
    }
    
    public void displayMessage(String message) {
        if (messageBoxDisplayed) return;
        
        messageBoxDisplayed = true;
        
        try {
            currentMessageBox = createMessageBox();
            JLabel messageLabel = createMessageLabel();
            currentMessageBox.add(messageLabel, BorderLayout.CENTER);
            
            addMessageBoxToFrame();
            startTypewriterEffect(messageLabel, message);
            
        } catch (Exception e) {
            System.err.println("Error displaying message: " + e.getMessage());
            messageBoxDisplayed = false;
        }
    }
    
    private JLabel createMessageBox() {
        JLabel messageBox = new JLabel(new ImageIcon(messageBoxImage));
        messageBox.setLayout(new BorderLayout());
        
        int boxWidth = frame.getWidth() / 2;
        int boxHeight = frame.getHeight() / 4;
        int boxX = (frame.getWidth() - boxWidth) / 2;
        int boxY = (int) (frame.getHeight() * 0.75);
        
        messageBox.setBounds(boxX, boxY, boxWidth, boxHeight);
        return messageBox;
    }
    
    private JLabel createMessageLabel() {
        JLabel messageLabel = new JLabel("", SwingConstants.CENTER);
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        return messageLabel;
    }
    
    private void addMessageBoxToFrame() {
        frame.getLayeredPane().add(currentMessageBox, JLayeredPane.POPUP_LAYER);
        frame.revalidate();
        frame.repaint();
    }
    
    private void startTypewriterEffect(JLabel messageLabel, String message) {
        if (typewriterTimer != null && typewriterTimer.isRunning()) {
            typewriterTimer.stop();
        }
        
        final int[] charIndex = {0};
        typewriterTimer = new Timer(TYPEWRITER_DELAY, null);
        
        typewriterTimer.addActionListener(e -> {
            if (charIndex[0] <= message.length()) {
                String displayText = message.substring(0, charIndex[0]);
                messageLabel.setText("<html><div style='text-align: center; padding: 10px;'>" 
                                   + displayText + "</div></html>");
                charIndex[0]++;
                
                if (charIndex[0] > message.length()) {
                    typewriterTimer.stop();
                }
            }
        });
        
        typewriterTimer.start();
    }
    
    public void closeMessage() {
        if (!messageBoxDisplayed) return;
        
        messageBoxDisplayed = false;
        
        if (typewriterTimer != null && typewriterTimer.isRunning()) {
            typewriterTimer.stop();
        }
        
        if (currentMessageBox != null) {
            frame.getLayeredPane().remove(currentMessageBox);
            currentMessageBox = null;
        }
        
        soundPlayer.stop();
        frame.revalidate();
        frame.repaint();
    }
    
    private void interactWithNearestNPC() {
        if (entities.size() < 2) return;
        
        Entity player = entities.get(0);
        Entity nearestNPC = findNearestNPC(player);
        
        if (nearestNPC != null) {
            String randomMessage = getRandomNPCMessage();
            playTalkingSound();
            displayMessage(randomMessage);
        }
    }
    
    private Entity findNearestNPC(Entity player) {
        Entity nearest = null;
        double minDistance = NPC_INTERACTION_DISTANCE;
        
        for (int i = 1; i < entities.size(); i++) {
            Entity npc = entities.get(i);
            double distance = calculateDistance(player, npc);
            
            if (distance < minDistance) {
                minDistance = distance;
                nearest = npc;
            }
        }
        
        return nearest;
    }
    
    private double calculateDistance(Entity a, Entity b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    private String getRandomNPCMessage() {
        int index = ThreadLocalRandom.current().nextInt(NPC_MESSAGES.length);
        return NPC_MESSAGES[index];
    }
    
    private void playTalkingSound() {
        try {
            Path soundPath = Paths.get(RESOURCE_PATH, "audio", "talking.wav");
            soundPlayer.play(soundPath.toString());
        } catch (Exception e) {
            System.err.println("Could not play talking sound: " + e.getMessage());
        }
    }
    
    // Custom JPanel for game rendering
    private class GamePanel extends JPanel {
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            // Enable antialiasing for smoother graphics
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw background
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            
            // Draw all entities
            for (Entity entity : entities) {
                entity.draw(g2d, this);
            }
        }
        
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(800, 600);
        }
    }
    
    // KeyListener implementations
    @Override
    public void keyPressed(KeyEvent e) {
        if (messageBoxDisplayed) {
            if (e.getKeyCode() == KeyEvent.VK_E) {
                closeMessage();
            }
            return;
        }
        
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                yVelocity = -MOVE_AMOUNT;
                break;
            case KeyEvent.VK_DOWN:
                yVelocity = MOVE_AMOUNT;
                break;
            case KeyEvent.VK_LEFT:
                xVelocity = -MOVE_AMOUNT;
                break;
            case KeyEvent.VK_RIGHT:
                xVelocity = MOVE_AMOUNT;
                break;
            case KeyEvent.VK_E:
                interactWithNearestNPC();
                break;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        if (messageBoxDisplayed) return;
        
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
                if ((e.getKeyCode() == KeyEvent.VK_UP && yVelocity < 0) ||
                    (e.getKeyCode() == KeyEvent.VK_DOWN && yVelocity > 0)) {
                    yVelocity = 0;
                }
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
                if ((e.getKeyCode() == KeyEvent.VK_LEFT && xVelocity < 0) ||
                    (e.getKeyCode() == KeyEvent.VK_RIGHT && xVelocity > 0)) {
                    xVelocity = 0;
                }
                break;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
    
    // Resource cleanup
    public void dispose() {
        if (gameTimer != null) {
            gameTimer.stop();
        }
        if (typewriterTimer != null) {
            typewriterTimer.stop();
        }
        soundPlayer.stop();
        
        if (frame != null) {
            frame.dispose();
        }
    }
    
    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            
            // Show title screen first
            titleScreen titleScreen = new titleScreen();
            titleScreen.showTitleScreen();
            
            // Game will be started from title screen
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start game: " + e.getMessage());
        }
    }
}