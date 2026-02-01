package fbla.editor;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.flag.ImGuiWindowFlags;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.type.ImString;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.ImGui.*;
import imgui.flag.*;
import imgui.flag.ImGuiWindowFlags.*;
import imgui.ImGuiIO.*;
import imgui.ImGuiIO;
import imgui.ImFontConfig;
import imgui.ImFontGlyphRangesBuilder;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.ImGuiStyle;
import imgui.ImVec4;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.BufferUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fbla.editor.jsonParser;
import fbla.editor.Level;
import fbla.editor.Entity;
import fbla.editor.Door;

// todo priority levels:
// 1 - high, needs to be done first
// 2 - medium, do after all ones are finished
// 3 - low, nice to haves, don't need to be added now

public class LevelEditor {
    private static final int WINDOW_W = 1700;
    private static final int WINDOW_H = 900;
    private static final String RESOURCE_PATH = System.getProperty("user.home")
            + "\\Desktop\\FBLA-Game\\game_resources";

    // Grid settings
    private static final int GRID_CELL_SIZE = 24;
    private static final int GRID_WIDTH = 53;
    private static final int GRID_HEIGHT = 30;

    private long window;
    private int winW = WINDOW_W, winH = WINDOW_H;

    // ImGui
    private ImGuiImplGlfw imguiGlfw;
    private ImGuiImplGl3 imguiGl3;

    // Editor state
    private jsonParser parser;
    private List<Level> levels;
    private int currentLevelIndex = 0;
    private int[][] collisionGrid;
    private List<Entity> entities;
    private List<Door> doors;

    // UI state
    private ImString selectedEntityType = new ImString("npc", 256);
    private ImString entityName = new ImString("placeholder", 256);
    private ImString entityImage = new ImString("npc.png");
    private ImFloat entityX = new ImFloat(0);
    private ImFloat entityY = new ImFloat(0);
    private int selectedEntityIndex = -1;
    private boolean showCollisionGrid = true;
    private boolean showEntities = true;
    private boolean showDoors = true;

    // Door creation state
    private ImString doorTargetLevel = new ImString("0", 256);
    private ImString doorTargetX = new ImString("0", 256);
    private ImString doorTargetY = new ImString("0", 256);
    private ImString doorImage = new ImString("door.png");

    // Grid editing state
    private ImBoolean paintMode = new ImBoolean(false);
    private ImBoolean eraserMode = new ImBoolean(false);

    // Dialogue/Animation editing state (Level 1 TODOs)
    private boolean showDialogueEditor = false;
    private boolean showAnimationEditor = false;
    private int editingEntityIndex = -1;
    private dialogueTree editingDialogueNode = null;
    private List<dialogueTree> dialogueNodeStack = new ArrayList<>();
    private ImString dialogueNpcText = new ImString("", 512);
    private ImString dialogueNpcAction = new ImString("", 256);
    private ImString dialogueResponseText = new ImString("", 256);
    private int editingResponseIndex = -1;

    // Level 2 TODO state variables
    private ImString backgroundImagePath = new ImString("", 256);
    private int backgroundImageTextureId = -1;
    private boolean isDraggingEntity = false;
    private boolean isDraggingDoor = false;
    private int draggingEntityIndex = -1;
    private int draggingDoorIndex = -1;
    private int dragPreviewGridX = -1;
    private int dragPreviewGridY = -1;

    // Collision grid update constants
    private static final int ENTITY_WIDTH = 3;  // cells
    private static final int ENTITY_HEIGHT = 5; // cells
    private static final int DOOR_WIDTH = 2;    // cells
    private static final int DOOR_HEIGHT = 3;   // cells

    public static void main(String[] args) throws Exception {
        new LevelEditor().run();
    }

    public void run() throws Exception {
        init();
        loop();
        cleanup();
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

    private void init() throws IOException {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(winW, winH, "FBLA Game - Level Editor", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create GLFW window");

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            winW = w;
            winH = h;
            glViewport(0, 0, w, h);
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                double[] xpos = new double[1];
                double[] ypos = new double[1];
                glfwGetCursorPos(win, xpos, ypos);

                int mouseX = (int) xpos[0];
                int mouseY = (int) ypos[0];

                if (action == GLFW_PRESS) {
                    handleMousePress(mouseX, mouseY);
                } else if (action == GLFW_RELEASE) {
                    handleMouseRelease(mouseX, mouseY);
                }
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
            }
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        // Initialize ImGui
        ImGui.createContext();
        imguiGlfw = new ImGuiImplGlfw();
        imguiGl3 = new ImGuiImplGl3();
        final ImGuiIO io = ImGui.getIO();
        imguiGlfw.init(window, true);
        imguiGl3.init();
        initFonts(io, "roboto", 16);

        // Load levels
        parser = new jsonParser(new File(RESOURCE_PATH + "\\levels.json"));
        levels = new ArrayList<>(parser.getLevels());
        if (!levels.isEmpty()) {
            loadLevel(0);
        }
    }

    private void loadLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= levels.size())
            return;

        currentLevelIndex = levelIndex;
        Level level = levels.get(levelIndex);
        collisionGrid = level.getCollisionGrid();
        entities = new ArrayList<>(level.getEntities());
        doors = new ArrayList<>(level.getDoors());

        // Rebuild collision grid from entities and doors
        rebuildCollisionGrid();

        // Check for conflicts
        checkForCollisionConflicts();

        // Load background image if it exists
        String bgImage = level.getBackgroundImage();
        if (bgImage != null && !bgImage.isEmpty()) {
            backgroundImagePath.set(bgImage);
            loadBackgroundImage(bgImage);
        } else {
            backgroundImagePath.set("");
            backgroundImageTextureId = -1;
        }

