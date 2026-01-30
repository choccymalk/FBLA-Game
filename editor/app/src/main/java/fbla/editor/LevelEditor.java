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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fbla.editor.jsonParser;
import fbla.editor.Level;
import fbla.editor.Entity;
import fbla.editor.Door;

public class LevelEditor {
    private static final int WINDOW_W = 1400;
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

    // Grid editing state
    private ImBoolean paintMode = new ImBoolean(false);
    private ImBoolean eraserMode = new ImBoolean(false);

    public static void main(String[] args) throws Exception {
        new LevelEditor().run();
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
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                handleGridClick();
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
        ImGuiIO io = ImGui.getIO();
        initFonts(io, "roboto", 16);
        imguiGlfw.init(window, true);
        imguiGl3.init();

        // Load levels
        parser = new jsonParser(new File(RESOURCE_PATH + "\\levels.json"));
        levels = parser.getLevels();
        if (!levels.isEmpty()) {
            loadLevel(0);
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

    private void loadLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= levels.size())
            return;

        currentLevelIndex = levelIndex;
        Level level = levels.get(levelIndex);
        collisionGrid = level.getCollisionGrid();
        entities = level.getEntities();
        doors = level.getDoors();
        selectedEntityIndex = -1;
    }

    private void handleGridClick() {
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(window, xpos, ypos);

        int canvasX = 20;
        int canvasY = 90;
        int canvasW = 53 * GRID_CELL_SIZE;
        int canvasH = 30 * GRID_CELL_SIZE;

        int mouseX = (int) xpos[0];
        int mouseY = (int) ypos[0];

        if (mouseX >= canvasX && mouseX <= canvasX + canvasW &&
            mouseY >= canvasY && mouseY <= canvasY + canvasH) {

            int gridX = (mouseX - canvasX) / GRID_CELL_SIZE;
            int gridY = (mouseY - canvasY) / GRID_CELL_SIZE;

            if (paintMode.get()) {
                collisionGrid[gridY][gridX] = 1;
            } else if (eraserMode.get()) {
                collisionGrid[gridY][gridX] = 0;
            }
        }
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            imguiGlfw.newFrame();
            ImGui.newFrame();

            renderUI();
            renderCanvas();

            ImGui.render();
            imguiGl3.renderDrawData(ImGui.getDrawData());

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void renderUI() {
        // Level selection
        ImGui.setNextWindowPos(20f, 20f);
        ImGui.begin("Level Editor", ImGuiWindowFlags.NoMove);
        ImGui.text("Current Level: " + currentLevelIndex);

        if (ImGui.button("Previous Level") && currentLevelIndex > 0) {
            loadLevel(currentLevelIndex - 1);
        }
        ImGui.sameLine();
        if (ImGui.button("Next Level") && currentLevelIndex < levels.size() - 1) {
            loadLevel(currentLevelIndex + 1);
        }
        ImGui.sameLine();
        if (ImGui.button("Save All")) {
            saveAllLevels();
        }

        ImGui.separator();

        // Grid editing
        ImGui.text("Grid Editing:");
        ImGui.checkbox("Paint Mode", paintMode);
        ImGui.sameLine();
        ImGui.checkbox("Eraser Mode", eraserMode);

        if (paintMode.get() && eraserMode.get()) {
            paintMode.set(false);
        }
        if (eraserMode.get() && paintMode.get()) {
            eraserMode.set(false);
        }

        ImGui.separator();
        ImGui.text("Visibility:");
        ImGui.checkbox("Show Collision Grid", showCollisionGrid);
        ImGui.checkbox("Show Entities", showEntities);
        ImGui.checkbox("Show Doors", showDoors);

        ImGui.separator();
        ImGui.text("Add Entity:");
        ImGui.inputText("Type", selectedEntityType);
        ImGui.inputFloat("X (grid)", entityX);
        ImGui.inputFloat("Y (grid)", entityY);

        if (ImGui.button("Add Entity##add")) {
            addEntity((int) entityX.get(), (int) entityY.get(), selectedEntityType.get());
        }

        ImGui.separator();
        ImGui.text("Add Door:");
        ImGui.inputText("Target Level", doorTargetLevel);
        ImGui.inputText("Target X", doorTargetX);
        ImGui.inputText("Target Y", doorTargetY);

        if (ImGui.button("Add Door##add")) {
            addDoor((int) entityX.get(), (int) entityY.get(),
                    Integer.parseInt(doorTargetLevel.get()),
                    Integer.parseInt(doorTargetX.get()),
                    Integer.parseInt(doorTargetY.get()));
        }

        ImGui.separator();
        ImGui.text("Entities (" + entities.size() + "):");
        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            ImGui.text("[" + i + "] " + e.getType() + " @ (" + e.getX() + ", " + e.getY() + ")");
            ImGui.sameLine();
            if (ImGui.button("X##remove" + i)) {
                entities.remove(i);
                i--;
            }
        }

        ImGui.separator();
        ImGui.text("Doors (" + doors.size() + "):");
        for (int i = 0; i < doors.size(); i++) {
            Door d = doors.get(i);
            ImGui.text("[" + i + "] Door @ (" + d.getX() + ", " + d.getY() + ") -> Level " + d.getTargetLevel());
            ImGui.sameLine();
            if (ImGui.button("X##removedoor" + i)) {
                doors.remove(i);
                i--;
            }
        }

    }

