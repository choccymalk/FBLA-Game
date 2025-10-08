package fbla.game;
/*
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
    List<Level> levels = new ArrayList<>();
    int currentLevelIndex = 0;

    File movingImage = new File(
            System.getenv("LOCALAPPDATA")+"\\FBLA-Game\\game_resources\\textures\\ios_large_1662954661_image.jpg");
    File backgroundImage = new File(
            System.getenv("LOCALAPPDATA")+"\\FBLA-Game\\game_resources\\textures\\background.jpg");
    File messageBoxImage = new File(
            System.getenv("LOCALAPPDATA")+"\\FBLA-Game\\game_resources\\textures\\message_box_bg.jpg");
    File npcImage = new File(System.getenv("LOCALAPPDATA")+"\\FBLA-Game\\game_resources\\textures\\npc.png");

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
            soundPlayer.play(System.getenv("LOCALAPPDATA")+"\\FBLA-Game\\game_resources\\audio\\talking.wav");
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
        Entity entity = new Entity(image, x, y, width, height, null);
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
}*/
// File: LwjglGame.java
// Single-file migration of your JFrame game to LWJGL (GLFW + OpenGL).
// Note: requires LWJGL (glfw, opengl, stb) on the classpath with natives.

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.lwjgl.stb.STBImage;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.Timer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import fbla.game.*;

public class main {
    // === Config ===
    private static final int WINDOW_W = 1272;
    private static final int WINDOW_H = 720;
    private static final int MOVE_AMOUNT = 24;
    public static final int MOVEMENT_DELAY_MS = 100; // seconds between moves
    private static final int TYPEWRITER_DELAY_MS = 75;
    private static final double NPC_INTERACTION_DISTANCE = 500.0;
    private static final String RESOURCE_PATH = System.getenv("LOCALAPPDATA")+"\\FBLA-Game\\game_resources";
    private double FRAMERATE = 24.0;
    private static final int GRID_CELL_SIZE = 24; // each cell in the grid is 24x24 pixels
    private static final int GRID_WIDTH = 53; // number of cells in the grid horizontally
    private static final int GRID_HEIGHT = 30; // number of cells in the grid vertically 
    private static final int ENTITY_WIDTH_CELLS = 3; // all entities are 3x5 cells
    private static final int ENTITY_HEIGHT_CELLS = 5;
    private static final int DOOR_WIDTH = 96;
    private static final int DOOR_HEIGHT = 144;

    // === Game state ===
    private long window;
    private int winW = WINDOW_W, winH = WINDOW_H;

    private final List<Entity> entities = new ArrayList<>();
    private BufferedImage playerBI, backgroundBI, npcBI, messageBoxBI, gridBI, doorBI;
    private int playerTex, backgroundTex, npcTex, messageBoxTex, gridTex, doorTex;

    private int playerX = 0, playerY = 600;
    private int xVelocity = 0, yVelocity = 0;
    private boolean messageBoxDisplayed = false;

    private final WavPlayer soundPlayer = new WavPlayer();

    jsonParser parser = new jsonParser(new File(RESOURCE_PATH+"\\levels.json"));

    // get collision grid for the current level fron parser, for right now, we will only use level 0
    int[][] collisionGrid = parser.getCollisionGrid(0);

    // Typewriter state
    private String currentFullMessage = "";
    private int typeIndex = 0;
    private long lastTypeTime = 0;
    private BufferedImage messageTextureImage; // combined BG + text
    private int messageTextureGL = -1;

    private static final String[] NPC_MESSAGES = {
            "I have nothing to say to you."
    };

    public static void main(String[] args) throws Exception {
        new main().run();
    }

    public void run() throws Exception {
        init();
        buildLevel(0);
        loop();
        cleanup();
    }

    private void init() throws IOException {
        // Initialize GLFW
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW window
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(winW, winH, "FBLA Educational Game (LWJGL)", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create GLFW window");

        // Set up resize callback
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            winW = w;
            winH = h;
            glViewport(0, 0, w, h);
        });

