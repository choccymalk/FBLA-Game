package fbla.game;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.flag.ImGuiWindowFlags;
import imgui.ImGui.*;
import imgui.flag.*;
import imgui.flag.ImGuiWindowFlags.*;
import imgui.ImGuiIO.*;
import imgui.ImGuiIO;
import imgui.ImFontConfig;
import imgui.ImFontGlyphRangesBuilder;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import imgui.ImGuiStyle;
import imgui.ImVec4;
import imgui.type.ImBoolean;
//import imgui.ImGuiCol;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import fbla.game.GameRenderer.Object3D;

public class main {
    // === Config ===
    private static final int WINDOW_W = 1280;
    private static final int WINDOW_H = 720;
    private static final int MOVE_AMOUNT = 24;
    public static final int MOVEMENT_DELAY_MS = 75;
    private static final double NPC_INTERACTION_DISTANCE = 500.0;
    private static final String RESOURCE_PATH = System.getProperty("user.home")
            + "\\Desktop\\FBLA-Game\\game_resources";
    public double FRAMERATE = 10.0;
    public double UPDATE_RATE = 10.0;
    public int soundPlayerVolume = 50;
    private static final int GRID_CELL_SIZE = 24;
    private static final int GRID_WIDTH = 53;
    private static final int GRID_HEIGHT = 30;
    private static final int ENTITY_WIDTH_CELLS = 3;
    private static final int ENTITY_HEIGHT_CELLS = 5;
    private static final int DOOR_WIDTH = 96;
    private static final int DOOR_HEIGHT = 144;
    public boolean DRAW_DEBUG_GRID;
    private static final String DEFAULT_FONT = "roboto";
    private static final int DEFAULT_FONT_SIZE = 20;
    private static boolean FULLSCREEN = false;
    ImBoolean isFullscreen;
    ImBoolean shouldDebugGridBeDrawn;
    public FullscreenToggle fstoggle;
    private GameRenderer renderer;
    Object3D model;
    // Model3D model;

    public enum GameState {
        TITLE_SCREEN,
        IN_GAME,
        PAUSED,
        OPTIONS
    }

    public long window;
    public int winW = WINDOW_W, winH = WINDOW_H;
    public final List<Entity> entities = new ArrayList<>();
    public List<EntityAI> entityAIs = new ArrayList<>();
    public BufferedImage playerBI, backgroundBI, npcBI, gridBI, doorBI, titleScreenBI, titleScreenGameLogoBI, fishBI;
    public int playerTex, backgroundTex, npcTex, gridTex, doorTex, titleScreenTex, titleScreenGameLogoTex, fishTex;
    public java.util.HashMap<Integer, entityAnimation> entityIndexToAnimationObjects = new java.util.HashMap<>();
    public int playerX = 0, playerY = 600;
    private int xVelocity = 0, yVelocity = 0;
    private int cursorXPosition;
    private int cursorYPosition;
    private boolean leftMouseButtonPressed = false;
    public String[] currentResponseOptions;
    public dialogueTree currentTree = null;
    public Entity currentNPC = null;
    public int currentLevelIndex;
    public int[][] entityMovement;
    private int[] pressedKey = new int[2];
    jsonParser parser = new jsonParser(new File(RESOURCE_PATH + "\\levels.json"));
    int[][] collisionGrid = parser.getCollisionGrid(0);
    public GameState currentGameState = GameState.TITLE_SCREEN;
    private HashMap<Entity, Integer> entityMap = new HashMap<>(); // entity to level index map
    private List<Level> levels;
    private HashMap<Integer, List<Entity>> levelIndexToEntityMap = new HashMap<>();

    // Title screen state
    public String[] titleScreenOptions = { "Start Game", "Options", "Exit" };
    public int titleScreenSelectedOption = 0;

    // Options screen state
    public String[] optionsScreenOptions = { "Volume", "Framerate" };
    public float[] optionsVolume = { 50.0f };
    public float[] optionsFrameRate = { 30.0f };
    public float[] optionsUpdateRate = { 30.0f };

    // ImGui state
    public ImGuiImplGlfw imguiGlfw;
    public ImGuiImplGl3 imguiGl3;
    public boolean messageBoxDisplayed = false;
    public boolean messageBoxOptionsDisplayed = false;
    public String currentFullMessage = "";
    public int selectedResponseIndex = -1;

