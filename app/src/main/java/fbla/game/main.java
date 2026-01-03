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
    // === Game state ===
    private long window;
    private int winW = WINDOW_W, winH = WINDOW_H;

    private final List<Entity> entities = new ArrayList<>();
    private List<EntityAI> entityAIs = new ArrayList<>(); // maps to the indices of entities list
    private BufferedImage playerBI, backgroundBI, npcBI, messageBoxBI, gridBI, doorBI;
    private int playerTex, backgroundTex, npcTex, messageBoxTex, gridTex, doorTex;

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

        // Entities: index 0 is player
        //entities.add(new Entity(null, playerTex, playerX, playerY, playerBI.getWidth(), playerBI.getHeight()));
        //entities.add(new Entity(null, npcTex, 200, 150, npcBI.getWidth(), npcBI.getHeight()));

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

    private void buildLevel(int levelIndex){
        currentLevelIndex = levelIndex;
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
        try {
            playerBI = ImageIO.read(new File(RESOURCE_PATH+"\\textures\\" + player.getImagePath()));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
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
        entityIndexToAnimationObjects.put(0, new entityAnimation(player, RESOURCE_PATH)); // map player entity index to its animation object
        entityMovement = new int[level.getEntities().size()][4]; // initialize entity movement array

        // add NPCs and other entities
        for(Entity e : level.getEntities()){
            if(!e.getType().equals("player")){
                entityAIs.add(new EntityAI(e, parser, this)); // create AI object for the entity
                entityIndexToAnimationObjects.put(entities.size(), new entityAnimation(e, RESOURCE_PATH)); // map entity index to its animation object
                System.out.println("Adding entity of type " + e.getType() + " at (" + e.getX() + ", " + e.getY() + ")");
                try {
                    e.setTextureId(createTextureFromBufferedImage(ImageIO.read(new File(RESOURCE_PATH+"\\textures\\" + e.getImagePath()))));
                    System.out.println("Loaded texture for entity from " + RESOURCE_PATH+"\\textures\\" + e.getImagePath() + ". Texture ID: " + e.getTextureId());
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
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
                // print the new collision grid
                for(int[] row : collisionGrid){
                    for(int cell : row){
                        System.out.print(cell + " ");
                    }
                    System.out.println();
                }
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

        if(yVelocity == 0 && xVelocity == 0){
            // reset player animation to idle
            entityMovement[0][0] = 0; // right
            entityMovement[0][1] = 0; // up
            entityMovement[0][2] = 0; // left
            entityMovement[0][3] = 0; // down
            entityIndexToAnimationObjects.get(0).tick("idle");
        } else if (xVelocity == 24){
            entityMovement[0][0] = 0; // right
            entityMovement[0][1] = 0; // up
            entityMovement[0][2] = 0; // left
            entityMovement[0][3] = 0; // down
            entityIndexToAnimationObjects.get(0).tick("walkingRight");
        } else if (xVelocity == -24){
            entityMovement[0][0] = 0; // right
            entityMovement[0][1] = 0; // up
            entityMovement[0][2] = 0; // left
            entityMovement[0][3] = 0; // down
            entityIndexToAnimationObjects.get(0).tick("walkingLeft");
        } else if( yVelocity == 24){
            entityMovement[0][0] = 0; // right
            entityMovement[0][1] = 0; // up
            entityMovement[0][2] = 0; // left
            entityMovement[0][3] = 0; // down
            // right now, the assets for walking up and down are unfinished, so just use idle for now
            // TODO: replace with walkingUp animation when available
            entityIndexToAnimationObjects.get(0).tick("idle");
        } else if (yVelocity == -24){
            entityMovement[0][0] = 0; // right
            entityMovement[0][1] = 0; // up
            entityMovement[0][2] = 0; // left
            entityMovement[0][3] = 0; // down
            // TODO: replace with walkingDown animation when available
            entityIndexToAnimationObjects.get(0).tick("idle");
        }

        entities.get(0).setPosition(playerX, playerY);

        for(int i = 1; i < entities.size(); i++){
            Entity npc = entities.get(i);
            if(entityMovement != null && entityMovement.length > i){
                if(entityMovement[i][0] > 0){
                    entityIndexToAnimationObjects.get(i).tick("walkingRight");
                    //entityAnimation(npc, "walkingRight");
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

        // Typewriter update if message is shown
        //if (messageBoxDisplayed ){//&& typeIndex <= currentFullMessage.length()) {
            //long now = System.currentTimeMillis();
            //if (now - lastTypeTime >= TYPEWRITER_DELAY_MS) {
                //typeIndex++;
                //lastTypeTime = now;
        //        displayMessageWithResponses(currentFullMessage, new String[]{"test 1", "test 2", "test 3"});
            //}
        //}
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
        if(DRAW_DEBUG_GRID){
            drawTexturedQuad(gridTex, 0, 0, winW, winH, gridBI.getWidth(), gridBI.getHeight());
        }

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

    public void drawTexturedQuad(int texId, int x, int y, int w, int h, int texW, int texH) {
        //System.out.println("Drawing textured quad at (" + x + ", " + y + ") with size (" + w + ", " + h + ") using texture ID " + texId);
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

    // === Input handling (GLFW key codes) ===
    private void keyPressed(int key) {
        System.out.println("Key pressed: " + key);
        if (messageBoxDisplayed) {
            if (key == GLFW_KEY_E) {
                closeMessage();
                messageBoxOptionsDisplayed = false;
                responsePositions = null;
            }
            return;
        }
        // handle smooth key transitions
        pressedKey[0] = pressedKey[1];
        pressedKey[1] = key;
        

        // handle movement keys
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
            case GLFW_KEY_I:
                // debug: display message box with test responses
                //displayMessage("This is a test message. Choose an option below:");
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
                // reset collision grid
                collisionGrid = parser.getCollisionGrid(currentLevelIndex);
                // add locations of npcs
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
        }
        System.out.println("xVelocity: " + xVelocity + ", yVelocity: " + yVelocity);
        // prevent diagonal movement while allowing for smooth key transitions
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

    private void cursorCallback(long window, double xpos, double ypos) {
        //System.out.println("Cursor moved to: (" + xpos + ", " + ypos + ")");
        cursorXPosition = (int)xpos;
        cursorYPosition = (int)ypos;
        
    }

    private void mouseClickCallback(long window, int button, int action, int mods) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            System.out.println("Left mouse at " + cursorXPosition + ", " + cursorYPosition + ", position on grid: (" + playerPositionInWindowToPositionInGridX(cursorXPosition, cursorYPosition) + ", " + playerPositionInWindowToPositionInGridY(cursorXPosition, cursorYPosition) + ")");
            leftMouseButtonPressed = true;
        }
        // if the message box is open and the conversation is over, the user can click anywhere to close it
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            if (messageBoxDisplayed && !messageBoxOptionsDisplayed) {
                closeMessage();
            }
        }
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
        // close old message
        closeMessage();
        System.out.println("Displaying message with responses: " + message);
        yVelocity = 0;
        xVelocity = 0;
        messageBoxOptionsDisplayed = true;
        messageBoxDisplayed = true;
        currentFullMessage = message;
        //typeIndex = 0;
        //lastTypeTime = System.currentTimeMillis();
        updateMessageTexture(message);
        drawResponseTextOnMessageBox(responses);
        playTalkingSound();
    }

    public void drawResponseTextOnMessageBox(String[] responses) {
        // draws text at the bottom of the message box for player responses and uses the cursor position to select a response
        // reuse code from updateMessageTexture to draw the text
        //if (messageBoxDisplayed) return;
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
        currentFullMessage = "test"; // dummy message to show the box
        int numberOfResponses = responses.length;
        responsePositions = new int[numberOfResponses][4]; // x, y, w, h for each response
        for(int i = 0; i < numberOfResponses; i++){
            Graphics2D g = messageTextureImage.createGraphics();
            g.setColor(Color.YELLOW);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            //g.drawImage(messageBoxBI, 0, 0, messageTextureImage.getWidth(), messageTextureImage.getHeight(), null);
            int padding = 12;
            int maxWidth = messageTextureImage.getWidth() - padding * 2;
            int lineHeight = 22;
            int startY = messageTextureImage.getHeight() - (numberOfResponses * lineHeight) - padding;
            g.drawString(responses[i], padding, startY + (i * lineHeight) + g.getFontMetrics().getAscent());
            // add response to responsePositions
            int textWidth = g.getFontMetrics().stringWidth(responses[i]);
            responsePositions[i][0] = padding; // x
            responsePositions[i][1] = startY + (i * lineHeight); // y
            responsePositions[i][2] = textWidth; // w
            responsePositions[i][3] = lineHeight; // h
            g.dispose();
        }
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

        System.out.println("Response positions: " + java.util.Arrays.deepToString(responsePositions));
            // since the options positions are relative to the message box, we need to offset them by the message box position in the window
            for(int i = 0; i < responsePositions.length; i++){
                responsePositions[i][0] += (winW - (winW / 2)) / 2; // boxX
                responsePositions[i][1] += (int) (winH * 0.75); // boxY
                // also shift the y position down by 22 pixels to account for the text ascent
                responsePositions[i][1] += 22;
                // and add some padding to the width and height
                responsePositions[i][2] += 10;
                responsePositions[i][3] += 10;
            }
            // specifically add extra padding to the last option to make it easier to click
            responsePositions[responsePositions.length - 1][3] += 10;
            System.out.println("Response positions in window: " + java.util.Arrays.deepToString(responsePositions));
    }

    public void drawResponseTextOnMessageBoxWithHighlight(String[] responses, int indexOfHighlightedOption) {
        // draws text at the bottom of the message box for player responses and uses the cursor position to select a response
        // reuse code from updateMessageTexture to draw the text
        //if (messageBoxDisplayed) return;
        yVelocity = 0;
        xVelocity = 0;
        messageBoxOptionsDisplayed = true;
        messageBoxDisplayed = true;
        currentFullMessage = "test"; // dummy message to show the box
        int numberOfResponses = responses.length;
        responsePositions = new int[numberOfResponses][4]; // x, y, w, h for each response
        for(int i = 0; i < numberOfResponses; i++){
            if(i == indexOfHighlightedOption){
                // draw highlighted option in different color
                Graphics2D g = messageTextureImage.createGraphics();
                g.setColor(Color.RED);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                int padding = 12;
                int maxWidth = messageTextureImage.getWidth() - padding * 2;
                int lineHeight = 22;
                int startY = messageTextureImage.getHeight() - (numberOfResponses * lineHeight) - padding;
                g.drawString(responses[i], padding, startY + (i * lineHeight) + g.getFontMetrics().getAscent());
                // add response to responsePositions
                int textWidth = g.getFontMetrics().stringWidth(responses[i]);
                responsePositions[i][0] = padding; // x
                responsePositions[i][1] = startY + (i * lineHeight); // y
                responsePositions[i][2] = textWidth; // w
                responsePositions[i][3] = lineHeight; // h
                g.dispose();
                continue;
            }
            Graphics2D g = messageTextureImage.createGraphics();
            g.setColor(Color.YELLOW);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            //g.drawImage(messageBoxBI, 0, 0, messageTextureImage.getWidth(), messageTextureImage.getHeight(), null);
            int padding = 12;
            int maxWidth = messageTextureImage.getWidth() - padding * 2;
            int lineHeight = 22;
            int startY = messageTextureImage.getHeight() - (numberOfResponses * lineHeight) - padding;
            g.drawString(responses[i], padding, startY + (i * lineHeight) + g.getFontMetrics().getAscent());
            // add response to responsePositions
            int textWidth = g.getFontMetrics().stringWidth(responses[i]);
            responsePositions[i][0] = padding; // x
            responsePositions[i][1] = startY + (i * lineHeight); // y
            responsePositions[i][2] = textWidth; // w
            responsePositions[i][3] = lineHeight; // h
            g.dispose();
        }
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

        System.out.println("Response positions: " + java.util.Arrays.deepToString(responsePositions));
            // since the options positions are relative to the message box, we need to offset them by the message box position in the window
            for(int i = 0; i < responsePositions.length; i++){
                responsePositions[i][0] += (winW - (winW / 2)) / 2; // boxX
                responsePositions[i][1] += (int) (winH * 0.75); // boxY
                // also shift the y position down by 22 pixels to account for the text ascent
                responsePositions[i][1] += 22;
                // and add some padding to the width and height
                responsePositions[i][2] += 10;
                responsePositions[i][3] += 10;
            }
            // specifically add extra padding to the last option to make it easier to click
            responsePositions[responsePositions.length - 1][3] += 10;
            System.out.println("Response positions in window: " + java.util.Arrays.deepToString(responsePositions));
    }

    public void closeMessage() {
        if (!messageBoxDisplayed) return;
        messageBoxDisplayed = false;
        currentFullMessage = "";
        typeIndex = 0;
        messageBoxOptionsDisplayed = false;
        responsePositions = null;
        //soundPlayer.stopAudio();
    }

    void changeColorOfResponseOptionMouseOver(){
        // check if cursor is over any response option and change its color
        if(messageBoxOptionsDisplayed && responsePositions != null){
            for(int i = 0; i < responsePositions.length; i++){
                int[] pos = responsePositions[i];
                if(cursorXPosition >= pos[0] && cursorXPosition <= pos[0] + pos[2] &&
                   cursorYPosition >= pos[1] && cursorYPosition <= pos[1] + pos[3]){
                    System.out.println(java.util.Arrays.deepToString(currentResponseOptions));
                    System.out.println(currentResponseOptions[i] + " is being hovered over");
                    drawResponseTextOnMessageBoxWithHighlight(currentResponseOptions, i);
                    return;
                }
            }
        }
    }

    private void updateMessageTexture(String currentFullMessage) {
        System.out.println("Updating message texture: " + currentFullMessage);
        // Compose message box image with current substring text
        String text = currentFullMessage;//.substring(0, Math.min(typeIndex, currentFullMessage.length()));

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

    private void checkIfPlayerClickedResponses(int[][] responsePositions) {
        // check if the player clicked on one of the response options in the message box
        // use responsePositions to check if cursorXPosition and cursorYPosition are within any of the response boxes
        // responsePotions is an array of [x, y, w, h] for each response
        if(leftMouseButtonPressed){
            for(int i = 0; i < responsePositions.length; i++){
                int[] pos = responsePositions[i];
                if(cursorXPosition >= pos[0] && cursorXPosition <= pos[0] + pos[2] &&
                   cursorYPosition >= pos[1] && cursorYPosition <= pos[1] + pos[3]){
                    System.out.println("Player clicked on response: " + (i + 1));
                    // close message box after clicking a response
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
        // directionIndex: 0 = right, 1 = up, 2 = left, 3 = down
        entityMovement[entityIndex][directionIndex] = value;
    }

    private void handleNPCActions(String action, Entity npc){
        // this method is primarily meant to handle NPC actions that are triggered from dialogue trees
        // right now, the only action that an NPC will do is to move to a new cell.
        // here is what the action looks like: move_to(11,2)
        if(action.startsWith("move_to")){
            // parse the action to get the target cell
            int start = action.indexOf('(');
            int end = action.indexOf(')');
            if(start == -1 || end == -1 || end <= start + 1) return;
            String[] parts = action.substring(start + 1, end).split(",");
            if(parts.length != 2) return;
            try {
                int targetX = Integer.parseInt(parts[0].trim()) * GRID_CELL_SIZE;
                int targetY = Integer.parseInt(parts[1].trim()) * GRID_CELL_SIZE;
                int oldPositionX = npc.getX();
                int oldPositionY = npc.getY();
                // move the npc to the target cell
                System.out.println("Start position: " + playerPositionInWindowToPositionInGridX(npc.getX(), npc.getY()) + ", " +  playerPositionInWindowToPositionInGridX(npc.getX(), npc.getY()) + " Target position: " + targetX / GRID_CELL_SIZE + ", " + targetY / GRID_CELL_SIZE);
                //NPCPathfindToPoint(collisionGrid, playerPositionInWindowToPositionInGridX(npc.getX(), npc.getY()), playerPositionInWindowToPositionInGridX(npc.getX(), npc.getY()), targetX / GRID_CELL_SIZE, targetY / GRID_CELL_SIZE, npc);
                Thread pathfindingThread = new Thread(() -> {
                    entityAIs.get(entities.indexOf(npc)).NPCPathfindToPoint(playerPositionInWindowToPositionInGridX(npc.getX(), npc.getY()), playerPositionInWindowToPositionInGridY(npc.getX(), npc.getY()), targetX / GRID_CELL_SIZE, targetY / GRID_CELL_SIZE, npc);
                });
                pathfindingThread.start();
                System.out.println("Moved NPC to (" + targetX + ", " + targetY + ")");
                //try {
                //    pathfindingThread.join();
                //} catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                //    e.printStackTrace();
               // }
            } catch (NumberFormatException e) {
                System.err.println("Invalid move_to action format: " + action);
            }
        } else if(action.startsWith("change_player_level")){ // only used for the title screen, since we are reusing an npc for the title screen choices
            int start = action.indexOf('(');
            int end = action.indexOf(')');
            if(start == -1 || end == -1 || end <= start + 1) return;
            int level = Integer.parseInt(action.substring(start + 1, end));
            buildLevel(level);
        } else if(action.startsWith("exit_game")){
            cleanup();
            System.exit(0);
        }
    }

    public void dialogueHandler(dialogueTree tree, int selectedResponse, Entity npc) {
        // handle dialogue tree traversal
        // if selected response is zero, assume we are at the top level of the tree
        // close old message box
        closeMessage();
        String npcText = tree.getNpcText();
        List<Response> playerResponses = tree.getResponses();
        String[] responses = new String[playerResponses.size()];
        for(int i = 0; i < playerResponses.size(); i++){
            responses[i] = playerResponses.get(i).getResponseText();
        }
        if(selectedResponse == 0){
            // display top level npc text and responses
            displayMessageWithResponses(npcText, responses);
            return;
        }
        // print all responses for debugging
        for(int i = 0; i < responses.length; i++){
            System.out.println((i + 1) + ": " + responses[i]);
        }
        // enumerate through the responses to find the selected one
        for(int i = 0; i < playerResponses.size(); i++){
            // check to ensure that there are actually responses to select from
            if(i == selectedResponse - 1 && responses.length > 0){
                // remember that a selectedresponse of 0 is the top level, so subtract 1 from the index
                Response r = playerResponses.get(i);
                // if there are no responses, just display the npc text, this is the end of the conversation
                if(currentTree.getResponses().isEmpty()){
                    System.out.println("End of conversation reached.");
                    displayMessage(currentTree.getNpcText());
                    messageBoxOptionsDisplayed = false;
                    responsePositions = null;
                    responses = null;
                } else if(r.getNextNode() != null && r.getNextNode().getResponses().size() > 0){
                    // display the npc text for the selected response
                    dialogueHandler(r.getNextNode(), 0, npc);
                    // refresh playerresponses and responses array for the new node
                    currentTree = r.getNextNode();
                } else if(r.getNextNode() != null && r.getNextNode().getResponses().size() == 0) {
                    closeMessage();
                    messageBoxOptionsDisplayed = false;
                    responsePositions = null;
                    responses = null;
                    currentTree = r.getNextNode();
                    displayMessage(r.getNextNode().getNpcText());
                    // run an npc action if it has one, these should always be at the end of the tree
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
        // if the nearest npc is a door, run the build level function with the door's target level
        if(nearest != null && nearest.getType().equals("door")){
            buildLevel(nearest.getTargetLevel());
            return;
        }
        // display npc conversation if npc has one defined
        if(nearest != null && !nearest.getDialogueTree().isTreeEmpty()) {
            // start dialogue tree, the 0 means the top level of the tree
            currentTree = nearest.getDialogueTree();
            currentNPC = nearest;
            dialogueHandler(nearest.getDialogueTree(), 0, nearest);
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
        //soundPlayer.stopAudio();
    }
}
