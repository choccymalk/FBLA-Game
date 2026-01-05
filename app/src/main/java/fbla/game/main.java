package fbla.game;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import javax.imageio.ImageIO;
import org.checkerframework.checker.units.qual.s;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import java.awt.Point;
import java.util.concurrent.*;
import java.util.Arrays;

public class main {
    // === Config ===
    private static final int WINDOW_W = 1272;
    private static final int WINDOW_H = 720;
    private static final int MOVE_AMOUNT = 24;
    public static final int MOVEMENT_DELAY_MS = 75; // seconds between moves
    private static final int TYPEWRITER_DELAY_MS = 75;
    private static final double NPC_INTERACTION_DISTANCE = 500.0;
    private static final String RESOURCE_PATH = /*System.getenv("LOCALAPPDATA")*/System.getProperty("user.home")+"\\Desktop\\FBLA-Game\\game_resources"; // TODO: don't use hardcoded path
    private double FRAMERATE = 12.0;
    private static final int GRID_CELL_SIZE = 24; // each cell in the grid is 24x24 pixels
    private static final int GRID_WIDTH = 53; // number of cells in the grid horizontally
    private static final int GRID_HEIGHT = 30; // number of cells in the grid vertically
    private static final int ENTITY_WIDTH_CELLS = 3; // all entities are 3x5 cells
    private static final int ENTITY_HEIGHT_CELLS = 5;
    private static final int DOOR_WIDTH = 96;
    private static final int DOOR_HEIGHT = 144;
    private static final boolean DRAW_DEBUG_GRID = false;
    
    // === Game States ===
    private enum GameState {
        TITLE_SCREEN,
        OPTIONS_MENU,
        IN_GAME,
        PAUSED
    }
    
    // === Game state ===
    private long window;
    private int winW = WINDOW_W, winH = WINDOW_H;
    private final List<Entity> entities = new ArrayList<>();
    private List<EntityAI> entityAIs = new ArrayList<>(); // maps to the indices of entities list
    private BufferedImage playerBI, backgroundBI, npcBI, messageBoxBI, gridBI, doorBI, titleScreenBI;
    private int playerTex, backgroundTex, npcTex, messageBoxTex, gridTex, doorTex, titleScreenTex;
    private HashMap<Integer, entityAnimation> entityIndexToAnimationObjects = new HashMap<>();
    private int playerX = 0, playerY = 600;
    private int xVelocity = 0, yVelocity = 0;
    private boolean messageBoxDisplayed = false;
    private int cursorXPosition;
    private int cursorYPosition;
    private boolean leftMouseButtonPressed = false;
    private int[][] responsePositions;
    private boolean messageBoxOptionsDisplayed = false;
    private String[] currentResponseOptions;
    private dialogueTree currentTree = null;
    private Entity currentNPC = null;
    private int currentLevelIndex;
    private int[][] entityMovement; // stores movement for npcs, used to animate them. Format: entityMovement[entityIndex][0] = +x movement, entityMovement[entityIndex][1] = +y movement, entityMovement[entityIndex][2] = -x movement, entityMovement[entityIndex][3] = -y movement
    private int[] pressedKey = new int[2]; // store up to 2 pressed keys for smooth movement transitions, last index is the most recent key, first index is the previous key
    jsonParser parser = new jsonParser(new File(RESOURCE_PATH+"\\levels.json"));
    
    // Title screen state
    private GameState currentGameState = GameState.TITLE_SCREEN;
    private String[] titleScreenOptions = {"Start Game", "Options", "Exit"};
    private int[][] titleScreenOptionsPositions;
    private int titleScreenSelectedOption = 0;
    private BufferedImage titleScreenTextureImage;
    private int titleScreenTextureGL = -1;
    private boolean titleScreenNeedsUpdate = true;
    
    // Options menu state
    private BufferedImage optionsMenuTextureImage;
    private int optionsMenuTextureGL = -1;
    private boolean optionsMenuNeedsUpdate = true;
    private String[] optionsMenuItems = {"Graphics", "Audio", "Controls", "Back"};
    private String[][] optionsSubMenus = {
        {"Resolution", "Fullscreen", "VSync", "Frame Rate Limit", "Back"},
        {"Master Volume", "Music Volume", "SFX Volume", "Back"},
        {"Key Bindings", "Mouse Sensitivity", "Invert Y-Axis", "Back"}
    };
    private int currentOptionsMenu = 0; // 0 = main, 1 = graphics, 2 = audio, 3 = controls
    private int selectedOption = 0;
    private int[][] optionsMenuPositions;
    
    // Settings variables
    private boolean fullscreen = false;
    private boolean vsync = true;
    private int frameRateLimit = 60;
    private int masterVolume = 100;
    private int musicVolume = 80;
    private int sfxVolume = 100;
    private boolean invertYAxis = false;
    private double mouseSensitivity = 1.0;
    
    // get collision grid for the current level fron parser, for right now, we will only use level 0
    int[][] collisionGrid = parser.getCollisionGrid(0);
    
    // Typewriter state
    private String currentFullMessage = "";
    private int typeIndex = 0;
    private long lastTypeTime = 0;
    private BufferedImage messageTextureImage; // combined BG + text
    private int messageTextureGL = -1;
    
    private static final String[] NPC_MESSAGES = { // does nothing (i think) but breaks when removed
            "I have nothing to say to you."
    };
    
    public static void main(String[] args) throws Exception {
        new main().run();
    }
    
    public void run() throws Exception {
        init();
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
        window = glfwCreateWindow(winW, winH, "Game Window", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create GLFW window");
        
        // Set up resize callback
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            winW = w;
            winH = h;
            glViewport(0, 0, w, h);
            titleScreenNeedsUpdate = true;
            optionsMenuNeedsUpdate = true;
        });
        