    private void renderCanvas() {
        int canvasX = 20;
        int canvasY = 90;
        int canvasW = 53 * GRID_CELL_SIZE;
        int canvasH = 30 * GRID_CELL_SIZE;

        // Draw background
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, winW, winH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

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

        // Draw entities
        if (showEntities) {
            for (Entity e : entities) {
                if (!e.getType().equals("door")) {
                    int px = canvasX + e.getX();
                    int py = canvasY + e.getY();
                    drawEntity(px, py, e);
                }
            }
        }

        // Draw doors
        if (showDoors) {
            for (Door d : doors) {
                int px = canvasX + d.getX();
                int py = canvasY + d.getY();
                drawDoor(px, py, d);
            }
        }
    }

    private void drawEntity(int x, int y, Entity e) {
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
        // Draw a rectangle for doors
        glColor4f(1, 1, 0, 0.8f);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + 24, y);
        glVertex2f(x + 24, y + 36);
        glVertex2f(x, y + 36);
        glEnd();
    }

    private void addEntity(int x, int y, String type) {
        Entity newEntity = new Entity();
        newEntity.setPosition(x * GRID_CELL_SIZE, y * GRID_CELL_SIZE);
        // Use reflection to set the type field since Entity doesn't expose a public setter
        setPrivateField(newEntity, "type", type);
        entities.add(newEntity);
        System.out.println("Added entity: " + type + " at (" + x + ", " + y + ")");
    }

    private void addDoor(int x, int y, int targetLevel, int targetX, int targetY) {
        Door newDoor = new Door();
        setPrivateField(newDoor, "x", x * GRID_CELL_SIZE);
        setPrivateField(newDoor, "y", y * GRID_CELL_SIZE);
        setPrivateField(newDoor, "targetLevel", targetLevel);
        setPrivateField(newDoor, "targetX", targetX);
        setPrivateField(newDoor, "targetY", targetY);
        doors.add(newDoor);
        System.out.println("Added door at (" + x + ", " + y + ") -> Level " + targetLevel);
    }

    private void setPrivateField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            System.err.println("Failed to set field " + fieldName + ": " + e.getMessage());
        }
    }

    private void saveAllLevels() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject root = new JsonObject();
            
            var levelsArray = gson.toJsonTree(levels);
            root.add("levels", levelsArray);

            File file = new File(RESOURCE_PATH + "\\levels.json");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(gson.toJson(root));
            }
            System.out.println("Saved levels to " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save levels: " + e.getMessage());
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
}