        // Input callbacks
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) keyPressed(key);
            if (action == GLFW_RELEASE) keyReleased(key);
        });

        // Center window
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidmode != null) {
                glfwSetWindowPos(
                        window,
                        (vidmode.width() - pWidth.get(0)) / 2,
                        (vidmode.height() - pHeight.get(0)) / 2
                );
            }
        }

        // Make OpenGL context current and show
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // VSync
        glfwShowWindow(window);
        GL.createCapabilities();

        // Setup orthographic 2D projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, winW, winH, 0, -1, 1); // top-left origin
        glMatrixMode(GL_MODELVIEW);

        // Enable textures and alpha blending
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Load images (using AWT for easier text overlay creation)
        loadResources();

        // Upload textures to OpenGL
        backgroundTex = createTextureFromBufferedImage(backgroundBI);
        playerTex = createTextureFromBufferedImage(playerBI);
        npcTex = createTextureFromBufferedImage(npcBI);
        messageBoxTex = createTextureFromBufferedImage(messageBoxBI);
        gridTex = createTextureFromBufferedImage(gridBI);
        doorTex = createTextureFromBufferedImage(doorBI);

        // Entities: index 0 is player
        entities.add(new Entity(null, playerTex, playerX, playerY, playerBI.getWidth(), playerBI.getHeight()));
        entities.add(new Entity(null, npcTex, 200, 150, npcBI.getWidth(), npcBI.getHeight()));

        // Prepare message overlay buffer image (same size as message box) but will update later
        messageTextureImage = new BufferedImage(messageBoxBI.getWidth(), messageBoxBI.getHeight(), BufferedImage.TYPE_INT_ARGB);
        // Create a GL texture placeholder for the message box text overlay
        messageTextureGL = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, messageTextureGL);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        // Allocate empty image
        ByteBuffer empty = memAlloc(messageBoxBI.getWidth() * messageBoxBI.getHeight() * 4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, messageBoxBI.getWidth(), messageBoxBI.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, empty);
        memFree(empty);
    }

    private void loadResources() throws IOException {
        Path resourcesPath = Paths.get(RESOURCE_PATH);

        try {
            playerBI = ImageIO.read(resourcesPath.resolve("textures/player.png").toFile());
            backgroundBI = ImageIO.read(resourcesPath.resolve("textures/background1.png").toFile());
            npcBI = ImageIO.read(resourcesPath.resolve("textures/npc.png").toFile());
            messageBoxBI = ImageIO.read(resourcesPath.resolve("textures/message_box_bg.jpg").toFile());
            gridBI = ImageIO.read(resourcesPath.resolve("textures/grid_overlay.png").toFile());
            doorBI = ImageIO.read(resourcesPath.resolve("textures/door.png").toFile());
        } catch (IOException e) {
            System.err.println("Failed to load game resources: " + e.getMessage());
            throw e;
        }
    }

    private int createTextureFromBufferedImage(BufferedImage img) {
        int[] pixels = new int[img.getWidth() * img.getHeight()];
        img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

        ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);

        // ARGB to RGBA
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int pixel = pixels[y * img.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                buffer.put((byte) (pixel & 0xFF));         // B
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
            }
        }
        buffer.flip();

        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, img.getWidth(), img.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        return texId;
    }

    private void buildLevel(int levelIndex){
        // clear existing entities
        entities.clear();

        // get level data from parser
        Level level = parser.getLevel(levelIndex);
        collisionGrid = level.getCollisionGrid();
        try {
            backgroundBI = ImageIO.read(new File(RESOURCE_PATH+"\\textures\\" + level.getBackgroundImage()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        backgroundTex = createTextureFromBufferedImage(backgroundBI);

        // add player entity first
        Entity player = null;
        for(Entity e : level.getEntities()){
            if(e.getType().equals("player")){
                player = e;
                break;
            }
        }
        if(player == null){
            throw new IllegalStateException("Level " + levelIndex + " has no player entity defined.");
        }
        player.setTextureId(playerTex);
        player.setWidth(ENTITY_WIDTH_CELLS * GRID_CELL_SIZE);
        player.setHeight(ENTITY_HEIGHT_CELLS * GRID_CELL_SIZE);
        playerX = player.getX() * GRID_CELL_SIZE;
        playerY = player.getY() * GRID_CELL_SIZE;
        entities.add(player);
        parser.parse();
        player.setPosition(level.getEntities().get(0).getX(), level.getEntities().get(0).getY());

        // add NPCs and other entities
        for(Entity e : level.getEntities()){
            if(!e.getType().equals("player")){
                e.setTextureId(npcTex); // for now, all non-player entities use npc texture
                e.setWidth(ENTITY_WIDTH_CELLS * GRID_CELL_SIZE);
                e.setHeight(ENTITY_HEIGHT_CELLS * GRID_CELL_SIZE);
                e.setPosition(e.getX(), e.getY());
                entities.add(e);
            }
        }
        // add doors
        createDoors(parser.getDoors(levelIndex));
    }

    private void createDoors(List<Door> doors){
        for(Door d : doors){
            // drawtexturequad draws textures at window coordinates, and that is what the coordinates are in json
            drawTexturedQuad(doorTex, d.getX(), d.getY(), DOOR_WIDTH, DOOR_HEIGHT, DOOR_WIDTH, DOOR_HEIGHT);
            System.out.println(d.getX() + ", " + d.getY());
            // create door entity
            Entity door = new Entity("door", doorTex, d.getX(), d.getY(), DOOR_WIDTH, DOOR_HEIGHT, d.getTargetLevel(), d.getTargetX(), d.getTargetY());
            entities.add(door);
        }
    }

    private void loop() {
        long lastTime = System.nanoTime();
        double nsPerUpdate = 1_000_000_000.0 / FRAMERATE;

        while (!glfwWindowShouldClose(window)) {
            long now = System.nanoTime();
            if (now - lastTime >= nsPerUpdate) {
                updateGame((now - lastTime) / 1_000_000.0); // delta in ms
                lastTime = now;
            }

            render();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private int[][] playerPositionInWindowToPositionInGrid(int x, int y) {
        int gridX = x / GRID_CELL_SIZE;
        int gridY = y / GRID_CELL_SIZE;
        return new int[][]{{gridX, gridY}};
    }
    
    private int playerPositionInWindowToPositionInGridX(int x, int y) {
        int gridX = x / GRID_CELL_SIZE;
        return gridX;
    }
    
    private int playerPositionInWindowToPositionInGridY(int x, int y) {
        int gridY = y / GRID_CELL_SIZE;
        return gridY;
    }

    long movementLastTime = System.currentTimeMillis();
    private void updateGame(double deltaMs) {
        // Update player position
        if(!messageBoxDisplayed){
            // delay movement to prevent moving too fast
            long now = System.currentTimeMillis();
            if (now - movementLastTime >= MOVEMENT_DELAY_MS) {
                // check to see if player will move into a collision tile
                // since the player is 3x5 cells, check all 15 cells
                int newPlayerX = Math.max(0, Math.min(playerX + xVelocity, winW - playerBI.getWidth()));
                int newPlayerY = Math.max(0, Math.min(playerY + yVelocity, winH - playerBI.getHeight()));
                boolean collision = false;
                for(int px = 0; px < 3; px++){
                    for(int py = 0; py < 5; py++){
                        int checkX = playerPositionInWindowToPositionInGridX(newPlayerX + px * GRID_CELL_SIZE, newPlayerY + py * GRID_CELL_SIZE);
                        int checkY = playerPositionInWindowToPositionInGridY(newPlayerX + px * GRID_CELL_SIZE, newPlayerY + py * GRID_CELL_SIZE);
                        if(checkX < 0 || checkX >= collisionGrid[0].length || checkY < 0 || checkY >= collisionGrid.length || collisionGrid[checkY][checkX] == 1){
                            collision = true;
                            return; // don't move if collision detected
                        }
                    }
                }
                
                playerX = Math.max(0, Math.min(playerX + xVelocity, winW - playerBI.getWidth()));
                playerY = Math.max(0, Math.min(playerY + yVelocity, winH - playerBI.getHeight()));
                movementLastTime = now;
            } else {
                return; // skip movement update
            }
        }

        entities.get(0).setPosition(playerX, playerY);

        // NPC random movement
        if (Math.random() < 0.01) {
            for (int i = 1; i < entities.size(); i++) {
                Entity npc = entities.get(i);
                // check to see if npc will move into a collision tile, reuse the player collision code
                // check to see if npc will move into a collision tile
                // since the npc is 3x5 cells, check all 15 cells
                int newX = npc.getX() + 25;
                int newY = npc.getY() + 0;
                boolean collision = false;
                for(int px = 0; px < 3; px++){
                    for(int py = 0; py < 5; py++){
                        int checkX = newX + npc.getX() + px * GRID_CELL_SIZE;
                        int checkY = newY + npc.getX() + py * GRID_CELL_SIZE;
                        if(checkX < 0 || checkX >= collisionGrid[0].length || checkY < 0 || checkY >= collisionGrid.length || collisionGrid[checkY][checkX] == 1){
                            collision = true;
                            return; // don't move if collision detected
                        }
                    }
                }
                npc.setPosition(npc.getX() + 25, npc.getY() + 0);
            }
        } else if (Math.random() < 0.01) {
            for (int i = 1; i < entities.size(); i++) {
                Entity npc = entities.get(i);
                int newX = npc.getX() + 0;
                int newY = npc.getY() + 25;
                boolean collision = false;
                for(int px = 0; px < 3; px++){
                    for(int py = 0; py < 5; py++){
                        int checkX = newX + npc.getX() + px * GRID_CELL_SIZE;
                        int checkY = newY + npc.getX() + py * GRID_CELL_SIZE;
                        if(checkX < 0 || checkX >= collisionGrid[0].length || checkY < 0 || checkY >= collisionGrid.length || collisionGrid[checkY][checkX] == 1){
                            collision = true;
                            return; // don't move if collision detected
                        }
                    }
                }
                npc.setPosition(npc.getX() + 0, npc.getY() + 25);
            }
        } else if (Math.random() < 0.01) {
            for (int i = 1; i < entities.size(); i++) {
                Entity npc = entities.get(i);
                int newX = npc.getX() - 25;
                int newY = npc.getY() + 0;
                boolean collision = false;
                for(int px = 0; px < 3; px++){
                    for(int py = 0; py < 5; py++){
                        int checkX = newX + npc.getX() + px * GRID_CELL_SIZE;
                        int checkY = newY + npc.getX() + py * GRID_CELL_SIZE;
                        if(checkX < 0 || checkX >= collisionGrid[0].length || checkY < 0 || checkY >= collisionGrid.length || collisionGrid[checkY][checkX] == 1){
                            collision = true;
                            return; // don't move if collision detected
                        }
                    }
                }
                npc.setPosition(npc.getX() - 25, npc.getY() + 0);
            }
        } else if (Math.random() < 0.01) {
            for (int i = 1; i < entities.size(); i++) {
                Entity npc = entities.get(i);
                int newX = npc.getX() + 0;
                int newY = npc.getY() - 25;
                boolean collision = false;
                for(int px = 0; px < 3; px++){
                    for(int py = 0; py < 5; py++){
                        int checkX = newX + npc.getX() + px * GRID_CELL_SIZE;
                        int checkY = newY + npc.getX() + py * GRID_CELL_SIZE;
                        if(checkX < 0 || checkX >= collisionGrid[0].length || checkY < 0 || checkY >= collisionGrid.length || collisionGrid[checkY][checkX] == 1){
                            collision = true;
                            return; // don't move if collision detected
                        }
                    }
                }
                npc.setPosition(npc.getX() + 0, npc.getY() - 25);
            }
        }

        // Typewriter update if message is shown
        if (messageBoxDisplayed && typeIndex <= currentFullMessage.length()) {
            long now = System.currentTimeMillis();
            if (now - lastTypeTime >= TYPEWRITER_DELAY_MS) {
                typeIndex++;
                lastTypeTime = now;
                updateMessageTexture();
            }
        }
    }

    void drawGrid(float size, int cells) {
        float half = size / 2.0f;
        float step = size / cells;
    
        glColor3f(0.7f, 0.7f, 0.7f); // light gray grid lines
        glBegin(GL_LINES);
        for (int i = 0; i <= cells; i++) {
            float x = -half + i * step;
    
            // vertical lines
            glVertex3f(x, 0.0f, -half);
            glVertex3f(x, 0.0f, half);
    
            // horizontal lines
            glVertex3f(-half, 0.0f, x);
            glVertex3f(half, 0.0f, x);
        }
        glEnd();
    }    

    private void render() {
        //drawGrid();
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);

        // Setup projection for current window size (update if resized)
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        //drawGrid(24.0f, 24);
        glOrtho(0, winW, winH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Draw background to cover the entire window
        drawTexturedQuad(backgroundTex, 0, 0, winW, winH, backgroundBI.getWidth(), backgroundBI.getHeight());
        // draw grid overlay for debugging
        //drawTexturedQuad(gridTex, 0, 0, winW, winH, gridBI.getWidth(), gridBI.getHeight());

        // Draw entities
        for (Entity e : entities) {
            drawTexturedQuad(e.getTextureId(), e.getX(), e.getY(), e.getWidth(), e.getHeight(), e.getWidth(), e.getHeight());
        }

        // Draw message box overlay if necessary
        if (messageBoxDisplayed) {
            int boxW = winW / 2;
            int boxH = winH / 4;
            int boxX = (winW - boxW) / 2;
            int boxY = (int) (winH * 0.75);

            // Draw message box background (scaled)
            drawTexturedQuad(messageBoxTex, boxX, boxY, boxW, boxH, messageBoxBI.getWidth(), messageBoxBI.getHeight());

            // Draw text overlay (texture updated dynamically)
            glEnable(GL_BLEND);
            glBindTexture(GL_TEXTURE_2D, messageTextureGL);
            drawTexturedQuad(messageTextureGL, boxX, boxY, boxW, boxH, messageBoxBI.getWidth(), messageBoxBI.getHeight());
        }
    }

    private void drawTexturedQuad(int texId, int x, int y, int w, int h, int texW, int texH) {
        glBindTexture(GL_TEXTURE_2D, texId);
        glPushMatrix();
        glTranslatef(0f, 0f, 0f);
        glBegin(GL_QUADS);
        glTexCoord2f(0f, 0f);
        glVertex2f(x, y);
        glTexCoord2f(1f, 0f);
        glVertex2f(x + w, y);
        glTexCoord2f(1f, 1f);
        glVertex2f(x + w, y + h);
        glTexCoord2f(0f, 1f);
        glVertex2f(x, y + h);
        glEnd();
        glPopMatrix();
    }

    // === Input handling (GLFW key codes) ===
    private void keyPressed(int key) {
        if (messageBoxDisplayed) {
            if (key == GLFW_KEY_E) {
                closeMessage();
            }
            return;
        }

        switch (key) {
            case GLFW_KEY_UP:
            case GLFW_KEY_W:
                yVelocity = -MOVE_AMOUNT;
                break;
            case GLFW_KEY_DOWN:
            case GLFW_KEY_S:
                yVelocity = MOVE_AMOUNT;
                break;
            case GLFW_KEY_LEFT:
            case GLFW_KEY_A:
                xVelocity = -MOVE_AMOUNT;
                break;
            case GLFW_KEY_RIGHT:
            case GLFW_KEY_D:
                xVelocity = MOVE_AMOUNT;
                break;
            case GLFW_KEY_E:
                interactWithNearestNPC();
                break;
            case GLFW_KEY_ESCAPE:
                glfwSetWindowShouldClose(window, true);
                break;
            case GLFW_KEY_P:
                // debug: print player position
                System.out.println("Player position in window: (" + playerX + ", " + playerY + ")" + " " + "Player position in grid: (" + playerPositionInWindowToPositionInGridX(playerX, playerY) + ", " + playerPositionInWindowToPositionInGridY(playerX, playerY) + ")");
                break;
            
        }
        // prevent movement in both x and y at the same time
        if (xVelocity != 0 && yVelocity != 0) {
            xVelocity = 0;
            yVelocity = 0;
        }
    }

    private void keyReleased(int key) {
        if (messageBoxDisplayed) return;

        switch (key) {
            case GLFW_KEY_UP:
            case GLFW_KEY_W:
                if (yVelocity < 0) yVelocity = 0;
                break;
            case GLFW_KEY_DOWN:
            case GLFW_KEY_S:
                if (yVelocity > 0) yVelocity = 0;
                break;
            case GLFW_KEY_LEFT:
            case GLFW_KEY_A:
                if (xVelocity < 0) xVelocity = 0;
                break;
            case GLFW_KEY_RIGHT:
            case GLFW_KEY_D:
                if (xVelocity > 0) xVelocity = 0;
                break;
        }
    }

    // === Message box handling ===
    public void displayMessage(String message) {
        if (messageBoxDisplayed) return;
        yVelocity = 0;
        xVelocity = 0;
        messageBoxDisplayed = true;
        currentFullMessage = message;
        typeIndex = 0;
        lastTypeTime = System.currentTimeMillis();
        updateMessageTexture();
        playTalkingSound();
    }

    public void displayMessageConversation(String[] messages) {
        for(int i=0; i < messages.length; i++){
            displayMessage(messages[i]);
            // wait until message is fully displayed and user clicks the text box
            
        }
    }

    public void closeMessage() {
        if (!messageBoxDisplayed) return;
        messageBoxDisplayed = false;
        currentFullMessage = "";
        typeIndex = 0;
        soundPlayer.stop();
    }

    private void updateMessageTexture() {
        // Compose message box image with current substring text
        String text = currentFullMessage.substring(0, Math.min(typeIndex, currentFullMessage.length()));

        Graphics2D g = messageTextureImage.createGraphics();
        // draw background image as base
        g.drawImage(messageBoxBI, 0, 0, messageTextureImage.getWidth(), messageTextureImage.getHeight(), null);

        // draw wrapped text (simple)
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        // Basic wrapping
        int padding = 12;
        int maxWidth = messageTextureImage.getWidth() - padding * 2;
        drawStringWrapped(g, text, padding, padding, maxWidth, 22);

        g.dispose();

        // upload to GL texture
        int w = messageTextureImage.getWidth();
        int h = messageTextureImage.getHeight();
        int[] pixels = new int[w * h];
        messageTextureImage.getRGB(0, 0, w, h, pixels, 0, w);

        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = pixels[y * w + x];
                buf.put((byte) ((pixel >> 16) & 0xFF)); // R
                buf.put((byte) ((pixel >> 8) & 0xFF));  // G
                buf.put((byte) (pixel & 0xFF));         // B
                buf.put((byte) ((pixel >> 24) & 0xFF)); // A
            }
        }
        buf.flip();

        glBindTexture(GL_TEXTURE_2D, messageTextureGL);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
    }

    private void drawStringWrapped(Graphics2D g, String text, int x, int y, int maxWidth, int lineHeight) {
        if (text == null || text.isEmpty()) return;
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int curY = y + fm.getAscent();

        for (String w : words) {
            String test = line.length() == 0 ? w : line + " " + w;
            if (fm.stringWidth(test) > maxWidth) {
                g.drawString(line.toString(), x, curY);
                line = new StringBuilder(w);
                curY += lineHeight;
            } else {
                if (line.length() > 0) line.append(" ");
                line.append(w);
            }
        }
        if (line.length() > 0) {
            g.drawString(line.toString(), x, curY);
        }
    }

    private void interactWithNearestNPC() {
        if (entities.size() < 2) return;
        Entity player = entities.get(0);
        Entity nearest = null;
        double minDist = NPC_INTERACTION_DISTANCE;
        for (int i = 1; i < entities.size(); i++) {
            Entity npc = entities.get(i);
            double dx = player.getX() - npc.getX();
            double dy = player.getY() - npc.getY();
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d < minDist) {
                minDist = d;
                nearest = npc;
            }
        }
        // if the nearest npc is a door, run the build level function with the door's target level
        if(nearest.getType().equals("door")){
            buildLevel(nearest.getTargetLevel());
            return;
        }
        // display npc conversation if npc has one defined
        if(nearest != null && nearest.getDialogueSequential().size() > 0) {
            String[] conversation = nearest.getDialogueSequential().toArray(new String[0]);
            displayMessageConversation(conversation);
            return;
        }

        // display random message if npc does not have a conversation defined
        if(nearest != null) {
            displayMessage(getRandomNPCMessage());
        } 
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

    // === Cleanup ===
    private void cleanup() {
        // delete GL textures
        glDeleteTextures(playerTex);
        glDeleteTextures(npcTex);
        glDeleteTextures(backgroundTex);
        glDeleteTextures(messageBoxTex);
        if (messageTextureGL != -1) glDeleteTextures(messageTextureGL);

        //glfwFreeCallbacks(window);
        //glfwDestroyWindow(window);
        //glfwTerminate();
        //glfwSetErrorCallback(null).free();
        soundPlayer.stop();
    }
}