        // Reset drag-drop state
        isDraggingEntity = false;
        isDraggingDoor = false;
        draggingEntityIndex = -1;
        draggingDoorIndex = -1;
    }

    /**
     * Rebuild the collision grid from scratch based on current entities and doors
     */
    private void rebuildCollisionGrid() {
        // Clear the collision grid first (keep painted collision cells)
        // We need to preserve manually painted cells, so we iterate through entities/doors
        // and mark only their cells
        
        // First, clear all cells that entities/doors occupy
        for (Entity e : entities) {
            if (!e.getType().equals("player")) {
                clearEntityFromCollisionGrid(e);
            }
        }
        for (Door d : doors) {
            clearDoorFromCollisionGrid(d);
        }
        
        // Now add back entities and doors
        for (Entity e : entities) {
            if (!e.getType().equals("player")) {
                updateEntityInCollisionGrid(e);
            }
        }
        for (Door d : doors) {
            updateDoorInCollisionGrid(d);
        }
    }

    private void handleMousePress(int mouseX, int mouseY) {
        int canvasX = 320;
        int canvasY = 20;
        int canvasW = GRID_WIDTH * GRID_CELL_SIZE;
        int canvasH = GRID_HEIGHT * GRID_CELL_SIZE;

        // Check if click is within canvas
        if (mouseX >= canvasX && mouseX <= canvasX + canvasW &&
            mouseY >= canvasY && mouseY <= canvasY + canvasH) {

            // Check if clicking on an entity
            for (int i = 0; i < entities.size(); i++) {
                Entity e = entities.get(i);
                int entityScreenX = canvasX + e.getX();
                int entityScreenY = canvasY + e.getY();

                if (mouseX >= entityScreenX - 12 && mouseX <= entityScreenX + 12 &&
                    mouseY >= entityScreenY - 12 && mouseY <= entityScreenY + 12) {
                    isDraggingEntity = true;
                    draggingEntityIndex = i;
                    return;
                }
            }

            // Check if clicking on a door
            for (int i = 0; i < doors.size(); i++) {
                Door d = doors.get(i);
                int doorScreenX = canvasX + d.getX();
                int doorScreenY = canvasY + d.getY();

                if (mouseX >= doorScreenX && mouseX <= doorScreenX + 48 &&
                    mouseY >= doorScreenY && mouseY <= doorScreenY + 72) {
                    isDraggingDoor = true;
                    draggingDoorIndex = i;
                    return;
                }
            }

            // Otherwise, handle grid paint/erase
            int gridX = (mouseX - canvasX) / GRID_CELL_SIZE;
            int gridY = (mouseY - canvasY) / GRID_CELL_SIZE;

            if (gridX >= 0 && gridX < GRID_WIDTH && gridY >= 0 && gridY < GRID_HEIGHT) {
                if (paintMode.get()) {
                    collisionGrid[gridY][gridX] = 1;
                } else if (eraserMode.get()) {
                    collisionGrid[gridY][gridX] = 0;
                }
            }
        }
    }

    private void handleMouseRelease(int mouseX, int mouseY) {
        int canvasX = 320;
        int canvasY = 20;
        int canvasW = GRID_WIDTH * GRID_CELL_SIZE;
        int canvasH = GRID_HEIGHT * GRID_CELL_SIZE;

        if (isDraggingEntity) {
            if (mouseX >= canvasX && mouseX <= canvasX + canvasW &&
                mouseY >= canvasY && mouseY <= canvasY + canvasH) {
                
                int gridX = (mouseX - canvasX) / GRID_CELL_SIZE;
                int gridY = (mouseY - canvasY) / GRID_CELL_SIZE;
                
                if (gridX >= 0 && gridX < GRID_WIDTH && gridY >= 0 && gridY < GRID_HEIGHT) {
                    Entity e = entities.get(draggingEntityIndex);
                    
                    // Clear old position from collision grid
                    // don't if e is player, player collision is calculated in game
                    if(!e.getType().equals("player")){
                        clearEntityFromCollisionGrid(e);
                    }
                    
                    // Try to place at new position
                    int newGridX = gridX;
                    int newGridY = gridY;
                    
                    if (!canPlaceEntity(newGridX, newGridY, draggingEntityIndex)) {
                        // Find nearest valid position
                        int[] nearest = findNearestValidPosition(newGridX, newGridY, ENTITY_WIDTH, ENTITY_HEIGHT, draggingEntityIndex);
                        if (nearest != null) {
                            newGridX = nearest[0];
                            newGridY = nearest[1];
                        } else {
                            // No valid position, revert to old position
                            System.err.println("No valid position found for entity");
                            if(!e.getType().equals("player")){
                                updateEntityInCollisionGrid(e);
                            }
                            isDraggingEntity = false;
                            draggingEntityIndex = -1;
                            dragPreviewGridX = -1;
                            dragPreviewGridY = -1;
                            return;
                        }
                    }
                    
                    // Update entity position
                    e.setPosition(newGridX * GRID_CELL_SIZE, newGridY * GRID_CELL_SIZE);
                    
                    // Update collision grid
                    if(!e.getType().equals("player")){
                        updateEntityInCollisionGrid(e);
                    }
                }
            }
            isDraggingEntity = false;
            draggingEntityIndex = -1;
            dragPreviewGridX = -1;
            dragPreviewGridY = -1;
        }

        if (isDraggingDoor) {
            if (mouseX >= canvasX && mouseX <= canvasX + canvasW &&
                mouseY >= canvasY && mouseY <= canvasY + canvasH) {
                
                int gridX = (mouseX - canvasX) / GRID_CELL_SIZE;
                int gridY = (mouseY - canvasY) / GRID_CELL_SIZE;

                if (gridX >= 0 && gridX < GRID_WIDTH && gridY >= 0 && gridY < GRID_HEIGHT) {
                    Door d = doors.get(draggingDoorIndex);
                    
                    // Clear old position from collision grid
                    clearDoorFromCollisionGrid(d);
                    
                    // Try to place at new position
                    int newGridX = gridX;
                    int newGridY = gridY;
                    
                    if (!canPlaceDoor(newGridX, newGridY, draggingDoorIndex)) {
                        // Find nearest valid position
                        int[] nearest = findNearestValidPosition(newGridX, newGridY, DOOR_WIDTH, DOOR_HEIGHT, -1);
                        if (nearest != null) {
                            newGridX = nearest[0];
                            newGridY = nearest[1];
                        } else {
                            // No valid position, revert to old position
                            System.err.println("No valid position found for door");
                            updateDoorInCollisionGrid(d);
                            isDraggingDoor = false;
                            draggingDoorIndex = -1;
                            dragPreviewGridX = -1;
                            dragPreviewGridY = -1;
                            return;
                        }
                    }
                    
                    // Update door position
                    setPrivateField(d, "x", newGridX * GRID_CELL_SIZE);
                    setPrivateField(d, "y", newGridY * GRID_CELL_SIZE);
                    
                    // Update collision grid
                    updateDoorInCollisionGrid(d);
                }
            }
            isDraggingDoor = false;
            draggingDoorIndex = -1;
            dragPreviewGridX = -1;
            dragPreviewGridY = -1;
        }
    }

    private void updateDragPreview() {
        if (!isDraggingEntity && !isDraggingDoor) {
            dragPreviewGridX = -1;
            dragPreviewGridY = -1;
            return;
        }

        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(window, xpos, ypos);

        int canvasX = 320;
        int canvasY = 20;
        int mouseX = (int) xpos[0];
        int mouseY = (int) ypos[0];

        int gridX = (mouseX - canvasX) / GRID_CELL_SIZE;
        int gridY = (mouseY - canvasY) / GRID_CELL_SIZE;

        if (gridX >= 0 && gridX < GRID_WIDTH && gridY >= 0 && gridY < GRID_HEIGHT) {
            dragPreviewGridX = gridX;
            dragPreviewGridY = gridY;
        }
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClearColor(0.15f, 0.15f, 0.15f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Setup projection
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glOrtho(0, winW, winH, 0, -1, 1);
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();

            // Update drag preview position
            updateDragPreview();

            // Render canvas (OpenGL)
            renderCanvas();

            // Render UI (ImGui on top)
            imguiGlfw.newFrame();
            imguiGl3.newFrame();
            ImGui.newFrame();
            
            renderUI();
            
            ImGui.render();
            imguiGl3.renderDrawData(ImGui.getDrawData());

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void renderCanvas() {
        int canvasX = 320;
        int canvasY = 20;
        int canvasW = GRID_WIDTH * GRID_CELL_SIZE;
        int canvasH = GRID_HEIGHT * GRID_CELL_SIZE;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Draw background image if loaded
        if (backgroundImageTextureId != -1) {
            glBindTexture(GL_TEXTURE_2D, backgroundImageTextureId);
            glColor4f(1, 1, 1, 1);
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0); glVertex2f(canvasX, canvasY);
            glTexCoord2f(1, 0); glVertex2f(canvasX + canvasW, canvasY);
            glTexCoord2f(1, 1); glVertex2f(canvasX + canvasW, canvasY + canvasH);
            glTexCoord2f(0, 1); glVertex2f(canvasX, canvasY + canvasH);
            glEnd();
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        // Draw collision grid
        if (showCollisionGrid) {
            glColor4f(1, 0, 0, 0.3f);
            glBegin(GL_QUADS);
            for (int y = 0; y < GRID_HEIGHT; y++) {
                for (int x = 0; x < GRID_WIDTH; x++) {
                    if (collisionGrid[y][x] == 1) {
                        int px = canvasX + x * GRID_CELL_SIZE;
                        int py = canvasY + y * GRID_CELL_SIZE;
                        glVertex2f(px, py);
                        glVertex2f(px + GRID_CELL_SIZE, py);
                        glVertex2f(px + GRID_CELL_SIZE, py + GRID_CELL_SIZE);
                        glVertex2f(px, py + GRID_CELL_SIZE);
                    }
                }
            }
            glEnd();
        }

        // Draw grid lines
        glColor4f(0.3f, 0.3f, 0.3f, 0.5f);
        glBegin(GL_LINES);
        for (int x = 0; x <= GRID_WIDTH; x++) {
            int px = canvasX + x * GRID_CELL_SIZE;
            glVertex2f(px, canvasY);
            glVertex2f(px, canvasY + canvasH);
        }
        for (int y = 0; y <= GRID_HEIGHT; y++) {
            int py = canvasY + y * GRID_CELL_SIZE;
            glVertex2f(canvasX, py);
            glVertex2f(canvasX + canvasW, py);
        }
        glEnd();

        // Draw doors
        if (showDoors) {
            for (int i = 0; i < doors.size(); i++) {
                Door d = doors.get(i);
                int px = canvasX + d.getX();
                int py = canvasY + d.getY();
                drawDoor(px, py);
            }
        }

        // Draw entities
        if (showEntities) {
            for (int i = 0; i < entities.size(); i++) {
                Entity e = entities.get(i);
                if (!e.getType().equals("door")) {
                    int px = canvasX + e.getX();
                    int py = canvasY + e.getY();
                    drawEntity(px, py, e);
                }
            }
        }

        // Draw drag preview
        if (isDraggingEntity && dragPreviewGridX >= 0 && dragPreviewGridY >= 0) {
            int px = canvasX + dragPreviewGridX * GRID_CELL_SIZE;
            int py = canvasY + dragPreviewGridY * GRID_CELL_SIZE;
            glColor4f(0, 1, 0, 0.5f);
            glBegin(GL_TRIANGLE_FAN);
            int radius = 12;
            for (int i = 0; i < 16; i++) {
                double angle = (i / 16.0) * Math.PI * 2;
                glVertex2f(px + (float) Math.cos(angle) * radius, 
                          py + (float) Math.sin(angle) * radius);
            }
            glEnd();
        } else if (isDraggingDoor && dragPreviewGridX >= 0 && dragPreviewGridY >= 0) {
            int px = canvasX + dragPreviewGridX * GRID_CELL_SIZE;
            int py = canvasY + dragPreviewGridY * GRID_CELL_SIZE;
            glColor4f(1, 1, 0, 0.5f);
            glBegin(GL_QUADS);
            glVertex2f(px, py);
            glVertex2f(px + 48, py);
            glVertex2f(px + 48, py + 72);
            glVertex2f(px, py + 72);
            glEnd();
        }
    }

    private void drawEntity(int x, int y, Entity e) {
        // TODO: draw entity sprite instead of circle, path to sprite is found in Entity.java, level 3
        // Draw a circle for entities
        glColor4f(0, 1, 0, 0.8f);
        glBegin(GL_TRIANGLE_FAN);
        int radius = 12;
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0) * Math.PI * 2;
            glVertex2f(x + (float) Math.cos(angle) * radius, 
                      y + (float) Math.sin(angle) * radius);
        }
        glEnd();

        // Draw type label
        glColor4f(1, 1, 1, 1);
    }

    private void drawDoor(int x, int y) {
        // TODO: draw door sprite instead of rectangle, path to sprite is found in Door.java, level 3
        // Draw a rectangle for doors (48x72 = 2x3 cells)
        glColor4f(1, 1, 0, 0.8f);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + 48, y);
        glVertex2f(x + 48, y + 72);
        glVertex2f(x, y + 72);
        glEnd();
    }

    private void renderUI() {
        // Main control panel
        ImGui.setNextWindowPos(10, 20, 0);
        ImGui.setNextWindowSize(300, 860, 0);
        
        ImGui.begin("Level Editor");

        // Level info
        ImGui.text("Level " + currentLevelIndex + " of " + (levels.size() - 1));
        ImGui.separator();

        // Navigation
        if (ImGui.button("< Prev", 90, 30)) {
            if (currentLevelIndex > 0) loadLevel(currentLevelIndex - 1);
        }
        ImGui.sameLine();
        if (ImGui.button("Next >", 90, 30)) {
            if (currentLevelIndex < levels.size() - 1) loadLevel(currentLevelIndex + 1);
        }
        ImGui.sameLine();
        if (ImGui.button("Save", 90, 30)) {
            saveAllLevels();
        }

        // Add new level button (Level 2 TODO)
        if (ImGui.button("New Level", 90, 30)) {
            addNewLevel();
        }
        ImGui.sameLine();
        if (ImGui.button("Delete Level", 90, 30)) {
            deleteCurrentLevel();
        }

        ImGui.separator();

        // Background image section (Level 2 TODO)
        ImGui.text("Background Image:");
        ImGui.inputText("Image Path##bg", backgroundImagePath);
        ImGui.sameLine();
        if (ImGui.button("Load##bg", 50, 20)) {
            String bgPath = backgroundImagePath.get();
            if (!bgPath.isEmpty()) {
                loadBackgroundImage(bgPath);
            }
        }
        if (ImGui.button("Clear##bg", 50, 20)) {
            backgroundImagePath.set("");
            backgroundImageTextureId = -1;
            setPrivateField(levels.get(currentLevelIndex), "backgroundImage", "");
        }

        ImGui.separator();

        // Grid tools
        ImGui.text("Grid Editing:");
        ImGui.checkbox("Paint", paintMode);
        ImGui.sameLine();
        ImGui.checkbox("Erase", eraserMode);
        
        if (paintMode.get() && eraserMode.get()) {
            eraserMode.set(false);
        }

        ImGui.separator();

        // Visibility
        ImGui.text("Show:");
        if (ImGui.checkbox("Collision", new ImBoolean(showCollisionGrid))) {
            showCollisionGrid = !showCollisionGrid;
        }
        if (ImGui.checkbox("Entities", new ImBoolean(showEntities))) {
            showEntities = !showEntities;
        }
        if (ImGui.checkbox("Doors", new ImBoolean(showDoors))) {
            showDoors = !showDoors;
        }

        ImGui.separator();

        // Add entity
        ImGui.text("Add Entity:");
        ImGui.inputText("Type##ent", selectedEntityType);
        ImGui.inputText("Name##ent", entityName);
        ImGui.inputFloat("X##ent", entityX);
        ImGui.inputFloat("Y##ent", entityY);
        ImGui.inputText("Entity Image##ent", entityImage);
        if (ImGui.button("Add Entity##btn", 150, 25)) {
            addEntity((int)entityX.get(), (int)entityY.get(), selectedEntityType.get(), entityName.get(), entityImage.get());
            entityX.set(0f);
            entityY.set(0f);
        }

        ImGui.separator();

        // Add door
        ImGui.text("Add Door:");
        ImGui.inputText("Target Lvl", doorTargetLevel);
        ImGui.inputText("Target X", doorTargetX);
        ImGui.inputText("Target Y", doorTargetY);
        ImGui.inputText("Door Image##ent", doorImage);
        if (ImGui.button("Add Door##btn", 150, 25)) {
            try {
                int targetLvl = Integer.parseInt(doorTargetLevel.get());
                int targetX = Integer.parseInt(doorTargetX.get());
                int targetY = Integer.parseInt(doorTargetY.get());
                addDoor((int) entityX.get(), (int) entityY.get(), targetLvl, targetX, targetY, doorImage.get());
            } catch (NumberFormatException e) {
                System.err.println("Invalid door values");
            }
        }

        ImGui.separator();

        // Entity list with edit buttons
        ImGui.text("Entities (" + entities.size() + "):");
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            String label = "[" + i + "] " + e.getType() + " @" + e.getX() + "," + e.getY();
            ImGui.text(label);
            ImGui.sameLine();
            if (ImGui.button("D##dial" + i, 25, 20)) {
                openDialogueEditor(i);
            }
            ImGui.sameLine();
            if (ImGui.button("A##anim" + i, 25, 20)) {
                openAnimationEditor(i);
            }
            ImGui.sameLine();
            if (ImGui.button("X##ent" + i, 25, 20)) {
                clearEntityFromCollisionGrid(entities.get(i));
                entities.remove(i);
                break;
            }
        }

        ImGui.separator();

        // Door list
        ImGui.text("Doors (" + doors.size() + "):");
        for (int i = 0; i < doors.size(); i++) {
            Door d = doors.get(i);
            String label = "[" + i + "] L" + d.getTargetLevel() + " @" + d.getX() + "," + d.getY();
            ImGui.text(label);
            ImGui.sameLine();
            if (ImGui.button("X##door" + i, 25, 20)) {
                doors.remove(i);
                break;
            }
        }

        ImGui.end();

        // Render dialogue editor window if open
        if (showDialogueEditor) {
            renderDialogueEditor();
        }

        // Render animation editor window if open
        if (showAnimationEditor) {
            renderAnimationEditor();
        }
    }

    // ===== DIALOGUE EDITOR (Level 1 TODO) =====
    private void openDialogueEditor(int entityIndex) {
        editingEntityIndex = entityIndex;
        Entity entity = entities.get(entityIndex);
        dialogueTree tree = entity.getDialogueTree();

        if (tree == null) {
            tree = new dialogueTree();
            setPrivateField(entity, "dialogueTree", tree);
        }

        editingDialogueNode = tree;
        dialogueNodeStack.clear();
        dialogueNodeStack.add(tree);
        showDialogueEditor = true;
        updateDialogueInputs();
    }

    private void updateDialogueInputs() {
        if (editingDialogueNode != null) {
            dialogueNpcText.set(editingDialogueNode.getNpcText() != null ? editingDialogueNode.getNpcText() : "");
            dialogueNpcAction.set(editingDialogueNode.getNpcAction() != null ? editingDialogueNode.getNpcAction() : "");
        }
        dialogueResponseText.set("");
        editingResponseIndex = -1;
    }

    private void renderDialogueEditor() {
        ImGui.setNextWindowPos(650, 20, 0);
        ImGui.setNextWindowSize(730, 860, 0);

        if (!ImGui.begin("Dialogue Editor", new ImBoolean(true))) {
            ImGui.end();
            return;
        }

        if (editingEntityIndex >= 0 && editingEntityIndex < entities.size()) {
            Entity entity = entities.get(editingEntityIndex);
            ImGui.text("Editing: " + entity.getType());
            ImGui.separator();

            // Navigation breadcrumb
            ImGui.text("Path: Root");
            for (int i = 1; i < dialogueNodeStack.size(); i++) {
                ImGui.sameLine();
                ImGui.text(" > Response " + i);
            }
            ImGui.separator();

            // Current node editor
            ImGui.text("NPC Text:");
            ImGui.inputTextMultiline("##npcText", dialogueNpcText, 700, 80);

            ImGui.text("NPC Action (optional):");
            ImGui.inputText("##npcAction", dialogueNpcAction);
            ImGui.sameLine();
            ImGui.textDisabled("(?)");
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("e.g., move_to(10,20) or attack(enemy)");
            }

            // Update the node if user changed values
            if (editingDialogueNode != null) {
                String npcText = dialogueNpcText.get();
                String npcAction = dialogueNpcAction.get();
                
                if (!npcText.isEmpty()) {
                    setPrivateField(editingDialogueNode, "npcText", npcText);
                }
                if (!npcAction.isEmpty()) {
                    setPrivateField(editingDialogueNode, "npcAction", npcAction);
                }
            }

            ImGui.separator();

            // Responses list
            ImGui.text("Responses:");
            List<Response> responses = editingDialogueNode.getResponses();
            
            if (responses == null) {
                responses = new ArrayList<>();
                setPrivateField(editingDialogueNode, "responses", responses);
            }

            for (int i = 0; i < responses.size(); i++) {
                Response r = responses.get(i);
                ImGui.text("[" + i + "] " + (r.getResponseText() != null ? r.getResponseText() : "(empty)"));
                ImGui.sameLine();
                if (ImGui.button("Edit##resp" + i, 50, 20)) {
                    editingResponseIndex = i;
                    dialogueResponseText.set(r.getResponseText() != null ? r.getResponseText() : "");
                }
                ImGui.sameLine();
                if (ImGui.button("Go##resp" + i, 40, 20)) {
                    if (r.getNextNode() != null) {
                        dialogueNodeStack.add(r.getNextNode());
                        editingDialogueNode = r.getNextNode();
                        updateDialogueInputs();
                    }
                }
                ImGui.sameLine();
                if (ImGui.button("Del##resp" + i, 40, 20)) {
                    responses.remove(i);
                    break;
                }
            }

            ImGui.separator();

            // Add new response
            ImGui.text("Add Response:");
            ImGui.inputText("Response Text", dialogueResponseText);
            if (ImGui.button("Add Response##btn", 150, 25)) {
                String responseText = dialogueResponseText.get();
                if (!responseText.isEmpty()) {
                    Response newResponse = new Response();
                    setPrivateField(newResponse, "responseText", responseText);
                    
                    dialogueTree nextNode = new dialogueTree();
                    setPrivateField(nextNode, "responses", new ArrayList<>());
                    setPrivateField(newResponse, "next", nextNode);
                    
                    responses.add(newResponse);
                    dialogueResponseText.set("");
                }
            }

            ImGui.separator();

            // Navigation buttons
            if (dialogueNodeStack.size() > 1) {
                if (ImGui.button("Back", 100, 25)) {
                    dialogueNodeStack.remove(dialogueNodeStack.size() - 1);
                    editingDialogueNode = dialogueNodeStack.get(dialogueNodeStack.size() - 1);
                    updateDialogueInputs();
                }
            }

            if (ImGui.button("Save & Close", 100, 25)) {
                showDialogueEditor = false;
                editingEntityIndex = -1;
                editingDialogueNode = null;
                dialogueNodeStack.clear();
            }
        }

        ImGui.end();
    }

    // ===== ANIMATION EDITOR (Level 1 TODO) =====
    private void openAnimationEditor(int entityIndex) {
        editingEntityIndex = entityIndex;
        showAnimationEditor = true;
    }

    private void renderAnimationEditor() {
        ImGui.setNextWindowPos(650, 20, 0);
        ImGui.setNextWindowSize(730, 860, 0);

        if (!ImGui.begin("Animation Editor", new ImBoolean(true))) {
            ImGui.end();
            return;
        }

        if (editingEntityIndex >= 0 && editingEntityIndex < entities.size()) {
            Entity entity = entities.get(editingEntityIndex);
            animationStates animStates = entity.getAnimationStates();

            ImGui.text("Editing: " + entity.getType());
            ImGui.separator();

            if (animStates == null) {
                ImGui.text("No animation states defined for this entity");
            } else {
                // Edit each animation state
                editAnimationStateList("Idle", animStates.getIdleImagesPaths(), entity, "idle");
                ImGui.spacing();
                editAnimationStateList("Walking Up", animStates.getWalkingUpImagesPaths(), entity, "walking_up");
                ImGui.spacing();
                editAnimationStateList("Walking Down", animStates.getWalkingDownImagesPaths(), entity, "walking_down");
                ImGui.spacing();
                editAnimationStateList("Walking Left", animStates.getWalkingLeftImagesPaths(), entity, "walking_left");
                ImGui.spacing();
                editAnimationStateList("Walking Right", animStates.getWalkingRightImagesPaths(), entity, "walking_right");
            }

            ImGui.separator();

            if (ImGui.button("Close", 100, 25)) {
                showAnimationEditor = false;
                editingEntityIndex = -1;
            }
        }

        ImGui.end();
    }

    private void editAnimationStateList(String stateName, List<String> imagePaths, Entity entity, String stateKey) {
        ImGui.text(stateName + ":");
        ImGui.indent();
        if (imagePaths == null || imagePaths.isEmpty()) {
            ImGui.text("(No frames)");
        } else {
            for (int i = 0; i < imagePaths.size(); i++) {
                String imagePath = imagePaths.get(i);
                ImGui.text("[" + i + "] " + imagePath);
                //try{
                //    BufferedImage image = ImageIO.read(new File(RESOURCE_PATH + "\\textures\\" + imagePath));
                //    ImGui.image(createTextureFromBufferedImage(image), 96, 144);
                //} catch(Exception e){
                //    
                //}
                ImGui.sameLine();
                if (ImGui.button("X##anim" + stateKey + i, 25, 20)) {
                    imagePaths.remove(i);
                    break;
                }
            }
        }

        // Add new frame
        ImString newFramePath = new ImString(256);
        ImGui.inputText("##addFrame" + stateKey, newFramePath);
        ImGui.sameLine();
        if (ImGui.button("Add Frame##" + stateKey, 100, 20)) {
            String framePath = newFramePath.get();
            if (!framePath.isEmpty()) {
                imagePaths.add(framePath);
                newFramePath.set("");
            }
        }

        ImGui.unindent();
    }

    private void addEntity(int x, int y, String type, String name, String imagePath) {
        List<String> aiPackages = List.of("");
        Entity newEntity = new Entity();
        newEntity.setPosition(x * GRID_CELL_SIZE, y * GRID_CELL_SIZE);
        setPrivateField(newEntity, "type", type);
        setPrivateField(newEntity, "name", name);
        setPrivateField(newEntity, "imagePath", imagePath);
        newEntity.setAIAbilities(new AIAbilities(aiPackages));
        newEntity.setAnimationStates(new animationStates(false));
        setPrivateField(newEntity, "dialogueTree", new dialogueTree(false));
        // Check if this is the player entity
        boolean isPlayer = type.equals("player");
        
        // Only update collision grid if not player
        if (!isPlayer) {
            // Check if we can place the entity at this location
            if (!canPlaceEntity(x, y, -1)) {
                // Find nearest valid position
                int[] nearest = findNearestValidPosition(x, y, ENTITY_WIDTH, ENTITY_HEIGHT, -1);
                if (nearest != null) {
                    x = nearest[0];
                    y = nearest[1];
                    newEntity.setPosition(x * GRID_CELL_SIZE, y * GRID_CELL_SIZE);
                    System.out.println("Entity placed at nearest valid position: (" + x + ", " + y + ")");
                } else {
                    System.err.println("Could not find valid position to place entity");
                    return;
                }
            }
        }
        
        entities.add(newEntity);
        
        // Update collision grid if not player
        if (!isPlayer) {
            updateEntityInCollisionGrid(newEntity);
        }
        
        System.out.println("Added " + type + " at (" + x + ", " + y + ")");
    }

    private void addDoor(int x, int y, int targetLevel, int targetX, int targetY, String imagePath) {
        Door newDoor = new Door();
        setPrivateField(newDoor, "x", x * GRID_CELL_SIZE);
        setPrivateField(newDoor, "y", y * GRID_CELL_SIZE);
        setPrivateField(newDoor, "targetLevel", targetLevel);
        setPrivateField(newDoor, "targetX", targetX);
        setPrivateField(newDoor, "targetY", targetY);
        setPrivateField(newDoor, "imagePath", imagePath);
        
        // Check if we can place the door at this location
        if (!canPlaceDoor(x, y, -1)) {
            // Find nearest valid position
            int[] nearest = findNearestValidPosition(x, y, DOOR_WIDTH, DOOR_HEIGHT, -1);
            if (nearest != null) {
                x = nearest[0];
                y = nearest[1];
                setPrivateField(newDoor, "x", x * GRID_CELL_SIZE);
                setPrivateField(newDoor, "y", y * GRID_CELL_SIZE);
                System.out.println("Door placed at nearest valid position: (" + x + ", " + y + ")");
            } else {
                System.err.println("Could not find valid position to place door");
                return;
            }
        }
        
        doors.add(newDoor);
        updateDoorInCollisionGrid(newDoor);
        System.out.println("Added door at (" + x + ", " + y + ") -> Level " + targetLevel);
    }

    // TODO: stop using reflection, level 3
    private void setPrivateField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            System.err.println("Error setting " + fieldName + ": " + e.getMessage());
        }
    }

    private void saveAllLevels() {
        try {
            // Update the current level in the list
            levels.set(currentLevelIndex, createLevelFromEditor());

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject root = new JsonObject();
            root.add("levels", gson.toJsonTree(levels));

            File file = new File(RESOURCE_PATH + "\\levels.json");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(gson.toJson(root));
            }
            System.out.println("Saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Level createLevelFromEditor() {
        Level level = levels.get(currentLevelIndex);
        setPrivateField(level, "collisionGrid", collisionGrid);
        setPrivateField(level, "entities", entities);
        setPrivateField(level, "doors", doors);
        return level;
    }

    private void cleanup() {
        ImGui.destroyContext();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    // ===== LEVEL 2 TODO IMPLEMENTATIONS =====

    // ===== COLLISION GRID MANAGEMENT =====

    /**
     * Update collision grid when entity is added/moved
     */
    private void updateEntityInCollisionGrid(Entity entity) {
        int gridX = entity.getX() / GRID_CELL_SIZE;
        int gridY = entity.getY() / GRID_CELL_SIZE;
        
        for (int y = gridY; y < gridY + ENTITY_HEIGHT; y++) {
            for (int x = gridX; x < gridX + ENTITY_WIDTH; x++) {
                if (x >= 0 && x < GRID_WIDTH && y >= 0 && y < GRID_HEIGHT) {
                    collisionGrid[y][x] = 1;
                }
            }
        }
    }

    /**
     * Clear entity from collision grid
     */
    private void clearEntityFromCollisionGrid(Entity entity) {
        int gridX = entity.getX() / GRID_CELL_SIZE;
        int gridY = entity.getY() / GRID_CELL_SIZE;
        
        for (int y = gridY; y < gridY + ENTITY_HEIGHT; y++) {
            for (int x = gridX; x < gridX + ENTITY_WIDTH; x++) {
                if (x >= 0 && x < GRID_WIDTH && y >= 0 && y < GRID_HEIGHT) {
                    collisionGrid[y][x] = 0;
                }
            }
        }
    }

    /**
     * Update collision grid when door is added/moved
     */
    private void updateDoorInCollisionGrid(Door door) {
        int gridX = door.getX() / GRID_CELL_SIZE;
        int gridY = door.getY() / GRID_CELL_SIZE;
        
        for (int y = gridY; y < gridY + DOOR_HEIGHT; y++) {
            for (int x = gridX; x < gridX + DOOR_WIDTH; x++) {
                if (x >= 0 && x < GRID_WIDTH && y >= 0 && y < GRID_HEIGHT) {
                    collisionGrid[y][x] = 1;
                }
            }
        }
    }

    /**
     * Clear door from collision grid
     */
    private void clearDoorFromCollisionGrid(Door door) {
        int gridX = door.getX() / GRID_CELL_SIZE;
        int gridY = door.getY() / GRID_CELL_SIZE;
        
        for (int y = gridY; y < gridY + DOOR_HEIGHT; y++) {
            for (int x = gridX; x < gridX + DOOR_WIDTH; x++) {
                if (x >= 0 && x < GRID_WIDTH && y >= 0 && y < GRID_HEIGHT) {
                    collisionGrid[y][x] = 0;
                }
            }
        }
    }

    /**
     * Check if an entity can be placed at the given grid position
     * excludingIndex: -1 for new entities, or the index to exclude when checking existing entities
     */
    private boolean canPlaceEntity(int gridX, int gridY, int excludingIndex) {
        // Check bounds
        if (gridX < 0 || gridY < 0 || gridX + ENTITY_WIDTH > GRID_WIDTH || gridY + ENTITY_HEIGHT > GRID_HEIGHT) {
            return false;
        }
        
        // Check collision grid
        for (int y = gridY; y < gridY + ENTITY_HEIGHT; y++) {
            for (int x = gridX; x < gridX + ENTITY_WIDTH; x++) {
                if (collisionGrid[y][x] != 0) {
                    return false;
                }
            }
        }
        
        // Check other entities
        for (int i = 0; i < entities.size(); i++) {
            if (i == excludingIndex) continue;
            
            Entity e = entities.get(i);
            int eGridX = e.getX() / GRID_CELL_SIZE;
            int eGridY = e.getY() / GRID_CELL_SIZE;
            
            if (checkRectangleOverlap(gridX, gridY, ENTITY_WIDTH, ENTITY_HEIGHT,
                                     eGridX, eGridY, ENTITY_WIDTH, ENTITY_HEIGHT)) {
                return false;
            }
        }
        
        // Check doors
        for (int i = 0; i < doors.size(); i++) {
            Door d = doors.get(i);
            int dGridX = d.getX() / GRID_CELL_SIZE;
            int dGridY = d.getY() / GRID_CELL_SIZE;
            
            if (checkRectangleOverlap(gridX, gridY, ENTITY_WIDTH, ENTITY_HEIGHT,
                                     dGridX, dGridY, DOOR_WIDTH, DOOR_HEIGHT)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Check if a door can be placed at the given grid position
     * excludingIndex: -1 for new doors, or the index to exclude when checking existing doors
     */
    private boolean canPlaceDoor(int gridX, int gridY, int excludingIndex) {
        // Check bounds
        if (gridX < 0 || gridY < 0 || gridX + DOOR_WIDTH > GRID_WIDTH || gridY + DOOR_HEIGHT > GRID_HEIGHT) {
            return false;
        }
        
        // Check collision grid
        for (int y = gridY; y < gridY + DOOR_HEIGHT; y++) {
            for (int x = gridX; x < gridX + DOOR_WIDTH; x++) {
                if (collisionGrid[y][x] != 0) {
                    return false;
                }
            }
        }
        
        // Check entities
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            int eGridX = e.getX() / GRID_CELL_SIZE;
            int eGridY = e.getY() / GRID_CELL_SIZE;
            
            if (checkRectangleOverlap(gridX, gridY, DOOR_WIDTH, DOOR_HEIGHT,
                                     eGridX, eGridY, ENTITY_WIDTH, ENTITY_HEIGHT)) {
                return false;
            }
        }
        
        // Check other doors
        for (int i = 0; i < doors.size(); i++) {
            if (i == excludingIndex) continue;
            
            Door d = doors.get(i);
            int dGridX = d.getX() / GRID_CELL_SIZE;
            int dGridY = d.getY() / GRID_CELL_SIZE;
            
            if (checkRectangleOverlap(gridX, gridY, DOOR_WIDTH, DOOR_HEIGHT,
                                     dGridX, dGridY, DOOR_WIDTH, DOOR_HEIGHT)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Check if two rectangles overlap
     */
    private boolean checkRectangleOverlap(int x1, int y1, int w1, int h1,
                                         int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

    /**
     * Find the nearest valid position to place an entity/door
     * Returns [gridX, gridY] or null if no position found
     */
    private int[] findNearestValidPosition(int startGridX, int startGridY, int width, int height, int excludingIndex) {
        int maxDistance = Math.max(GRID_WIDTH, GRID_HEIGHT);
        
        for (int distance = 0; distance < maxDistance; distance++) {
            // Search in expanding square
            for (int x = startGridX - distance; x <= startGridX + distance; x++) {
                for (int y = startGridY - distance; y <= startGridY + distance; y++) {
                    // Only check cells at the current distance
                    if (Math.abs(x - startGridX) != distance && Math.abs(y - startGridY) != distance) {
                        continue;
                    }
                    
                    // Check if entity or door (based on excludingIndex)
                    boolean isEntity = excludingIndex >= 0;
                    boolean canPlace = isEntity ? 
                        canPlaceEntity(x, y, excludingIndex) : 
                        canPlaceDoor(x, y, -1);
                    
                    if (canPlace) {
                        return new int[]{x, y};
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Check for collision conflicts at level load and warn user
     */
    private void checkForCollisionConflicts() {
        boolean hasConflicts = false;
        
        // Check entity conflicts
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if (!e.getType().equals("player")) {
                int gridX = e.getX() / GRID_CELL_SIZE;
                int gridY = e.getY() / GRID_CELL_SIZE;
                
                // Check collision grid
                for (int y = gridY; y < gridY + ENTITY_HEIGHT; y++) {
                    for (int x = gridX; x < gridX + ENTITY_WIDTH; x++) {
                        if (x >= 0 && x < GRID_WIDTH && y >= 0 && y < GRID_HEIGHT) {
                            if (collisionGrid[y][x] != 0 && !isEntityAtPosition(i, x, y)) {
                                hasConflicts = true;
                                System.err.println("WARNING: Entity " + i + " overlaps with collision grid");
                                break;
                            }
                        }
                    }
                    if (hasConflicts) break;
                }
            }
        }
        
        // Check entity-entity conflicts
        for (int i = 0; i < entities.size(); i++) {
            Entity e1 = entities.get(i);
            if (e1.getType().equals("player")) continue;
            
            int e1GridX = e1.getX() / GRID_CELL_SIZE;
            int e1GridY = e1.getY() / GRID_CELL_SIZE;
            
            for (int j = i + 1; j < entities.size(); j++) {
                Entity e2 = entities.get(j);
                if (e2.getType().equals("player")) continue;
                
                int e2GridX = e2.getX() / GRID_CELL_SIZE;
                int e2GridY = e2.getY() / GRID_CELL_SIZE;
                
                if (checkRectangleOverlap(e1GridX, e1GridY, ENTITY_WIDTH, ENTITY_HEIGHT,
                                         e2GridX, e2GridY, ENTITY_WIDTH, ENTITY_HEIGHT)) {
                    hasConflicts = true;
                    System.err.println("WARNING: Entity " + i + " overlaps with Entity " + j);
                }
            }
        }
        
        if (hasConflicts) {
            System.err.println("WARNING: Level " + currentLevelIndex + " has collision conflicts");
        }
    }

    /**
     * Helper to check if a specific entity is at a grid position
     */
    private boolean isEntityAtPosition(int entityIndex, int gridX, int gridY) {
        Entity e = entities.get(entityIndex);
        int eGridX = e.getX() / GRID_CELL_SIZE;
        int eGridY = e.getY() / GRID_CELL_SIZE;
        return gridX >= eGridX && gridX < eGridX + ENTITY_WIDTH &&
               gridY >= eGridY && gridY < eGridY + ENTITY_HEIGHT;
    }

    // Load background image from file path
    private void loadBackgroundImage(String imagePath) {
        try {
            String oldImagePath = imagePath;
            imagePath = RESOURCE_PATH + "\\textures\\" + imagePath;
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                System.err.println("Background image file not found: " + imagePath);
                return;
            }

            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                System.err.println("Failed to read image file: " + imagePath);
                return;
            }

            // Crop if necessary
            if (image.getWidth() > 1280 || image.getHeight() > 720) {
                BufferedImage croppedImage = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = croppedImage.createGraphics();
                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();
                image = croppedImage;
                System.out.println("Background image was larger than 1272x720 and has been cropped to fit.");
            }

            // Convert BufferedImage to OpenGL texture
            backgroundImageTextureId = createTextureFromBufferedImage(image);
            setPrivateField(levels.get(currentLevelIndex), "backgroundImage", oldImagePath);
            
        } catch (IOException e) {
            System.err.println("Error loading background image: " + e.getMessage());
        }
    }

    // Convert BufferedImage to OpenGL texture ID
    private int createTextureFromBufferedImage(BufferedImage img) {
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

    // Add a new blank level
    private void addNewLevel() {
        Level newLevel = new Level();
        
        // Create blank collision grid
        int[][] newCollisionGrid = new int[GRID_HEIGHT][GRID_WIDTH];
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                newCollisionGrid[y][x] = 0;
            }
        }

        // Set fields using reflection
        setPrivateField(newLevel, "collisionGrid", newCollisionGrid);
        setPrivateField(newLevel, "entities", new ArrayList<Entity>());
        setPrivateField(newLevel, "doors", new ArrayList<Door>());
        setPrivateField(newLevel, "backgroundImage", "");

        levels.add(newLevel);
        loadLevel(levels.size() - 1);
        System.out.println("Created new level: " + currentLevelIndex);
    }

    // Delete the current level
    private void deleteCurrentLevel() {
        if (levels.size() <= 1) {
            System.err.println("Cannot delete the only level");
            return;
        }

        levels.remove(currentLevelIndex);
        if (currentLevelIndex >= levels.size()) {
            currentLevelIndex = levels.size() - 1;
        }
        loadLevel(currentLevelIndex);
        System.out.println("Deleted level. Now viewing level " + currentLevelIndex);
    }
}