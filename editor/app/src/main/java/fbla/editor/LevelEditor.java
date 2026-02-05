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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;

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
    public static final int WINDOW_W = 1920;
    public static final int WINDOW_H = 1080;
    public static final String RESOURCE_PATH = System.getProperty("user.home")
            + "\\Desktop\\FBLA-Game\\game_resources";
    public static final long FRAMERATE = 20;

    // Grid settings
    public static final int GRID_CELL_SIZE = 24;
    public static final int GRID_WIDTH = 53;
    public static final int GRID_HEIGHT = 30;

    public long window;
    public int winW = WINDOW_W, winH = WINDOW_H;

    // ImGui
    public ImGuiImplGlfw imguiGlfw;
    public ImGuiImplGl3 imguiGl3;

    // Editor state
    private jsonParser parser;
    private List<Level> levels;
    private int currentLevelIndex = 0;
    private int[][] collisionGrid;
    private List<Entity> entities;
    private List<Door> doors;
    private List<Object3D> object3ds;
    public jsonParser object3DParser = new jsonParser(new File(RESOURCE_PATH + "\\levels.json"),
            new File(RESOURCE_PATH + "\\3dobjects.json"));

    private ImString backgroundImagePath = new ImString("", 256);
    private int backgroundImageTextureId = -1;
    private boolean isDraggingEntity = false;
    private boolean isDraggingDoor = false;
    private int draggingEntityIndex = -1;
    private int draggingDoorIndex = -1;
    private int dragPreviewGridX = -1;
    private int dragPreviewGridY = -1;

    private boolean isPanningCamera = false;

    private float cursorXPos = 0;
    private float cursorYPos = 0;

    // Collision grid update constants
    private static final int ENTITY_WIDTH = 3; // cells
    private static final int ENTITY_HEIGHT = 5; // cells
    private static final int DOOR_WIDTH = 2; // cells
    private static final int DOOR_HEIGHT = 3; // cells

    private int inGameFrameCount = 0;

    private Renderer3D renderer3d = new Renderer3D();
    private Renderer renderer = new Renderer(this, renderer3d);
    private UI ui = new UI(this);

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
            double[] xpos = new double[1];
            double[] ypos = new double[1];
            glfwGetCursorPos(win, xpos, ypos);

            int mouseX = (int) xpos[0];
            int mouseY = (int) ypos[0];
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                if (action == GLFW_PRESS) {
                    handleMousePress(mouseX, mouseY);
                } else if (action == GLFW_RELEASE) {
                    handleMouseRelease(mouseX, mouseY);
                } // } else if (action == GLFW_)
            } else if (button == GLFW_MOUSE_BUTTON_MIDDLE) {
                System.out.println(cursorXPos + "," + cursorYPos + " fromm callback " + mouseX + "," + mouseY);
                // figure out way user moved mouse more, x or y
                if (action == GLFW_PRESS) {
                    handleMiddleMouseButtonPress(mouseX, mouseY);
                } else if (action == GLFW_RELEASE) {
                    System.out.println("released");
                    handleMiddleMouseButtonRelease(mouseX, mouseY);
                } // else if(action )
            }
        });

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            double[] xpos = new double[1];
            double[] ypos = new double[1];
            glfwGetCursorPos(win, xpos, ypos);

            cursorXPos = (int) xpos[0];
            cursorYPos = (int) ypos[0];
        });

        glfwSetScrollCallback(window, (win, x, y) -> {

            double[] xpos = new double[1];
            double[] ypos = new double[1];
            glfwGetCursorPos(win, xpos, ypos);

            int mouseX = (int) xpos[0];
            int mouseY = (int) ypos[0];

            if (y > 0) {
                // 415 is canvas pos x in window, 1280 is size of canvas x
                // 15 is canvas pos y in window, 800 is size of canvas y
                // checks to see if mouse is in canvas
                if ((mouseX > 415 && mouseX < 415 + 1280) && (mouseY < 740)) {
                    renderer.setCameraPos(renderer.getCameraPosX(), renderer.getCameraPosY(),
                            renderer.getCameraPosZ() + 5);
                }
            } else if (y < 0) {
                if ((mouseX > 415 && mouseX < 415 + 1280) && (mouseY < 740)) {
                    renderer.setCameraPos(renderer.getCameraPosX(), renderer.getCameraPosY(),
                            renderer.getCameraPosZ() - 5);
                }
            } else {
                renderer.setCameraPos(renderer.getCameraPosX(), renderer.getCameraPosY(), renderer.getCameraPosZ());
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
        renderer.initFonts(io, "roboto", 16);

        // Load levels
        parser = new jsonParser(new File(RESOURCE_PATH + "\\levels.json"));
        levels = new ArrayList<>(parser.getLevels());
        List<Object3D> loadedObjects = new ArrayList<>();
        for (Object3D object3d : object3DParser.getAllObject3ds()) {
            System.out.println("Loaded Object with " + object3d.getModelPath() + " " + object3d.getTexturePath()
                    + " " + object3d.getName() + " @ (" + object3d.getX() + "," + object3d.getY() + ","
                    + object3d.getZ() + ") and scale of (" + object3d.getScaleX() + "," + object3d.getScaleY() + ","
                    + object3d.getScaleZ() + ") with level index of " + object3d.getLevelIndex());
            Object3D newModel = renderer3d.load3DObject(object3d.getModelPath(),
                    renderer.loadTexture3DObj(object3d.getTexturePath()),
                    object3d.getName(), object3d.getX(), object3d.getY(), object3d.getZ(), object3d.getScaleX(),
                    object3d.getScaleY(), object3d.getScaleZ(), object3d.getRotationX(), object3d.getRotationY(),
                    object3d.getRotationZ());
            newModel.setLevelIndex(object3d.getLevelIndex());
            newModel.setModelPath(object3d.getModelPath());
            newModel.setTexturePath(object3d.getTexturePath());
            newModel.setName(object3d.getName());
            loadedObjects.add(newModel);
        }
        object3ds = loadedObjects;
        if (!levels.isEmpty()) {
            loadLevel(0);
        }
    }

    void loadLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= levels.size())
            return;

        currentLevelIndex = levelIndex;
        Level level = levels.get(levelIndex);
        collisionGrid = level.getCollisionGrid();
        entities = new ArrayList<>(level.getEntities());
        doors = new ArrayList<>(level.getDoors());

        if (object3ds != null) {
            System.out.println(object3ds.size());
        }
        // Rebuild collision grid from entities and doors
        rebuildCollisionGrid();

        // Check for conflicts
        checkForCollisionConflicts();

        // Load background image if it exists
        // TODO: fix background image appearing as completely white, level 1
        String bgImage = level.getBackgroundImage();
        if (bgImage != null && !bgImage.isEmpty()) {
            backgroundImagePath.set(bgImage);
            ui.loadBackgroundImage(bgImage);
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
        // We need to preserve manually painted cells, so we iterate through
        // entities/doors
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
        int canvasX = 415;
        int canvasY = 20;
        int canvasW = GRID_WIDTH * GRID_CELL_SIZE;
        int canvasH = GRID_HEIGHT * GRID_CELL_SIZE;

        System.out.println("mouse x: " + mouseX + " mouse y:" + mouseY);
        // Check if click is within canvas
        if (mouseX >= canvasX && mouseX <= canvasX + canvasW &&
                mouseY >= canvasY && mouseY <= canvasY + canvasH) {

            // Check if clicking on an entity
            for (int i = 0; i < entities.size(); i++) {
                Entity e = entities.get(i);
                int entityScreenX = canvasX + e.getX();
                int entityScreenY = canvasY + e.getY();
                System.out.println(
                        "e x: " + e.getX() + ", e y: " + e.getY() + ", mouse x: " + mouseX + ", mouse y: " + mouseY);
                if (mouseX >= entityScreenX - 12 && mouseX <= entityScreenX + 12 &&
                        mouseY >= entityScreenY - 12 && mouseY <= entityScreenY + 12) {
                    isDraggingEntity = true;
                    draggingEntityIndex = i;
                    System.out.println("mouse clicked on entity with type: " + e.getType());
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
                if (ui.getPaintMode()) {
                    collisionGrid[gridY][gridX] = 1;
                } else if (ui.getEraserMode()) {
                    collisionGrid[gridY][gridX] = 0;
                }
            }
        }
    }

    private void handleMouseRelease(int mouseX, int mouseY) {
        int canvasX = 415;
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
                    if (!e.getType().equals("player")) {
                        clearEntityFromCollisionGrid(e);
                    }

                    // Try to place at new position
                    int newGridX = gridX;
                    int newGridY = gridY;

                    if (!canPlaceEntity(newGridX, newGridY, draggingEntityIndex)) {
                        // Find nearest valid position
                        int[] nearest = findNearestValidPosition(newGridX, newGridY, ENTITY_WIDTH, ENTITY_HEIGHT,
                                draggingEntityIndex);
                        if (nearest != null) {
                            newGridX = nearest[0];
                            newGridY = nearest[1];
                        } else {
                            // No valid position, revert to old position
                            System.err.println("No valid position found for entity");
                            if (!e.getType().equals("player")) {
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
                    if (!e.getType().equals("player")) {
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

    private int initalMMBX = -1;
    private int initalMMBY = -1;
    private float initialCameraRotationX = -1;
    private float initialCameraRotationY = -1;
    private float initialCameraRotationZ = -1;

    private void handleMiddleMouseButtonPress(int x, int y) {
        // TODO: handle panning and tilting of camera
        System.out.println("middle mouse button pressed");
        isPanningCamera = true;
        initalMMBX = x;
        initalMMBY = y;
        initialCameraRotationX = renderer.getCameraRotX();
        initialCameraRotationY = renderer.getCameraRotY();
        initialCameraRotationZ = renderer.getCameraRotZ();
    }

    private void handleMiddleMouseButtonRelease(int x, int y) {
        isPanningCamera = false;
        System.out.println("mmb not pressed anymore");
    }

    // in render loop, this method is called if ispanningcamera is true
    private void panCamera() {
        System.out.println("panning");

        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(window, xpos, ypos);

        int mouseX = (int) xpos[0];
        int mouseY = (int) ypos[0];

        // TODO: finish camera panning

        // Calculate mouse movement delta
        int deltaX = mouseX - initalMMBX;
        int deltaY = mouseY - initalMMBY;

        System.out.println(deltaX);
        System.out.println(deltaY);

        // Sensitivity factor for rotation (adjust as needed)
        float sensitivity = 0.2f;

        // Calculate new rotation angles
        float newRotationY = initialCameraRotationY + deltaX * sensitivity;
        float newRotationX = initialCameraRotationX - deltaY * sensitivity; // Invert Y axis for natural rotation

        // Optional: Clamp vertical rotation to prevent camera flipping
        // Typically limit pitch to [-89°, 89°] to avoid gimbal lock issues
        // newRotationX = Math.max(-89.0f, Math.min(89.0f, newRotationX));

        // Set the new camera rotation
        //renderer.setCameraRotX(newRotationX);
        //renderer.setCameraRotY(newRotationY);
        // Z rotation usually not changed for basic camera panning
        //renderer.setCameraRotZ(initialCameraRotationZ);
        //renderer.setCameraRot(deltaX, deltaY, initialCameraRotationZ);
        renderer.setCameraRot(newRotationX, newRotationY, initialCameraRotationZ);
    }

    void updateDragPreview() {
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
            renderer.tick(window, winW, winH);
            if (isPanningCamera)
                panCamera();
            try {
                Thread.sleep(1000 / FRAMERATE);
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public List<Door> getDoors() {
        return this.doors;
    }

    public Renderer getRenderer() {
        return this.renderer;
    }

    public boolean getIsDraggingEntity() {
        return this.isDraggingEntity;
    }

    public boolean getIsDraggingDoor() {
        return this.isDraggingDoor;
    }

    public boolean getShowDoors() {
        return ui.getShowDoors();
    }

    public boolean getShowEntities() {
        return ui.getShowEntities();
    }

    public int getCurrentLevelIndex() {
        return this.currentLevelIndex;
    }

    public List<Level> getLevels() {
        return this.levels;
    }

    public UI getUi() {
        return this.ui;
    }

    public ImGuiImplGlfw getImGuiImplGlfw() {
        return this.imguiGlfw;
    }

    public ImGuiImplGl3 getImGuiImplGl3() {
        return this.imguiGl3;
    }

    public ImString getBackgroundImagePath() {
        return this.backgroundImagePath;
    }

    public int getDragPreviewGridX() {
        return this.dragPreviewGridX;
    }

    public int getDragPreviewGridY() {
        return this.dragPreviewGridY;
    }

    public int[][] getCollisionGrid() {
        return this.collisionGrid;
    }

    public boolean getShowCollisionGrid() {
        return this.ui.getShowCollisionGrid();
    }

    public List<Object3D> getObject3ds() {
        return object3ds;
    }

    void addEntity(int x, int y, String type, String name, String imagePath) {
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

    public void add3DObject(float x, float y, float z, float sx, float sy, float sz, float ax, float ay, float az,
            String name, String path, int textureId, String texturePath) {
        Object3D model = renderer3d.load3DObject(path, renderer.loadTexture3DObj(texturePath), name, x, y, z, sx, sy,
                sz, ax, ay, az);
        model.setLevelIndex(currentLevelIndex);
        model.setModelPath(path);
        model.setTexturePath(texturePath);
        object3ds.add(model);
        levels.get(currentLevelIndex).appendItemTo3DObjectsList(model.getName());
    }

    public void addDoor(int x, int y, int targetLevel, int targetX, int targetY, String imagePath) {
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
    void setPrivateField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            System.err.println("Error setting " + fieldName + ": " + e.getMessage());
        }
    }

    void saveAllLevels() {
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

    public void saveAllObject3Ds() {
        try {
            // Update the current level in the list
            // levels.set(currentLevelIndex, createLevelFromEditor());

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject root = new JsonObject();
            root.add("3d_objects", gson.toJsonTree(object3ds));

            File file = new File(RESOURCE_PATH + "\\3dobjects.json");
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
        List<String> object3dList = new ArrayList<>();
        for (Object3D object3d : object3ds) {
            if (object3d.getLevelIndex() == currentLevelIndex) {
                object3dList.add(object3d.getName());
                System.out.println("wrote " + object3d.getName() + " obj path: " + object3d.getModelPath());
            }
        }
        level.set3DObjectsList(object3dList);
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
    void clearEntityFromCollisionGrid(Entity entity) {
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
     * excludingIndex: -1 for new entities, or the index to exclude when checking
     * existing entities
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
            if (i == excludingIndex)
                continue;

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
     * excludingIndex: -1 for new doors, or the index to exclude when checking
     * existing doors
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
            if (i == excludingIndex)
                continue;

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
                    boolean canPlace = isEntity ? canPlaceEntity(x, y, excludingIndex) : canPlaceDoor(x, y, -1);

                    if (canPlace) {
                        return new int[] { x, y };
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
                    if (hasConflicts)
                        break;
                }
            }
        }

        // Check entity-entity conflicts
        for (int i = 0; i < entities.size(); i++) {
            Entity e1 = entities.get(i);
            if (e1.getType().equals("player"))
                continue;

            int e1GridX = e1.getX() / GRID_CELL_SIZE;
            int e1GridY = e1.getY() / GRID_CELL_SIZE;

            for (int j = i + 1; j < entities.size(); j++) {
                Entity e2 = entities.get(j);
                if (e2.getType().equals("player"))
                    continue;

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

    // Add a new blank level
    void addNewLevel() {
        List<String> object3ds = new ArrayList<>();
        Level newLevel = new Level();

        // Create blank collision grid
        int[][] newCollisionGrid = new int[GRID_HEIGHT][GRID_WIDTH];
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                newCollisionGrid[y][x] = 0;
            }
        }

        newLevel.set3DObjectsList(object3ds);

        // Set fields using reflection
        setPrivateField(newLevel, "collisionGrid", newCollisionGrid);
        setPrivateField(newLevel, "entities", new ArrayList<Entity>());
        setPrivateField(newLevel, "doors", new ArrayList<Door>());
        setPrivateField(newLevel, "backgroundImage", "");
        object3ds = new ArrayList<>(newLevel.getObject3DList());

        levels.add(newLevel);
        loadLevel(levels.size() - 1);
        System.out.println("Created new level: " + currentLevelIndex);
    }

    // Delete the current level
    void deleteCurrentLevel() {
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