        // Input callbacks
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) keyPressed(key);
            if (action == GLFW_RELEASE) keyReleased(key);
        });
        
        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            cursorCallback(window, xpos, ypos);            
        });
        
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            mouseClickCallback(window, button, action, mods);
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
        titleScreenTex = createTextureFromBufferedImage(titleScreenBI);
        
        // Prepare message overlay buffer image (same size as message box) but will update later
        messageTextureImage = new BufferedImage(messageBoxBI.getWidth(), messageBoxBI.getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        // Prepare title screen overlay buffer image
        titleScreenTextureImage = new BufferedImage(winW, winH, BufferedImage.TYPE_INT_ARGB);
        
        // Prepare options menu overlay buffer image
        optionsMenuTextureImage = new BufferedImage(winW, winH, BufferedImage.TYPE_INT_ARGB);
        
        // Create a GL texture placeholder for the message box text overlay
        messageTextureGL = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, messageTextureGL);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        
        // Allocate empty image for message box
        ByteBuffer empty = memAlloc(messageBoxBI.getWidth() * messageBoxBI.getHeight() * 4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, messageBoxBI.getWidth(), messageBoxBI.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, empty);
        memFree(empty);
        
        // Create a GL texture placeholder for the title screen overlay
        titleScreenTextureGL = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, titleScreenTextureGL);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        
        // Allocate empty image for title screen
        ByteBuffer emptyTitle = memAlloc(winW * winH * 4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, winW, winH, 0, GL_RGBA, GL_UNSIGNED_BYTE, emptyTitle);
        memFree(emptyTitle);
        
        // Create a GL texture placeholder for the options menu overlay
        optionsMenuTextureGL = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, optionsMenuTextureGL);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        
        // Allocate empty image for options menu
        ByteBuffer emptyOptions = memAlloc(winW * winH * 4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, winW, winH, 0, GL_RGBA, GL_UNSIGNED_BYTE, emptyOptions);
        memFree(emptyOptions);
        
        // Initialize title screen
        updateTitleScreenTexture();
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
            
            // Try to load title screen image, fall back to background if not found
            try {
                titleScreenBI = ImageIO.read(resourcesPath.resolve("textures/title_screen.png").toFile());
            } catch (IOException e) {
                System.out.println("No title screen image found, using background with overlay.");
                titleScreenBI = backgroundBI;
            }
        } catch (IOException e) {
            System.err.println("Failed to load game resources: " + e.getMessage());
            throw e;
        }
    }
    
    public void setCollisionGrid(int[][] grid){
        collisionGrid = grid;
    }
    
    public int createTextureFromBufferedImage(BufferedImage img) {
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
    
    private void startGame() {
        currentGameState = GameState.IN_GAME;
        buildLevel(0);
    }
    
    private void buildLevel(int levelIndex){
        currentLevelIndex = levelIndex;
        // clear existing entities
        entities.clear();
        entityAIs.clear();
        entityIndexToAnimationObjects.clear();
        
        // get level data from parser
        Level level = parser.getLevel(levelIndex);
        collisionGrid = level.getCollisionGrid();
        try {
            backgroundBI = ImageIO.read(new File(RESOURCE_PATH+"\\textures\\" + level.getBackgroundImage()));
        } catch (IOException e) {
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
        try {
            playerBI = ImageIO.read(new File(RESOURCE_PATH+"\\textures\\" + player.getImagePath()));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        playerTex = createTextureFromBufferedImage(playerBI);
        player.setTextureId(playerTex);
        player.setWidth(ENTITY_WIDTH_CELLS * GRID_CELL_SIZE);
        player.setHeight(ENTITY_HEIGHT_CELLS * GRID_CELL_SIZE);
        playerX = player.getX() * GRID_CELL_SIZE;
        playerY = player.getY() * GRID_CELL_SIZE;
        entities.add(player);
        parser.parse();
        player.setPosition(level.getEntities().get(0).getX(), level.getEntities().get(0).getY());
        entityIndexToAnimationObjects.put(0, new entityAnimation(player, RESOURCE_PATH));
        entityMovement = new int[level.getEntities().size()][4];
        
        // add NPCs and other entities
        for(Entity e : level.getEntities()){
            if(!e.getType().equals("player")){
                entityAIs.add(new EntityAI(e, parser, this));
                entityIndexToAnimationObjects.put(entities.size(), new entityAnimation(e, RESOURCE_PATH));
                System.out.println("Adding entity of type " + e.getType() + " at (" + e.getX() + ", " + e.getY() + ")");
                try {
                    e.setTextureId(createTextureFromBufferedImage(ImageIO.read(new File(RESOURCE_PATH+"\\textures\\" + e.getImagePath()))));
                    System.out.println("Loaded texture for entity from " + RESOURCE_PATH+"\\textures\\" + e.getImagePath() + ". Texture ID: " + e.getTextureId());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.setWidth(ENTITY_WIDTH_CELLS * GRID_CELL_SIZE);
                e.setHeight(ENTITY_HEIGHT_CELLS * GRID_CELL_SIZE);
                e.setPosition(e.getX(), e.getY());
                entities.add(e);
                // update the collision grid with the entitys position
                for(int i = 0; i <= ENTITY_WIDTH_CELLS-1; i++){
                    for(int j = 0; j <= ENTITY_HEIGHT_CELLS-1; j++){
                        collisionGrid[(e.getY() / GRID_CELL_SIZE) + j][(e.getX() / GRID_CELL_SIZE) + i] = 1;
                    }
                }
            }
        }
        
        // add doors
        createDoors(parser.getDoors(levelIndex));
    }
    
    private void createDoors(List<Door> doors){
        for(Door d : doors){
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
        // Handle different game states
        switch (currentGameState) {
            case TITLE_SCREEN:
                updateTitleScreen();
                break;
            case OPTIONS_MENU:
                updateOptionsMenu();
                break;
            case IN_GAME:
                updateInGame(deltaMs);
                break;
            case PAUSED:
                break;
        }
    }
    
    private void updateTitleScreen() {
        updateTitleScreenTexture();
        
        // Update title screen selection based on mouse position
        if (cursorYPosition > winH / 2) {
            for(int i = 0; i < titleScreenOptions.length; i++){
                int[] pos = titleScreenOptionsPositions[i];
                if(cursorXPosition >= pos[0] && cursorXPosition <= pos[0] + pos[2] &&
                cursorYPosition >= pos[1] && cursorYPosition <= pos[1] + pos[3]){
                    if (titleScreenSelectedOption != i) {
                        titleScreenSelectedOption = i;
                        titleScreenNeedsUpdate = true;
                    }
                }
            }
        }
        
        // Handle mouse click on title screen options
        if (leftMouseButtonPressed) {
            leftMouseButtonPressed = false;
            if (cursorYPosition > winH / 2) {
                int optionHeight = 40;
                int startY = winH / 2;
                for (int i = 0; i < titleScreenOptions.length; i++) {
                    if (cursorYPosition >= startY + i * optionHeight && 
                        cursorYPosition <= startY + (i + 1) * optionHeight &&
                        cursorXPosition >= winW / 2 - 100 && 
                        cursorXPosition <= winW / 2 + 100) {
                        handleTitleScreenOption(i);
                        break;
                    }
                }
            }
        }
        
        if (titleScreenNeedsUpdate) {
            updateTitleScreenTexture();
            titleScreenNeedsUpdate = false;
        }
    }
    
    private void updateOptionsMenu() {
        updateOptionsMenuTexture();
        
        // Update selection based on mouse position
        String[] currentMenuItems = getCurrentMenuItems();
        if (cursorYPosition > winH / 3) {
            for(int i = 0; i < currentMenuItems.length; i++){
                int[] pos = optionsMenuPositions[i];
                if(cursorXPosition >= pos[0] && cursorXPosition <= pos[0] + pos[2] &&
                cursorYPosition >= pos[1] && cursorYPosition <= pos[1] + pos[3]){
                    if (selectedOption != i) {
                        selectedOption = i;
                        optionsMenuNeedsUpdate = true;
                    }
                }
            }
        }
        
        // Handle mouse click on options
        if (leftMouseButtonPressed) {
            leftMouseButtonPressed = false;
            handleOptionsMenuClick();
        }
        
        if (optionsMenuNeedsUpdate) {
            updateOptionsMenuTexture();
            optionsMenuNeedsUpdate = false;
        }
    }
    
    private String[] getCurrentMenuItems() {
        switch (currentOptionsMenu) {
            case 1: return optionsSubMenus[0]; // Graphics
            case 2: return optionsSubMenus[1]; // Audio
            case 3: return optionsSubMenus[2]; // Controls
            default: return optionsMenuItems; // Main menu
        }
    }
    
    private void updateInGame(double deltaMs) {
        // Update player position
        if(!messageBoxDisplayed){
            long now = System.currentTimeMillis();
            if (now - movementLastTime >= MOVEMENT_DELAY_MS) {
                int newPlayerX = Math.max(0, Math.min(playerX + xVelocity, winW - playerBI.getWidth()));
                int newPlayerY = Math.max(0, Math.min(playerY + yVelocity, winH - playerBI.getHeight()));
                boolean collision = false;
                for(int px = 0; px < 3; px++){
                    for(int py = 0; py < 5; py++){
                        int checkX = playerPositionInWindowToPositionInGridX(newPlayerX + px * GRID_CELL_SIZE, newPlayerY + py * GRID_CELL_SIZE);
                        int checkY = playerPositionInWindowToPositionInGridY(newPlayerX + px * GRID_CELL_SIZE, newPlayerY + py * GRID_CELL_SIZE);
                        if(checkX < 0 || checkX >= collisionGrid[0].length || checkY < 0 || checkY >= collisionGrid.length || collisionGrid[checkY][checkX] == 1){
                            collision = true;
                            return;
                        }
                    }
                }
                playerX = Math.max(0, Math.min(playerX + xVelocity, winW - playerBI.getWidth()));
                playerY = Math.max(0, Math.min(playerY + yVelocity, winH - playerBI.getHeight()));
                movementLastTime = now;
            } else {
                return;
            }
        }
        
        if(yVelocity == 0 && xVelocity == 0){
            entityMovement[0][0] = 0;
            entityMovement[0][1] = 0;
            entityMovement[0][2] = 0;
            entityMovement[0][3] = 0;
            entityIndexToAnimationObjects.get(0).tick("idle");
        } else if (xVelocity == 24){
            entityMovement[0][0] = 0;
            entityMovement[0][1] = 0;
            entityMovement[0][2] = 0;
            entityMovement[0][3] = 0;
            entityIndexToAnimationObjects.get(0).tick("walkingRight");
        } else if (xVelocity == -24){
            entityMovement[0][0] = 0;
            entityMovement[0][1] = 0;
            entityMovement[0][2] = 0;
            entityMovement[0][3] = 0;
            entityIndexToAnimationObjects.get(0).tick("walkingLeft");
        } else if( yVelocity == 24){
            entityMovement[0][0] = 0;
            entityMovement[0][1] = 0;
            entityMovement[0][2] = 0;
            entityMovement[0][3] = 0;
            entityIndexToAnimationObjects.get(0).tick("idle");
        } else if (yVelocity == -24){
            entityMovement[0][0] = 0;
            entityMovement[0][1] = 0;
            entityMovement[0][2] = 0;
            entityMovement[0][3] = 0;
            entityIndexToAnimationObjects.get(0).tick("idle");
        }
        
        entities.get(0).setPosition(playerX, playerY);
        
        for(int i = 1; i < entities.size(); i++){
            Entity npc = entities.get(i);
            if(entityMovement != null && entityMovement.length > i){
                if(entityMovement[i][0] > 0){
                    entityIndexToAnimationObjects.get(i).tick("walkingRight");
                    entityMovement[i][0]--;
                    continue;
                } else if(entityMovement[i][2] > 0){
                    entityIndexToAnimationObjects.get(i).tick("walkingLeft");
                    entityMovement[i][2]--;
                    continue;
                } else if(entityMovement[i][1] > 0){
                    entityIndexToAnimationObjects.get(i).tick("walkingUp");
                    entityMovement[i][1]--;
                    continue;
                } else if(entityMovement[i][3] > 0){
                    entityIndexToAnimationObjects.get(i).tick("walkingDown");
                    entityMovement[i][3]--;
                    continue;
                } else if(entityMovement[i][0] == 0 && entityMovement[i][1] == 0 && entityMovement[i][2] == 0 && entityMovement[i][3] == 0){
                    entityIndexToAnimationObjects.get(i).tick("idle");
                } else {
                    entityIndexToAnimationObjects.get(i).tick("idle");
                }
            }
        }
        
        // Update NPC AI
        for(int i = 0; i < entityAIs.size(); i++){
            EntityAI ai = entityAIs.get(i);
            ai.tick();
        }
        
        if(messageBoxOptionsDisplayed){
            if(leftMouseButtonPressed){
                for(int i = 0; i < responsePositions.length; i++){
                    int[] pos = responsePositions[i];
                    if(cursorXPosition >= pos[0] && cursorXPosition <= pos[0] + pos[2] &&
                    cursorYPosition >= pos[1] && cursorYPosition <= pos[1] + pos[3]){
                        System.out.println("Player clicked on response: " + (i + 1));
                        dialogueHandler(currentTree, (i + 1), currentNPC);
                        leftMouseButtonPressed = false;
                        return;
                    }
                }
                leftMouseButtonPressed = false;
            }
            for(int i = 0; i < responsePositions.length; i++){
                    int[] pos = responsePositions[i];
                    if(cursorXPosition >= pos[0] && cursorXPosition <= pos[0] + pos[2] &&
                    cursorYPosition >= pos[1] && cursorYPosition <= pos[1] + pos[3]){
                        changeColorOfResponseOptionMouseOver();
                    }
                }
        }
    }
    
    void drawGrid(float size, int cells) {
        float half = size / 2.0f;
        float step = size / cells;
   
        glColor3f(0.7f, 0.7f, 0.7f);
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
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Setup projection for current window size (update if resized)
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, winW, winH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        
        // Handle different game states
        switch (currentGameState) {
            case TITLE_SCREEN:
                renderTitleScreen();
                break;
            case OPTIONS_MENU:
                renderOptionsMenu();
                break;
            case IN_GAME:
                renderInGame();
                break;
            case PAUSED:
                renderInGame();
                renderPauseMenu();
                break;
        }
    }
    
    private void renderTitleScreen() {
        drawTexturedQuad(titleScreenTex, 0, 0, winW, winH, titleScreenBI.getWidth(), titleScreenBI.getHeight());
        
        glEnable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, titleScreenTextureGL);
        drawTexturedQuad(titleScreenTextureGL, 0, 0, winW, winH, winW, winH);
    }
    
    private void renderOptionsMenu() {
        // Draw background
        drawTexturedQuad(titleScreenTex, 0, 0, winW, winH, titleScreenBI.getWidth(), titleScreenBI.getHeight());
        
        // Draw semi-transparent overlay
        glDisable(GL_TEXTURE_2D);
        glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(winW, 0);
        glVertex2f(winW, winH);
        glVertex2f(0, winH);
        glEnd();
        glEnable(GL_TEXTURE_2D);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Draw options menu overlay
        glEnable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, optionsMenuTextureGL);
        drawTexturedQuad(optionsMenuTextureGL, 0, 0, winW, winH, winW, winH);
    }
    
    private void renderInGame() {
        drawTexturedQuad(backgroundTex, 0, 0, winW, winH, backgroundBI.getWidth(), backgroundBI.getHeight());
        
        if(DRAW_DEBUG_GRID){
            drawTexturedQuad(gridTex, 0, 0, winW, winH, gridBI.getWidth(), gridBI.getHeight());
        }
        
        for (Entity e : entities) {
            drawTexturedQuad(e.getTextureId(), e.getX(), e.getY(), e.getWidth(), e.getHeight(), e.getWidth(), e.getHeight());
        }
        
        if (messageBoxDisplayed) {
            int boxW = winW / 2;
            int boxH = winH / 4;
            int boxX = (winW - boxW) / 2;
            int boxY = (int) (winH * 0.75);
            
            drawTexturedQuad(messageBoxTex, boxX, boxY, boxW, boxH, messageBoxBI.getWidth(), messageBoxBI.getHeight());
            
            glEnable(GL_BLEND);
            glBindTexture(GL_TEXTURE_2D, messageTextureGL);
            drawTexturedQuad(messageTextureGL, boxX, boxY, boxW, boxH, messageBoxBI.getWidth(), messageBoxBI.getHeight());
        }
    }
    
    private void renderPauseMenu() {
        glDisable(GL_TEXTURE_2D);
        glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(winW, 0);
        glVertex2f(winW, winH);
        glVertex2f(0, winH);
        glEnd();
        glEnable(GL_TEXTURE_2D);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    public void drawTexturedQuad(int texId, int x, int y, int w, int h, int texW, int texH) {
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
    
    public int getCurrentLevelIndex(){
        return currentLevelIndex;
    }
    
    private void keyPressed(int key) {
        System.out.println("Key pressed: " + key);
        
        if (key == GLFW_KEY_ESCAPE) {
            if (currentGameState == GameState.IN_GAME) {
                currentGameState = GameState.PAUSED;
            } else if (currentGameState == GameState.PAUSED) {
                currentGameState = GameState.IN_GAME;
            } else if (currentGameState == GameState.TITLE_SCREEN) {
                glfwSetWindowShouldClose(window, true);
            } else if (currentGameState == GameState.OPTIONS_MENU) {
                // Go back to title screen or previous menu
                if (currentOptionsMenu == 0) {
                    currentGameState = GameState.TITLE_SCREEN;
                    titleScreenNeedsUpdate = true;
                } else {
                    currentOptionsMenu = 0;
                    selectedOption = 0;
                    optionsMenuNeedsUpdate = true;
                }
            }
            return;
        }
        
        switch (currentGameState) {
            case TITLE_SCREEN:
                handleTitleScreenKeyPress(key);
                break;
            case OPTIONS_MENU:
                handleOptionsMenuKeyPress(key);
                break;
            case IN_GAME:
                handleInGameKeyPress(key);
                break;
            case PAUSED:
                handlePauseMenuKeyPress(key);
                break;
        }
    }
    
    private void handleTitleScreenKeyPress(int key) {
        switch (key) {
            case GLFW_KEY_UP:
            case GLFW_KEY_W:
                titleScreenSelectedOption = (titleScreenSelectedOption - 1 + titleScreenOptions.length) % titleScreenOptions.length;
                titleScreenNeedsUpdate = true;
                break;
            case GLFW_KEY_DOWN:
            case GLFW_KEY_S:
                titleScreenSelectedOption = (titleScreenSelectedOption + 1) % titleScreenOptions.length;
                titleScreenNeedsUpdate = true;
                break;
            case GLFW_KEY_ENTER:
            case GLFW_KEY_SPACE:
                handleTitleScreenOption(titleScreenSelectedOption);
                break;
        }
    }
    
    private void handleOptionsMenuKeyPress(int key) {
        String[] currentMenuItems = getCurrentMenuItems();
        
        switch (key) {
            case GLFW_KEY_UP:
            case GLFW_KEY_W:
                selectedOption = (selectedOption - 1 + currentMenuItems.length) % currentMenuItems.length;
                optionsMenuNeedsUpdate = true;
                break;
            case GLFW_KEY_DOWN:
            case GLFW_KEY_S:
                selectedOption = (selectedOption + 1) % currentMenuItems.length;
                optionsMenuNeedsUpdate = true;
                break;
            case GLFW_KEY_ENTER:
            case GLFW_KEY_SPACE:
                handleOptionsMenuOption(selectedOption);
                break;
            case GLFW_KEY_LEFT:
            case GLFW_KEY_A:
                adjustSetting(-1);
                break;
            case GLFW_KEY_RIGHT:
            case GLFW_KEY_D:
                adjustSetting(1);
                break;
        }
    }
    
    private void handleInGameKeyPress(int key) {
        if (messageBoxDisplayed) {
            if (key == GLFW_KEY_E) {
                closeMessage();
                messageBoxOptionsDisplayed = false;
                responsePositions = null;
            }
            return;
        }
        
        pressedKey[0] = pressedKey[1];
        pressedKey[1] = key;
       
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
            case GLFW_KEY_P:
                System.out.println("Player position in window: (" + playerX + ", " + playerY + ")" + " " + "Player position in grid: (" + playerPositionInWindowToPositionInGridX(playerX, playerY) + ", " + playerPositionInWindowToPositionInGridY(playerX, playerY) + ")");
                break;
            case GLFW_KEY_I:
                drawResponseTextOnMessageBox(new String[]{"Option 1", "Option 2", "Option 3"});
                break;
            case GLFW_KEY_G:
                for(int[] row : collisionGrid){
                    for(int cell : row){
                        System.out.print(cell + " ");
                    }
                    System.out.println();
                }
                break;
            case GLFW_KEY_1:
                collisionGrid = parser.getCollisionGrid(currentLevelIndex);
                for (Entity entity : entities) {
                    if(!entity.getType().equals("player")){
                        for(int i = 0; i <= 3-1; i++){
                            for(int j = 0; j <= 5-1; j++){
                                collisionGrid[(entity.getY() / 24) + j][(entity.getX() / 24) + i] = 1;
                            }
                        }
                    }
                }
                break;
            case GLFW_KEY_2:
                buildLevel(currentLevelIndex);
                break;
            case GLFW_KEY_T:
                currentGameState = GameState.TITLE_SCREEN;
                break;
            case GLFW_KEY_O:
                // Open options menu from game
                currentGameState = GameState.OPTIONS_MENU;
                break;
        }
        System.out.println("xVelocity: " + xVelocity + ", yVelocity: " + yVelocity);
        
        if(pressedKey[0] != 0 && pressedKey[1] != 0){
            boolean firstKeyIsVertical = (pressedKey[0] == GLFW_KEY_UP || pressedKey[0] == GLFW_KEY_W || pressedKey[0] == GLFW_KEY_DOWN || pressedKey[0] == GLFW_KEY_S);
            boolean secondKeyIsVertical = (pressedKey[1] == GLFW_KEY_UP || pressedKey[1] == GLFW_KEY_W || pressedKey[1] == GLFW_KEY_DOWN || pressedKey[1] == GLFW_KEY_S);
            if(firstKeyIsVertical && !secondKeyIsVertical){
                yVelocity = 0;
            } else if(!firstKeyIsVertical && secondKeyIsVertical){
                xVelocity = 0;
            }
        }
    }
    
    private void handlePauseMenuKeyPress(int key) {
        // TODO: Implement pause menu navigation
    }
    
    private void keyReleased(int key) {
        if (currentGameState != GameState.IN_GAME || messageBoxDisplayed) return;
        
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
    
    private void cursorCallback(long window, double xpos, double ypos) {
        cursorXPosition = (int)xpos;
        cursorYPosition = (int)ypos;
    }
    
    private void mouseClickCallback(long window, int button, int action, int mods) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            System.out.println("Left mouse at " + cursorXPosition + ", " + cursorYPosition + ", position on grid: (" + playerPositionInWindowToPositionInGridX(cursorXPosition, cursorYPosition) + ", " + playerPositionInWindowToPositionInGridY(cursorXPosition, cursorYPosition) + ")");
            leftMouseButtonPressed = true;
        }
        
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            if (messageBoxDisplayed && !messageBoxOptionsDisplayed) {
                closeMessage();
            }
        }
    }
    
    private void updateTitleScreenTexture() {
        Graphics2D g = titleScreenTextureImage.createGraphics();
        
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, winW, winH);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        String title = "FBLA Game";
        FontMetrics fm = g.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g.drawString(title, (winW - titleWidth) / 2, winH / 3);
        
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        int optionHeight = 40;
        int startY = winH / 2;
        titleScreenOptionsPositions = new int[titleScreenOptions.length][4];
        for (int i = 0; i < titleScreenOptions.length; i++) {
            if (i == titleScreenSelectedOption) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            
            fm = g.getFontMetrics();
            int optionWidth = fm.stringWidth(titleScreenOptions[i]);
            g.drawString(titleScreenOptions[i], (winW - optionWidth) / 2, (startY + i * optionHeight) + 20);
            titleScreenOptionsPositions[i][0] = (winW - optionWidth) / 2;
            titleScreenOptionsPositions[i][1] = startY + i * optionHeight;
            titleScreenOptionsPositions[i][2] = optionWidth;
            titleScreenOptionsPositions[i][3] = optionHeight;
        }
        
        // Draw instructions
        g.setColor(Color.GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        String instructions = "Use arrow keys/WASD to navigate, ENTER/SPACE to select, ESC to exit";
        int instructionsWidth = g.getFontMetrics().stringWidth(instructions);
        g.drawString(instructions, (winW - instructionsWidth) / 2, winH - 30);
        
        g.dispose();
        
        uploadTextureToGL(titleScreenTextureImage, titleScreenTextureGL);
    }
    
    private void updateOptionsMenuTexture() {
        optionsMenuTextureImage = new BufferedImage(winW, winH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = optionsMenuTextureImage.createGraphics();
        
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, winW, winH);
        
        // Draw menu title
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        String title = "Options";
        if (currentOptionsMenu == 1) title = "Graphics";
        else if (currentOptionsMenu == 2) title = "Audio";
        else if (currentOptionsMenu == 3) title = "Controls";
        FontMetrics fm = g.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g.drawString(title, (winW - titleWidth) / 2, winH / 4);
        
        // Draw menu items
        String[] currentMenuItems = getCurrentMenuItems();
        optionsMenuPositions = new int[currentMenuItems.length][4];
        
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        int optionHeight = 40;
        int startY = winH / 3;
        
        for (int i = 0; i < currentMenuItems.length; i++) {
            if (i == selectedOption) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            
            String menuText = currentMenuItems[i];
            
            // Add setting values for specific options
            if (currentOptionsMenu == 0) {
                // Main options menu
                menuText = currentMenuItems[i];
            } else if (currentOptionsMenu == 1) {
                // Graphics submenu
                switch (currentMenuItems[i]) {
                    case "Fullscreen":
                        menuText = "Fullscreen: " + (fullscreen ? "ON" : "OFF");
                        break;
                    case "VSync":
                        menuText = "VSync: " + (vsync ? "ON" : "OFF");
                        break;
                    case "Frame Rate Limit":
                        menuText = "Frame Rate: " + frameRateLimit + " FPS";
                        break;
                }
            } else if (currentOptionsMenu == 2) {
                // Audio submenu
                switch (currentMenuItems[i]) {
                    case "Master Volume":
                        menuText = "Master Volume: " + masterVolume + "%";
                        break;
                    case "Music Volume":
                        menuText = "Music Volume: " + musicVolume + "%";
                        break;
                    case "SFX Volume":
                        menuText = "SFX Volume: " + sfxVolume + "%";
                        break;
                }
            } else if (currentOptionsMenu == 3) {
                // Controls submenu
                switch (currentMenuItems[i]) {
                    case "Invert Y-Axis":
                        menuText = "Invert Y-Axis: " + (invertYAxis ? "ON" : "OFF");
                        break;
                    case "Mouse Sensitivity":
                        menuText = String.format("Mouse Sensitivity: %.1f", mouseSensitivity);
                        break;
                }
            }
            
            fm = g.getFontMetrics();
            int optionWidth = fm.stringWidth(menuText);
            g.drawString(menuText, (winW - optionWidth) / 2, (startY + i * optionHeight) + 20);
            optionsMenuPositions[i][0] = (winW - optionWidth) / 2;
            optionsMenuPositions[i][1] = startY + i * optionHeight - 30;
            optionsMenuPositions[i][2] = optionWidth;
            optionsMenuPositions[i][3] = optionHeight;
        }
        
        // Draw instructions
        g.setColor(Color.GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        String instructions;
        if (currentOptionsMenu == 0) {
            instructions = "Use arrow keys/WASD to navigate, ENTER/SPACE to select, ESC to go back";
        } else {
            instructions = "Use arrow keys/WASD to navigate and adjust, ENTER/SPACE to select, ESC to go back";
        }
        int instructionsWidth = g.getFontMetrics().stringWidth(instructions);
        g.drawString(instructions, (winW - instructionsWidth) / 2, winH - 30);
        
        g.dispose();
        
        uploadTextureToGL(optionsMenuTextureImage, optionsMenuTextureGL);
    }
    
    private void uploadTextureToGL(BufferedImage image, int textureID) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = new int[w * h];
        image.getRGB(0, 0, w, h, pixels, 0, w);
        
        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = pixels[y * w + x];
                buf.put((byte) ((pixel >> 16) & 0xFF));
                buf.put((byte) ((pixel >> 8) & 0xFF));
                buf.put((byte) (pixel & 0xFF));
                buf.put((byte) ((pixel >> 24) & 0xFF));
            }
        }
        
        buf.flip();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
    }
    
    private void handleTitleScreenOption(int optionIndex) {
        switch (optionIndex) {
            case 0: // Start Game
                startGame();
                break;
            case 1: // Options
                currentGameState = GameState.OPTIONS_MENU;
                currentOptionsMenu = 0;
                selectedOption = 0;
                optionsMenuNeedsUpdate = true;
                break;
            case 2: // Exit
                glfwSetWindowShouldClose(window, true);
                break;
        }
    }
    
    private void handleOptionsMenuClick() {
        String[] currentMenuItems = getCurrentMenuItems();
        for (int i = 0; i < currentMenuItems.length; i++) {
            int[] pos = optionsMenuPositions[i];
            if (cursorXPosition >= pos[0] && cursorXPosition <= pos[0] + pos[2] &&
                cursorYPosition >= pos[1] && cursorYPosition <= pos[1] + pos[3]) {
                handleOptionsMenuOption(i);
                break;
            }
        }
    }
    
    private void handleOptionsMenuOption(int optionIndex) {
        String[] currentMenuItems = getCurrentMenuItems();
        String selectedItem = currentMenuItems[optionIndex];
        
        switch (currentOptionsMenu) {
            case 0: // Main options menu
                switch (selectedItem) {
                    case "Graphics":
                        currentOptionsMenu = 1;
                        selectedOption = 0;
                        break;
                    case "Audio":
                        currentOptionsMenu = 2;
                        selectedOption = 0;
                        break;
                    case "Controls":
                        currentOptionsMenu = 3;
                        selectedOption = 0;
                        break;
                    case "Back":
                        currentGameState = GameState.TITLE_SCREEN;
                        titleScreenNeedsUpdate = true;
                        break;
                }
                break;
                
            case 1: // Graphics submenu
                switch (selectedItem) {
                    case "Fullscreen":
                        fullscreen = !fullscreen;
                        applyGraphicsSettings();
                        break;
                    case "VSync":
                        vsync = !vsync;
                        applyGraphicsSettings();
                        break;
                    case "Frame Rate Limit":
                        // Cycle through common frame rates
                        if (frameRateLimit == 30) frameRateLimit = 60;
                        else if (frameRateLimit == 60) frameRateLimit = 120;
                        else if (frameRateLimit == 120) frameRateLimit = 144;
                        else frameRateLimit = 30;
                        applyGraphicsSettings();
                        break;
                    case "Back":
                        currentOptionsMenu = 0;
                        selectedOption = 0;
                        break;
                }
                break;
                
            case 2: // Audio submenu
                switch (selectedItem) {
                    case "Master Volume":
                        masterVolume = Math.min(100, Math.max(0, masterVolume + 10));
                        if (masterVolume >= 100) masterVolume = 0;
                        break;
                    case "Music Volume":
                        musicVolume = Math.min(100, Math.max(0, musicVolume + 10));
                        if (musicVolume >= 100) musicVolume = 0;
                        break;
                    case "SFX Volume":
                        sfxVolume = Math.min(100, Math.max(0, sfxVolume + 10));
                        if (sfxVolume >= 100) sfxVolume = 0;
                        break;
                    case "Back":
                        currentOptionsMenu = 0;
                        selectedOption = 0;
                        break;
                }
                break;
                
            case 3: // Controls submenu
                switch (selectedItem) {
                    case "Invert Y-Axis":
                        invertYAxis = !invertYAxis;
                        break;
                    case "Mouse Sensitivity":
                        mouseSensitivity += 0.1;
                        if (mouseSensitivity > 2.0) mouseSensitivity = 0.5;
                        break;
                    case "Back":
                        currentOptionsMenu = 0;
                        selectedOption = 0;
                        break;
                }
                break;
        }
        
        optionsMenuNeedsUpdate = true;
    }
    
    private void adjustSetting(int direction) {
        String[] currentMenuItems = getCurrentMenuItems();
        String selectedItem = currentMenuItems[selectedOption];
        
        switch (currentOptionsMenu) {
            case 1: // Graphics
                switch (selectedItem) {
                    case "Frame Rate Limit":
                        frameRateLimit = Math.max(30, Math.min(240, frameRateLimit + direction * 30));
                        applyGraphicsSettings();
                        break;
                }
                break;
            case 2: // Audio
                switch (selectedItem) {
                    case "Master Volume":
                        masterVolume = Math.min(100, Math.max(0, masterVolume + direction * 5));
                        break;
                    case "Music Volume":
                        musicVolume = Math.min(100, Math.max(0, musicVolume + direction * 5));
                        break;
                    case "SFX Volume":
                        sfxVolume = Math.min(100, Math.max(0, sfxVolume + direction * 5));
                        break;
                }
                break;
            case 3: // Controls
                switch (selectedItem) {
                    case "Mouse Sensitivity":
                        mouseSensitivity = Math.max(0.1, Math.min(5.0, mouseSensitivity + direction * 0.1));
                        mouseSensitivity = Math.round(mouseSensitivity * 10) / 10.0;
                        break;
                }
                break;
        }
        
        optionsMenuNeedsUpdate = true;
    }
    
    private void applyGraphicsSettings() {
        // Apply frame rate
        FRAMERATE = frameRateLimit;
        
        // Apply VSync
        glfwSwapInterval(vsync ? 1 : 0);
        
        // Note: Fullscreen would require recreating the window
        System.out.println("Graphics settings applied: Frame Rate=" + frameRateLimit + 
                         ", VSync=" + vsync + ", Fullscreen=" + fullscreen);
    }

    // === Message box handling ===
    public void displayMessage(String message) {
        if (messageBoxDisplayed) return;
        System.out.println("Displaying message: " + message);
        yVelocity = 0;
        xVelocity = 0;
        messageBoxDisplayed = true;
        currentFullMessage = message;
        typeIndex = 0;
        lastTypeTime = System.currentTimeMillis();
        updateMessageTexture(message);
        playTalkingSound();
    }

    public void displayMessageWithResponses(String message, String[] responses) {
        if (messageBoxDisplayed) return;
        closeMessage();
        System.out.println("Displaying message with responses: " + message);
        yVelocity = 0;
        xVelocity = 0;
        messageBoxOptionsDisplayed = true;
        messageBoxDisplayed = true;
        currentFullMessage = message;
        updateMessageTexture(message);
        drawResponseTextOnMessageBox(responses);
        playTalkingSound();
    }

    public void drawResponseTextOnMessageBox(String[] responses) {
        yVelocity = 0;
        xVelocity = 0;
        messageBoxOptionsDisplayed = true;
        messageBoxDisplayed = true;
        if(responses != null || responses.length != 0){
            currentResponseOptions = responses;
        } else {
            currentResponseOptions = new String[]{"Error: No responses"};
        }
        currentResponseOptions = responses;
        currentFullMessage = "test";
        int numberOfResponses = responses.length;
        responsePositions = new int[numberOfResponses][4];
        for(int i = 0; i < numberOfResponses; i++){
            Graphics2D g = messageTextureImage.createGraphics();
            g.setColor(Color.YELLOW);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            int padding = 12;
            int maxWidth = messageTextureImage.getWidth() - padding * 2;
            int lineHeight = 22;
            int startY = messageTextureImage.getHeight() - (numberOfResponses * lineHeight) - padding;
            g.drawString(responses[i], padding, startY + (i * lineHeight) + g.getFontMetrics().getAscent());
            int textWidth = g.getFontMetrics().stringWidth(responses[i]);
            responsePositions[i][0] = padding;
            responsePositions[i][1] = startY + (i * lineHeight);
            responsePositions[i][2] = textWidth;
            responsePositions[i][3] = lineHeight;
            g.dispose();
        }
        
        uploadTextureToGL(messageTextureImage, messageTextureGL);
        
        for(int i = 0; i < responsePositions.length; i++){
            responsePositions[i][0] += (winW - (winW / 2)) / 2;
            responsePositions[i][1] += (int) (winH * 0.75);
            responsePositions[i][1] += 22;
            responsePositions[i][2] += 10;
            responsePositions[i][3] += 10;
        }
        responsePositions[responsePositions.length - 1][3] += 10;
    }

    public void drawResponseTextOnMessageBoxWithHighlight(String[] responses, int indexOfHighlightedOption) {
        yVelocity = 0;
        xVelocity = 0;
        messageBoxOptionsDisplayed = true;
        messageBoxDisplayed = true;
        currentFullMessage = "test";
        int numberOfResponses = responses.length;
        responsePositions = new int[numberOfResponses][4];
        for(int i = 0; i < numberOfResponses; i++){
            if(i == indexOfHighlightedOption){
                Graphics2D g = messageTextureImage.createGraphics();
                g.setColor(Color.RED);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                int padding = 12;
                int maxWidth = messageTextureImage.getWidth() - padding * 2;
                int lineHeight = 22;
                int startY = messageTextureImage.getHeight() - (numberOfResponses * lineHeight) - padding;
                g.drawString(responses[i], padding, startY + (i * lineHeight) + g.getFontMetrics().getAscent());
                int textWidth = g.getFontMetrics().stringWidth(responses[i]);
                responsePositions[i][0] = padding;
                responsePositions[i][1] = startY + (i * lineHeight);
                responsePositions[i][2] = textWidth;
                responsePositions[i][3] = lineHeight;
                g.dispose();
                continue;
            }
            Graphics2D g = messageTextureImage.createGraphics();
            g.setColor(Color.YELLOW);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            int padding = 12;
            int maxWidth = messageTextureImage.getWidth() - padding * 2;
            int lineHeight = 22;
            int startY = messageTextureImage.getHeight() - (numberOfResponses * lineHeight) - padding;
            g.drawString(responses[i], padding, startY + (i * lineHeight) + g.getFontMetrics().getAscent());
            int textWidth = g.getFontMetrics().stringWidth(responses[i]);
            responsePositions[i][0] = padding;
            responsePositions[i][1] = startY + (i * lineHeight);
            responsePositions[i][2] = textWidth;
            responsePositions[i][3] = lineHeight;
            g.dispose();
        }
        
        uploadTextureToGL(messageTextureImage, messageTextureGL);
        
        for(int i = 0; i < responsePositions.length; i++){
            responsePositions[i][0] += (winW - (winW / 2)) / 2;
            responsePositions[i][1] += (int) (winH * 0.75);
            responsePositions[i][1] += 22;
            responsePositions[i][2] += 10;
            responsePositions[i][3] += 10;
        }
        responsePositions[responsePositions.length - 1][3] += 10;
    }

    public void closeMessage() {
        if (!messageBoxDisplayed) return;
        messageBoxDisplayed = false;
        currentFullMessage = "";
        typeIndex = 0;
        messageBoxOptionsDisplayed = false;
        responsePositions = null;
    }

    void changeColorOfResponseOptionMouseOver(){
        if(messageBoxOptionsDisplayed && responsePositions != null){
            for(int i = 0; i < responsePositions.length; i++){
                int[] pos = responsePositions[i];
                if(cursorXPosition >= pos[0] && cursorXPosition <= pos[0] + pos[2] &&
                   cursorYPosition >= pos[1] && cursorYPosition <= pos[1] + pos[3]){
                    drawResponseTextOnMessageBoxWithHighlight(currentResponseOptions, i);
                    return;
                }
            }
        }
    }

    private void updateMessageTexture(String currentFullMessage) {
        System.out.println("Updating message texture: " + currentFullMessage);
        String text = currentFullMessage;

        Graphics2D g = messageTextureImage.createGraphics();
        g.drawImage(messageBoxBI, 0, 0, messageTextureImage.getWidth(), messageTextureImage.getHeight(), null);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        int padding = 12;
        int maxWidth = messageTextureImage.getWidth() - padding * 2;
        drawStringWrapped(g, text, padding, padding, maxWidth, 22);

        g.dispose();

        uploadTextureToGL(messageTextureImage, messageTextureGL);
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

    private void checkIfPlayerClickedResponses(int[][] responsePositions) {
        if(leftMouseButtonPressed){
            for(int i = 0; i < responsePositions.length; i++){
                int[] pos = responsePositions[i];
                if(cursorXPosition >= pos[0] && cursorXPosition <= pos[0] + pos[2] &&
                   cursorYPosition >= pos[1] && cursorYPosition <= pos[1] + pos[3]){
                    System.out.println("Player clicked on response: " + (i + 1));
                    closeMessage();
                    leftMouseButtonPressed = false;
                    return;
                }
            }
            leftMouseButtonPressed = false;
        }
    }

    public int[][] getCollisionGrid(){
        return collisionGrid;
    }

    public List<Entity> getEntities(){
        return entities;
    }

    public void setEntityMovement(int entityIndex, int directionIndex, int value){
        entityMovement[entityIndex][directionIndex] = value;
    }

    private void handleNPCActions(String action, Entity npc){
        if(action.startsWith("move_to")){
            int start = action.indexOf('(');
            int end = action.indexOf(')');
            if(start == -1 || end == -1 || end <= start + 1) return;
            String[] parts = action.substring(start + 1, end).split(",");
            if(parts.length != 2) return;
            try {
                int targetX = Integer.parseInt(parts[0].trim()) * GRID_CELL_SIZE;
                int targetY = Integer.parseInt(parts[1].trim()) * GRID_CELL_SIZE;
                Thread pathfindingThread = new Thread(() -> {
                    entityAIs.get(entities.indexOf(npc)).NPCPathfindToPoint(playerPositionInWindowToPositionInGridX(npc.getX(), npc.getY()), playerPositionInWindowToPositionInGridY(npc.getX(), npc.getY()), targetX / GRID_CELL_SIZE, targetY / GRID_CELL_SIZE, npc);
                });
                pathfindingThread.start();
                System.out.println("Moved NPC to (" + targetX + ", " + targetY + ")");
            } catch (NumberFormatException e) {
                System.err.println("Invalid move_to action format: " + action);
            }
        } else if(action.startsWith("change_player_level")){
            int start = action.indexOf('(');
            int end = action.indexOf(')');
            if(start == -1 || end == -1 || end <= start + 1) return;
            int level = Integer.parseInt(action.substring(start + 1, end));
            buildLevel(level);
            closeMessage();
        } else if(action.startsWith("exit_game")){
            cleanup();
            System.exit(0);
        }
    }

    public void dialogueHandler(dialogueTree tree, int selectedResponse, Entity npc) {
        closeMessage();
        String npcText = tree.getNpcText();
        List<Response> playerResponses = tree.getResponses();
        String[] responses = new String[playerResponses.size()];
        for(int i = 0; i < playerResponses.size(); i++){
            responses[i] = playerResponses.get(i).getResponseText();
        }
        if(selectedResponse == 0){
            displayMessageWithResponses(npcText, responses);
            return;
        }
        for(int i = 0; i < responses.length; i++){
            System.out.println((i + 1) + ": " + responses[i]);
        }
        for(int i = 0; i < playerResponses.size(); i++){
            if(i == selectedResponse - 1 && responses.length > 0){
                Response r = playerResponses.get(i);
                if(currentTree.getResponses().isEmpty()){
                    System.out.println("End of conversation reached.");
                    displayMessage(currentTree.getNpcText());
                    messageBoxOptionsDisplayed = false;
                    responsePositions = null;
                    responses = null;
                } else if(r.getNextNode() != null && r.getNextNode().getResponses().size() > 0){
                    dialogueHandler(r.getNextNode(), 0, npc);
                    currentTree = r.getNextNode();
                } else if(r.getNextNode() != null && r.getNextNode().getResponses().size() == 0) {
                    closeMessage();
                    messageBoxOptionsDisplayed = false;
                    responsePositions = null;
                    responses = null;
                    currentTree = r.getNextNode();
                    displayMessage(r.getNextNode().getNpcText());
                    if(r.getNextNode().getNpcAction() != null){
                        handleNPCActions(r.getNextNode().getNpcAction(), npc);
                    }
                }
                return;
            }
        }
    }

    public void setCurrentDialogueTree(dialogueTree tree){
        currentTree = tree;
    }

    public void setCurrentNPCPlayerIsInteractingWith(Entity npc){
        currentNPC = npc;
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
        if(nearest != null && nearest.getType().equals("door")){
            buildLevel(nearest.getTargetLevel());
            return;
        }
        if(nearest != null && !nearest.getDialogueTree().isTreeEmpty()) {
            currentTree = nearest.getDialogueTree();
            currentNPC = nearest;
            dialogueHandler(nearest.getDialogueTree(), 0, nearest);
            return;
        }

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
            WavPlayer soundPlayer = new WavPlayer(RESOURCE_PATH + "\\audio\\talking.wav");
            soundPlayer.setName("soundPlayerThread");
            soundPlayer.setPriority(Thread.MAX_PRIORITY);
            soundPlayer.start();
        } catch (Exception e) {
            System.err.println("Could not play talking sound: " + e.getMessage());
        }
    }

    // === Cleanup ===
    private void cleanup() {
        glDeleteTextures(playerTex);
        glDeleteTextures(npcTex);
        glDeleteTextures(backgroundTex);
        glDeleteTextures(messageBoxTex);
        if (messageTextureGL != -1) glDeleteTextures(messageTextureGL);
        if (titleScreenTextureGL != -1) glDeleteTextures(titleScreenTextureGL);
        if (optionsMenuTextureGL != -1) glDeleteTextures(optionsMenuTextureGL);
    }
}