    long movementLastTime = System.currentTimeMillis();

    public static void main(String[] args) throws Exception {
        new main().run();
    }

    public void run() throws Exception {
        init();
        loop();
        cleanup();
    }

    private void init() throws IOException {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        // glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        window = glfwCreateWindow(winW, winH, "Game Window", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create GLFW window");

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            winW = w;
            winH = h;
            glViewport(0, 0, w, h);
        });

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS)
                keyPressed(key);
            if (action == GLFW_RELEASE)
                keyReleased(key);
        });

        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            cursorXPosition = (int) xpos;
            cursorYPosition = (int) ypos;
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                leftMouseButtonPressed = true;
            }
        });

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidmode != null) {
                glfwSetWindowPos(
                        window,
                        (vidmode.width() - pWidth.get(0)) / 2,
                        (vidmode.height() - pHeight.get(0)) / 2);
                // glfwSetWindowMonitor(window, glfwGetPrimaryMonitor(), 0, 0, 1280, 720,
                // vidmode.refreshRate());
            }
        }
        FULLSCREEN = false;
        isFullscreen = new ImBoolean(false);
        shouldDebugGridBeDrawn = new ImBoolean(false);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        // glMatrixMode(GL_PROJECTION);
        // glLoadIdentity();
        // glOrtho(0, winW, winH, 0, -1, 1);
        // glMatrixMode(GL_MODELVIEW);

        // glEnable(GL_TEXTURE_2D);
        // glEnable(GL_BLEND);
        // glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        loadResources();

        backgroundTex = createTextureFromBufferedImage(backgroundBI);
        playerTex = createTextureFromBufferedImage(playerBI);
        npcTex = createTextureFromBufferedImage(npcBI);
        gridTex = createTextureFromBufferedImage(gridBI);
        doorTex = createTextureFromBufferedImage(doorBI);
        titleScreenTex = createTextureFromBufferedImage(titleScreenBI);
        titleScreenGameLogoTex = createTextureFromBufferedImage(titleScreenGameLogoBI);
        fishTex = createTextureFromBufferedImage(fishBI);

        // Initialize ImGui
        ImGui.createContext();
        imguiGlfw = new ImGuiImplGlfw();
        imguiGl3 = new ImGuiImplGl3();
        final ImGuiIO io = ImGui.getIO();
        imguiGlfw.init(window, true);
        imguiGl3.init();
        initFonts(io, DEFAULT_FONT, DEFAULT_FONT_SIZE);
        final ImGuiStyle style = ImGui.getStyle();
        style.setWindowRounding(5.3f);
        style.setFrameRounding(5f);
        style.setScrollbarRounding(0.0f);
        renderer = new GameRenderer(this, imguiGlfw, imguiGl3);

        fstoggle = new FullscreenToggle(window);

        // model = renderer.load3DModel(RESOURCE_PATH + "\\models\\model.obj");
        model = renderer.load3DObject(RESOURCE_PATH + "\\models\\fish.obj", fishTex, "model");

        // GL43.glEnable(GL43.GL_DEBUG_OUTPUT);
        // GL43.glEnable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
        // GL43.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE,
        // (IntBuffer)null, true);
        levels = parser.getLevels();
        for (Level level : levels) {
            for (Entity entity : level.getEntities()) {
                entityMap.put(entity, levels.indexOf(level));
            }
        }
    }

    public void GLDebugMessageCallback() {
    }

    public void changeWindowMode(boolean fullscreen) {
        if (fullscreen) {
            try (MemoryStack stack = stackPush()) {
                IntBuffer pWidth = stack.mallocInt(1);
                IntBuffer pHeight = stack.mallocInt(1);
                glfwGetWindowSize(window, pWidth, pHeight);
                GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
                if (vidmode != null) {
                    // glfwSetWindowPos(
                    // window,
                    // (vidmode.width() - pWidth.get(0)) / 2,
                    // (vidmode.height() - pHeight.get(0)) / 2);
                    glfwSetWindowMonitor(window, glfwGetPrimaryMonitor(), 0, 0, 1280, 720, vidmode.refreshRate());
                }
            }
            FULLSCREEN = false;
        } else {
            try (MemoryStack stack = stackPush()) {
                IntBuffer pWidth = stack.mallocInt(1);
                IntBuffer pHeight = stack.mallocInt(1);
                glfwGetWindowSize(window, pWidth, pHeight);
                GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
                if (vidmode != null) {
                    glfwSetWindowPos(
                            window,
                            (vidmode.width() - pWidth.get(0)) / 2,
                            (vidmode.height() - pHeight.get(0)) / 2);
                    // glfwSetWindowMonitor(window, glfwGetPrimaryMonitor(), 0, 0, 1280, 720,
                    // vidmode.refreshRate());
                }
            }
            FULLSCREEN = true;
        }
    }

    private void initFonts(final ImGuiIO io, String defaultFont, int defaultFontSize) {
        // This enables FreeType font renderer, which is disabled by default.
        io.getFonts().setFreeTypeRenderer(true);

        // Add default font for latin glyphs
        // io.getFonts().addFontDefault();

        // You can use the ImFontGlyphRangesBuilder helper to create glyph ranges based
        // on text input.
        // For example: for a game where your script is known, if you can feed your
        // entire script to it (using addText) and only build the characters the game
        // needs.
        // Here we are using it just to combine all required glyphs in one place
        final ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder(); // Glyphs ranges provide
        rangesBuilder.addRanges(io.getFonts().getGlyphRangesDefault());

        // Font config for additional fonts
        // This is a natively allocated struct so don't forget to call destroy after
        // atlas is built
        final ImFontConfig fontConfig = new ImFontConfig();
        // fontConfig.setMergeMode(true); // Enable merge mode to merge cyrillic,
        // japanese and icons with default font
        final short[] glyphRanges = rangesBuilder.buildRanges();
        System.out.println(RESOURCE_PATH + "\\fonts\\" + defaultFont + ".ttf");
        io.getFonts().addFontFromFileTTF(RESOURCE_PATH + "\\fonts\\" + defaultFont + ".ttf", defaultFontSize,
                fontConfig, glyphRanges);
        // io.getFonts().addFontFromFileTTF(RESOURCE_PATH + "fonts\\roboto.ttf", 14,
        // fontConfig, glyphRanges);
        io.getFonts().build();

        fontConfig.destroy();
    }

    private void loadResources() throws IOException {
        Path resourcesPath = Paths.get(RESOURCE_PATH);
        try {
            playerBI = javax.imageio.ImageIO.read(resourcesPath.resolve("textures/player.png").toFile());
            backgroundBI = javax.imageio.ImageIO.read(resourcesPath.resolve("textures/background1.png").toFile());
            npcBI = javax.imageio.ImageIO.read(resourcesPath.resolve("textures/npc.png").toFile());
            gridBI = javax.imageio.ImageIO.read(resourcesPath.resolve("textures/grid_overlay.png").toFile());
            doorBI = javax.imageio.ImageIO.read(resourcesPath.resolve("textures/door.png").toFile());
            titleScreenGameLogoBI = javax.imageio.ImageIO.read(resourcesPath.resolve("textures/logo.png").toFile());
            fishBI = javax.imageio.ImageIO.read(resourcesPath.resolve("textures/fish_texture.png").toFile());

            try {
                titleScreenBI = javax.imageio.ImageIO
                        .read(resourcesPath.resolve("textures/title_screen2.png").toFile());
            } catch (IOException e) {
                System.out.println("No title screen image found, using background with overlay.");
                titleScreenBI = backgroundBI;
            }
        } catch (IOException e) {
            System.err.println("Failed to load game resources: " + e.getMessage());
            throw e;
        }
    }

    public void setCollisionGrid(int[][] grid) {
        collisionGrid = grid;
    }

    public GameState getCurrentGameState() {
        return currentGameState;
    }

    public void setCurrentGameState(GameState state) {
        this.currentGameState = state;
    }

    public int createTextureFromBufferedImage(BufferedImage img) {
        int[] pixels = new int[img.getWidth() * img.getHeight()];
        img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
        ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int pixel = pixels[y * img.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
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
        if (!loadGameFromFile(this, "quicksave")) {
            currentGameState = GameState.IN_GAME;
            buildLevel(0);
        }
        ;
    }

    private void buildLevel(int levelIndex) {
        currentLevelIndex = levelIndex;
        entities.clear();

        Level level = levels.get(levelIndex);

        collisionGrid = level.getCollisionGrid();
        try {
            backgroundBI = javax.imageio.ImageIO
                    .read(new File(RESOURCE_PATH + "\\textures\\" + level.getBackgroundImage()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        backgroundTex = createTextureFromBufferedImage(backgroundBI);

        Entity player = null;
        for (Entity e : level.getEntities()) {
            if (e.getType().equals("player")) {
                player = e;
                break;
            }
        }
        if (player == null) {
            throw new IllegalStateException("Level " + levelIndex + " has no player entity defined.");
        }
        try {
            playerBI = javax.imageio.ImageIO.read(new File(RESOURCE_PATH + "\\textures\\" + player.getImagePath()));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        playerTex = createTextureFromBufferedImage(playerBI);
        player.setTextureId(playerTex);
        player.setWidth(ENTITY_WIDTH_CELLS * GRID_CELL_SIZE);
        player.setHeight(ENTITY_HEIGHT_CELLS * GRID_CELL_SIZE);
        //playerX = player.getX() * GRID_CELL_SIZE;
        //playerY = player.getY() * GRID_CELL_SIZE;
        entities.add(player);
        parser.parse();
        //player.setPosition(level.getEntities().get(0).getX(), level.getEntities().get(0).getY());
        player.setPosition(parser.getLevel(levelIndex).getPlayerEntityFromLevel().getX(), parser.getLevel(levelIndex).getPlayerEntityFromLevel().getY());
        player.setEntityAnimation(new entityAnimation(player, RESOURCE_PATH, this.renderer));
        playerX = player.getX();
        playerY = player.getY();
        entityMovement = new int[level.getEntities().size()][4];
        System.out.println(player.getX() + " " + player.getY());
        System.out.println(level.getEntities().get(0).getX() + " " + level.getEntities().get(0).getY());
        for (Entity e : level.getEntities()) {
            if (!e.getType().equals("player")) {
                e.setEntityAi(new EntityAI(e, this));
                e.setEntityAnimation(new entityAnimation(e, RESOURCE_PATH, this.renderer));
                System.out.println("Adding entity of type " + e.getType() + " at (" + e.getX() + ", " + e.getY() + ")");
                try {
                    e.setTextureId(createTextureFromBufferedImage(
                            javax.imageio.ImageIO.read(new File(RESOURCE_PATH + "\\textures\\" + e.getImagePath()))));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.setWidth(ENTITY_WIDTH_CELLS * GRID_CELL_SIZE);
                e.setHeight(ENTITY_HEIGHT_CELLS * GRID_CELL_SIZE);
                e.setPosition(e.getX(), e.getY());
                entities.add(e);
                for (int i = 0; i <= ENTITY_WIDTH_CELLS - 1; i++) {
                    for (int j = 0; j <= ENTITY_HEIGHT_CELLS - 1; j++) {
                        collisionGrid[(e.getY() / GRID_CELL_SIZE) + j][(e.getX() / GRID_CELL_SIZE) + i] = 1;
                    }
                }
            }
        }

        createDoors(levels.get(levelIndex).getDoors());
        renderer.move3DObject(model, 10, 10, 0);
        renderer.scale3DObject(model, 10.0f, 10.0f, 10.0f);
    }

    private void createDoors(List<Door> doors) {
        for (Door d : doors) {
            Entity door = new Entity("door", doorTex, d.getX(), d.getY(), DOOR_WIDTH, DOOR_HEIGHT, d.getTargetLevel(),
                    d.getTargetX(), d.getTargetY());
            entities.add(door);
        }
    }

    private void loop() {
        long lastTime = System.nanoTime();
        long lastRenderTime = lastTime;

        // Game updates fast (e.g., 60 UPS)
        double nsPerUpdate = 1_000_000_000.0 / UPDATE_RATE; // framerate / 2 updates per second

        // Rendering slow (e.g., 10 FPS)
        double nsPerFrame = 1_000_000_000.0 / FRAMERATE;

        double deltaUpdate = 0;
        double deltaFrame = 0;

        while (!glfwWindowShouldClose(window)) {
            long now = System.nanoTime();

            // Accumulate time for updates
            deltaUpdate += (now - lastTime) / nsPerUpdate;

            // Accumulate time for rendering
            deltaFrame += (now - lastRenderTime) / nsPerFrame;

            lastTime = now;

            // Update game as fast as needed (60 times/sec)
            while (deltaUpdate >= 1) {
                updateGame(1.0f / (float) UPDATE_RATE); // Fixed timestep for updates
                deltaUpdate--;
            }

            // Render only when it's time (10 times/sec)
            if (deltaFrame >= 1) {
                render(); // Render the current game state
                deltaFrame = 0; // Reset, don't accumulate

                // Swap buffers only when we actually rendered
                glfwSwapBuffers(window);

                // Update render timestamp
                lastRenderTime = now;
            }

            glfwPollEvents(); // Still poll events every iteration for responsiveness
        }
    }

    private int positionInWindowToPositionInGridX(int x, int y) {
        return x / GRID_CELL_SIZE;
    }

    private int positionInWindowToPositionInGridY(int x, int y) {
        return y / GRID_CELL_SIZE;
    }

    private void updateGame(double deltaMs) {
        switch (currentGameState) {
            case TITLE_SCREEN:
                updateTitleScreen();
                break;
            case IN_GAME:
                updateInGame(deltaMs);
                break;
            case PAUSED:
                break;
            case OPTIONS:
                updateOptions();
                break;
        }
    }

    private void updateTitleScreen() {
        // Title screen updates handled in ImGui rendering
    }

    private void updateOptions() {
        // moved to renderOptions like updateTitleScreen
    }

    private void spinfish() {
        modelRotX = modelRotX + 0.5f;
        modelRotY = modelRotY + 0.5f;
        modelRotZ = modelRotZ + 0.5f;
        renderer.rotate3DObject(model, modelRotX, modelRotY, modelRotZ);

    }

    private void updateInGame(double deltaMs) {
        if (!messageBoxDisplayed) {
            long now = System.currentTimeMillis();
            if (now - movementLastTime >= MOVEMENT_DELAY_MS) {
                int newPlayerX = Math.max(0, Math.min(playerX + xVelocity, winW - playerBI.getWidth()));
                int newPlayerY = Math.max(0, Math.min(playerY + yVelocity, winH - playerBI.getHeight()));
                boolean collision = false;
                System.out.println(newPlayerX + " " + newPlayerY);
                for (int px = 0; px < 3; px++) {
                    for (int py = 0; py < 5; py++) {
                        int checkX = positionInWindowToPositionInGridX(newPlayerX + px * GRID_CELL_SIZE,
                                newPlayerY + py * GRID_CELL_SIZE);
                        int checkY = positionInWindowToPositionInGridY(newPlayerX + px * GRID_CELL_SIZE,
                                newPlayerY + py * GRID_CELL_SIZE);
                        if (checkX < 0 || checkX >= collisionGrid[0].length || checkY < 0
                                || checkY >= collisionGrid.length || collisionGrid[checkY][checkX] == 1) {
                            collision = true;
                            return;
                        }
                    }
                }
                playerX = Math.max(0, Math.min(playerX + xVelocity, winW - playerBI.getWidth()));
                playerY = Math.max(0, Math.min(playerY + yVelocity, winH - playerBI.getHeight()));
                movementLastTime = now;
            }
        }

        for (Entity entity : entities) {
            if (entity.getEntityAI() != null) {
                entity.getEntityAI().tick();
            }
        }
    }

    private void render() {
        renderer.render(winW, winH, backgroundBI, backgroundTex, playerBI,
                gridBI, gridTex, titleScreenBI, titleScreenTex,
                titleScreenGameLogoBI, titleScreenGameLogoTex);
        spinfish();
        if (currentGameState == GameState.IN_GAME) {
            if (yVelocity == 0 && xVelocity == 0) {
                entities.get(0).setCurrentAnimationState("idle");
            } else if (xVelocity == 24) {
                entities.get(0).setCurrentAnimationState("walkingRight");
            } else if (xVelocity == -24) {
                entities.get(0).setCurrentAnimationState("walkingLeft");
            } else if (yVelocity != 0) {
                entities.get(0).setCurrentAnimationState("idle");
            }

            entities.get(0).setPosition(playerX, playerY);

            for (int i = 1; i < entities.size(); i++) {
                if (entityMovement != null && entityMovement.length > i) {
                    if (entityMovement[i][0] > 0) {
                        entities.get(i).setCurrentAnimationState("walkingRight");
                        entityMovement[i][0]--;
                    } else if (entityMovement[i][2] > 0) {
                        entities.get(i).setCurrentAnimationState("walkingLeft");
                        entityMovement[i][2]--;
                    } else if (entityMovement[i][1] > 0) {
                        entities.get(i).setCurrentAnimationState("walkingUp");
                        entityMovement[i][1]--;
                    } else if (entityMovement[i][3] > 0) {
                        entities.get(i).setCurrentAnimationState("walkingDown");
                        entityMovement[i][3]--;
                    } else {
                        // entityIndexToAnimationObjects.get(i).tick("idle");
                        entities.get(i).setCurrentAnimationState("idle");
                    }
                }
            }
        }
    }
    /*
     * private void render() {
     * glClearColor(0f, 0f, 0f, 1f);
     * glClear(GL_COLOR_BUFFER_BIT);
     * 
     * glMatrixMode(GL_PROJECTION);
     * glLoadIdentity();
     * glOrtho(0, winW, winH, 0, -1, 1);
     * glMatrixMode(GL_MODELVIEW);
     * glLoadIdentity();
     * 
     * switch (currentGameState) {
     * case TITLE_SCREEN:
     * renderTitleScreen();
     * break;
     * case IN_GAME:
     * renderInGame();
     * if (messageBoxDisplayed) {
     * renderMessageBox();
     * }
     * break;
     * case PAUSED:
     * // renderInGame();
     * renderPauseMenu();
     * break;
     * case OPTIONS:
     * renderOptions();
     * break;
     * }
     * }
     */

    private void keyPressed(int key) {
        System.out.println("Key pressed: " + key);

        if (key == GLFW_KEY_ESCAPE) {
            if (currentGameState == GameState.IN_GAME) {
                currentGameState = GameState.PAUSED;
            } else if (currentGameState == GameState.PAUSED) {
                currentGameState = GameState.IN_GAME;
            } else if (currentGameState == GameState.TITLE_SCREEN) {
                glfwSetWindowShouldClose(window, true);
            }
            return;
        }

        switch (currentGameState) {
            case IN_GAME:
                handleInGameKeyPress(key);
                break;
            case PAUSED:
                break;
        }
    } // (int)(Math.random() * 1000)

    float modelRotX, modelRotY, modelRotZ = 0f;

    private void handleInGameKeyPress(int key) {
        if (messageBoxDisplayed) {
            if (key == GLFW_KEY_E) {
                closeMessage();
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
            case GLFW_KEY_O:
                renderer.move3DObject(model, 500, 500, 0);
                break;
            case GLFW_KEY_P:
                renderer.scale3DObject(model, 50.0f, 50.0f, 50.0f);
                break;
            case GLFW_KEY_I:
                modelRotX = modelRotX + 10;
                modelRotY = modelRotY + 10;
                modelRotZ = modelRotZ + 10;
                renderer.rotate3DObject(model, modelRotX, modelRotY, modelRotZ);
                break;
            case GLFW_KEY_F9:
                loadGameFromFile(this, "player_save");
                break;
            case GLFW_KEY_0:
                System.out.println(Arrays.deepToString(collisionGrid));
                break;
            case GLFW_KEY_K:
                killNearestNPC();
                break;
            case GLFW_KEY_F12:
                buildLevel(currentLevelIndex);
                break;
            // case GLFW_KEY_P:
            // renderer.move3DModel(model, 1, 0, 5);
            // break;
        }

        if (pressedKey[0] != 0 && pressedKey[1] != 0) {
            boolean firstKeyIsVertical = (pressedKey[0] == GLFW_KEY_UP || pressedKey[0] == GLFW_KEY_W
                    || pressedKey[0] == GLFW_KEY_DOWN || pressedKey[0] == GLFW_KEY_S);
            boolean secondKeyIsVertical = (pressedKey[1] == GLFW_KEY_UP || pressedKey[1] == GLFW_KEY_W
                    || pressedKey[1] == GLFW_KEY_DOWN || pressedKey[1] == GLFW_KEY_S);
            if (firstKeyIsVertical && !secondKeyIsVertical) {
                yVelocity = 0;
            } else if (!firstKeyIsVertical && secondKeyIsVertical) {
                xVelocity = 0;
            }
        }
    }

    private void keyReleased(int key) {
        if (currentGameState != GameState.IN_GAME || messageBoxDisplayed)
            return;

        switch (key) {
            case GLFW_KEY_UP:
            case GLFW_KEY_W:
                if (yVelocity < 0)
                    yVelocity = 0;
                break;
            case GLFW_KEY_DOWN:
            case GLFW_KEY_S:
                if (yVelocity > 0)
                    yVelocity = 0;
                break;
            case GLFW_KEY_LEFT:
            case GLFW_KEY_A:
                if (xVelocity < 0)
                    xVelocity = 0;
                break;
            case GLFW_KEY_RIGHT:
            case GLFW_KEY_D:
                if (xVelocity > 0)
                    xVelocity = 0;
                break;
        }
    }

    public void displayMessage(String message) {
        if (messageBoxDisplayed)
            return;
        System.out.println("Displaying message: " + message);
        yVelocity = 0;
        xVelocity = 0;
        messageBoxDisplayed = true;
        currentFullMessage = message;
        messageBoxOptionsDisplayed = false;
        playTalkingSound();
    }

    public void displayMessageWithResponses(String message, String[] responses) {
        if (messageBoxDisplayed)
            return;
        closeMessage();
        System.out.println("Displaying message with responses: " + message);
        yVelocity = 0;
        xVelocity = 0;
        messageBoxOptionsDisplayed = true;
        messageBoxDisplayed = true;
        currentFullMessage = message;
        currentResponseOptions = responses;
        playTalkingSound();
    }

    public void closeMessage() {
        if (!messageBoxDisplayed)
            return;
        messageBoxDisplayed = false;
        currentFullMessage = "";
        messageBoxOptionsDisplayed = false;
        currentResponseOptions = null;
    }

    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    public int[][] getCollisionGrid() {
        return collisionGrid;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void setEntityMovement(int entityIndex, int directionIndex, int value) {
        entityMovement[entityIndex][directionIndex] = value;
    }

    public void saveCurrentGame(main mainInstance, String name) {
        SaveGame saveGame = new SaveGame(name);
        if (saveGame.saveGame(mainInstance)) {
            System.out.println("Game saved successfully!");
        } else {
            System.err.println("Failed to save game!");
        }
    }

    public boolean loadGameFromFile(main mainInstance, String saveFileName) {
        SaveGame loadGame = new SaveGame();
        if (loadGame.loadGame(saveFileName)) {
            // First, build the level to populate entities
            mainInstance.buildLevel(loadGame.getGameStateData().currentLevelIndex);

            // Then apply the loaded state to restore positions
            loadGame.applyLoadedState(mainInstance);

            System.out.println("Game loaded successfully!");
            mainInstance.currentGameState = main.GameState.IN_GAME;
            return true;
        } else {
            System.err.println("Failed to load game!");
            return false;
        }
    }

    public void handleTitleScreenOption(int optionIndex) {
        switch (optionIndex) {
            case 0:
                startGame();
                break;
            case 1:
                currentGameState = GameState.OPTIONS;
                break;
            case 2:
                glfwSetWindowShouldClose(window, true);
                break;
        }
    }

    public void setCurrentDialogueTree(dialogueTree tree) {
        currentTree = tree;
    }

    public void setCurrentNPCPlayerIsInteractingWith(Entity npc) {
        currentNPC = npc;
    }

    private void removeEntity(Entity e) {
        int[][] collisionGrid = getCollisionGrid();
        for (int a = 0; a < 3; a++) {
            for (int j = 0; j < 5; j++) {
                collisionGrid[(e.getY() / 24) + j][(e.getX() / 24) + a] = 0;
            }
        }
        setCollisionGrid(collisionGrid);
        entities.remove(e);
        List<Entity> levelEntities = levels.get(currentLevelIndex).getEntities();
        levelEntities.remove(e);
        levels.get(currentLevelIndex).setEntities(levelEntities);
    }

    private void killNearestNPC(){
        if (entities.size() < 2)
            return;
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
        if(nearest != null){
            if(!nearest.getType().equals("door")){
                removeEntity(nearest);
            }
        }
    }

    private void interactWithNearestNPC() {
        if (entities.size() < 2)
            return;
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

        if (nearest != null && nearest.getType().equals("door")) {
            levels.get(nearest.getTargetLevel()).getPlayerEntityFromLevel().setPosition(nearest.getTargetX(),
                    nearest.getTargetY());
            buildLevel(nearest.getTargetLevel());
            return;
        }

        if (nearest != null && !nearest.getDialogueTree().isTreeEmpty()) {
            currentTree = nearest.getDialogueTree();
            currentNPC = nearest;
            dialogueHandler(nearest.getDialogueTree(), 0, nearest);
            return;
        }

        if (nearest != null) {
            displayMessage("I have nothing to say to you.");
        }
    }

    public void dialogueHandler(dialogueTree tree, int selectedResponse, Entity npc) {
        closeMessage();
        String npcText = tree.getNpcText();
        List<Response> playerResponses = tree.getResponses();
        String[] responses = new String[playerResponses.size()];
        for (int i = 0; i < playerResponses.size(); i++) {
            responses[i] = playerResponses.get(i).getResponseText();
        }

        if (selectedResponse == 0) {
            displayMessageWithResponses(npcText, responses);
            return;
        }

        for (int i = 0; i < playerResponses.size(); i++) {
            if (i == selectedResponse - 1 && responses.length > 0) {
                Response r = playerResponses.get(i);
                if (currentTree.getResponses().isEmpty()) {
                    System.out.println("End of conversation reached.");
                    displayMessage(currentTree.getNpcText());
                    messageBoxOptionsDisplayed = false;
                    currentResponseOptions = null;
                } else if (r.getNextNode() != null && r.getNextNode().getResponses().size() > 0) {
                    dialogueHandler(r.getNextNode(), 0, npc);
                    currentTree = r.getNextNode();
                } else if (r.getNextNode() != null && r.getNextNode().getResponses().size() == 0) {
                    closeMessage();
                    messageBoxOptionsDisplayed = false;
                    currentResponseOptions = null;
                    currentTree = r.getNextNode();
                    displayMessage(r.getNextNode().getNpcText());
                    if (r.getNextNode().getNpcAction() != null) {
                        handleNPCActions(r.getNextNode().getNpcAction(), npc);
                    }
                }
                return;
            }
        }
    }

    private void handleNPCActions(String action, Entity npc) {
        if (action.startsWith("move_to")) {
            int start = action.indexOf('(');
            int end = action.indexOf(')');
            if (start == -1 || end == -1 || end <= start + 1)
                return;
            String[] parts = action.substring(start + 1, end).split(",");
            if (parts.length != 2)
                return;
            try {
                int targetX = Integer.parseInt(parts[0].trim()) * GRID_CELL_SIZE;
                int targetY = Integer.parseInt(parts[1].trim()) * GRID_CELL_SIZE;
                System.out.println("Start position: " + positionInWindowToPositionInGridX(npc.getX(), npc.getY())
                        + ", " + positionInWindowToPositionInGridY(npc.getX(), npc.getY()) + " Target position: "
                        + targetX / GRID_CELL_SIZE + ", " + targetY / GRID_CELL_SIZE);
                Thread pathfindingThread = new Thread(() -> {
                    npc.getEntityAI().NPCPathfindToPoint(
                            positionInWindowToPositionInGridX(npc.getX(), npc.getY()),
                            positionInWindowToPositionInGridY(npc.getX(), npc.getY()), targetX / GRID_CELL_SIZE,
                            targetY / GRID_CELL_SIZE, npc);
                });
                pathfindingThread.start();
                System.out.println("Moved NPC to (" + targetX + ", " + targetY + ")");
            } catch (NumberFormatException e) {
                System.err.println("Invalid move_to action format: " + action);
            }
        } else if (action.startsWith("change_player_level")) {
            int start = action.indexOf('(');
            int end = action.indexOf(')');
            if (start == -1 || end == -1 || end <= start + 1)
                return;
            int level = Integer.parseInt(action.substring(start + 1, end));
            buildLevel(level);
            closeMessage();
        } else if (action.startsWith("exit_game")) {
            cleanup();
            System.exit(0);
        }
    }

    public void playTalkingSound() {
        try {
            WavPlayer soundPlayer = new WavPlayer(RESOURCE_PATH + "\\audio\\talking.wav", soundPlayerVolume);
            soundPlayer.setName("soundPlayerThread");
            soundPlayer.setPriority(Thread.MAX_PRIORITY);
            soundPlayer.start();
        } catch (Exception e) {
            System.err.println("Could not play talking sound: " + e.getMessage());
        }
    }

    private void cleanup() {
        glDeleteTextures(playerTex);
        glDeleteTextures(npcTex);
        glDeleteTextures(backgroundTex);
        glDeleteTextures(gridTex);
        glDeleteTextures(doorTex);
        glDeleteTextures(titleScreenTex);
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}