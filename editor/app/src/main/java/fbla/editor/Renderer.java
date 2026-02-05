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
import org.lwjgl.stb.STBDXT;
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
import fbla.editor.Renderer3D;

public class Renderer {

    private static final int MAX_QUADS = 1000; // Max sprites per draw call
    private static final int VERTICES_PER_QUAD = 4;
    private static final int FLOATS_PER_VERTEX = 4; // x, y, u, v
    private final FloatBuffer batchBuffer = org.lwjgl.BufferUtils
            .createFloatBuffer(MAX_QUADS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX);

    private int currentBatchTex = -1;
    private int quadCount = 0;

    private Map<String, Integer> fontMap = new HashMap<>();

    private String RESOURCE_PATH = LevelEditor.RESOURCE_PATH;

    private int backgroundImageTextureId = -1;

    private LevelEditor editor;

    private int canvasX = 415;
    private int canvasY = 20;
    private int canvasW = LevelEditor.GRID_WIDTH * LevelEditor.GRID_CELL_SIZE;
    private int canvasH = LevelEditor.GRID_HEIGHT * LevelEditor.GRID_CELL_SIZE;

    private float cameraPosX = 0;
    private float cameraPosY = 0;
    private float cameraPosZ = 0;
    private float cameraRotX = 0;
    private float cameraRotY = 0;
    private float cameraRotZ = 0;

    private Renderer3D renderer3d;

    public Renderer(LevelEditor editor, Renderer3D renderer3d) {
        this.editor = editor;
        this.renderer3d = renderer3d;
    }

    public int getBackgroundImageTextureId() {
        return this.backgroundImageTextureId;
    }

    public void setBackgroundImageTextureId(int id) {
        this.backgroundImageTextureId = id;
    }

    public Renderer3D getRenderer3d(){
        return this.renderer3d;
    }

    private void buildFontMap() {
        String fontPathString = RESOURCE_PATH + "\\editorfonts\\roboto\\8\\";
        File fontDirectory = new File(fontPathString);
        File[] fontList = fontDirectory.listFiles();

        System.out.println(fontPathString);

        if (fontList != null) {
            for (File file : fontList) {
                fontMap.put(file.getName().substring(0, file.getName().length() - 4), loadTexture(file.getPath()));
                System.out.println(file.getName() + " path: " + file.getPath());
            }
        }
    }

    public void initFonts(final ImGuiIO io, String defaultFont, int defaultFontSize) {
        io.getFonts().setFreeTypeRenderer(true);
        final ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder(); // Glyphs ranges provide
        rangesBuilder.addRanges(io.getFonts().getGlyphRangesDefault());
        final ImFontConfig fontConfig = new ImFontConfig();
        final short[] glyphRanges = rangesBuilder.buildRanges();
        System.out.println(RESOURCE_PATH + "\\fonts\\" + defaultFont + ".ttf");
        io.getFonts().addFontFromFileTTF(RESOURCE_PATH + "\\fonts\\" + defaultFont + ".ttf", defaultFontSize,
                fontConfig, glyphRanges);
        io.getFonts().build();
        fontConfig.destroy();
        buildFontMap();
    }

    public void tick(long window, int winW, int winH) {
        glfwPollEvents();
        glClearColor(0.15f, 0.15f, 0.15f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Setup projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, winW, winH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Disable lighting for 2D rendering
        //glDisable(GL_LIGHTING);
        //glDisable(GL_LIGHT0);
        //glDisable(GL_LIGHT1);
        //glDisable(GL_COLOR_MATERIAL);

        // Set material to use vertex color directly
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        // Render canvas (OpenGL)
        renderCanvas();

        // Update drag preview position
        editor.updateDragPreview();

        // Render UI (ImGui on top)
        editor.getImGuiImplGlfw().newFrame();
        editor.getImGuiImplGl3().newFrame();
        ImGui.newFrame();

        editor.getUi().renderUI();

        ImGui.render();
        editor.getImGuiImplGl3().renderDrawData(ImGui.getDrawData());

        glfwSwapBuffers(window);
    }

    private static final int MAX_BATCH_SIZE = 1000; // Max quads per draw call
    // private float[] batchBuffer = new float[MAX_BATCH_SIZE * 4 * 4]; // 4
    // vertices, 4 attributes (x,y,u,v)
    private int batchIndex = 0;
    // private int currentBatchTex = -1;

    public void draw2D(int texId, float x, float y, float w, float h) {
        // 1. If texture changes or buffer is full, push the data to the GPU
        if (texId != currentBatchTex || quadCount >= MAX_QUADS) {
            flushBatch();
            currentBatchTex = texId;
        }

        // 2. Add vertex data [x, y, u, v] for 4 corners of the quad
        // Top-Left
        batchBuffer.put(x).put(y).put(0f).put(0f);
        // Top-Right
        batchBuffer.put(x + w).put(y).put(1f).put(0f);
        // Bottom-Right
        batchBuffer.put(x + w).put(y + h).put(1f).put(1f);
        // Bottom-Left
        batchBuffer.put(x).put(y + h).put(0f).put(1f);

        quadCount++;
    }

    public void flushBatch() {
        if (quadCount == 0)
            return;

        // Prepare buffer for reading
        batchBuffer.flip();

        // Bind texture and enable state
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, currentBatchTex);

        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);

        // Set pointers: Stride is 4 floats (16 bytes)
        // Vertex pointer: size 2 (x,y), type float, stride 16, pointer start 0
        glVertexPointer(2, GL_FLOAT, 16, batchBuffer);

        // TexCoord pointer: size 2 (u,v), type float, stride 16, pointer start 8 bytes
        // (after x,y)
        batchBuffer.position(2);
        glTexCoordPointer(2, GL_FLOAT, 16, batchBuffer);

        // DRAW!
        glDrawArrays(GL_QUADS, 0, quadCount * VERTICES_PER_QUAD);

        // Cleanup
        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        batchBuffer.clear();
        quadCount = 0;
    }

    private void renderCanvas() {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Draw background image if loaded
        if (backgroundImageTextureId != -1) {
            glColor4f(1, 1, 1, 1); // Reset color to white for textures
            draw2D(backgroundImageTextureId, canvasX, canvasY, canvasW, canvasH);
            flushBatch();
        }
        // reset colors
        glColor4f(1, 1, 1, 1);
        // Draw collision grid
        if (editor.getShowCollisionGrid()) {
            drawCollisionLayer();
        }
        // reset colors
        glColor4f(1, 1, 1, 1);
        // Draw grid lines
        drawGridLineLayer();

        // Draw doors
        if (editor.getShowDoors()) {
            drawDoorLayer();
        }
        // reset colors
        glColor4f(1, 1, 1, 1);
        // Draw entities
        if (editor.getShowEntities()) {
            drawEntityLayer();
        }
        // reset colors
        glColor4f(1, 1, 1, 1);
        flushBatch();
        // Draw 3d objects
        draw3DLayer();

        // Draw drag preview
        if (editor.getIsDraggingEntity() && editor.getDragPreviewGridX() >= 0 && editor.getDragPreviewGridY() >= 0) {
            int px = canvasX + editor.getDragPreviewGridX() * LevelEditor.GRID_CELL_SIZE;
            int py = canvasY + editor.getDragPreviewGridY() * LevelEditor.GRID_CELL_SIZE;
            px = px - 94;
            glColor4f(0, 1, 0, 0.5f);
            glBegin(GL_TRIANGLE_FAN);
            int radius = 12;
            for (int i = 0; i < 16; i++) {
                double angle = (i / 16.0) * Math.PI * 2;
                glVertex2f(px + (float) Math.cos(angle) * radius,
                        py + (float) Math.sin(angle) * radius);
            }
            glEnd();
        } else if (editor.getIsDraggingDoor() && editor.getDragPreviewGridX() >= 0
                && editor.getDragPreviewGridY() >= 0) {
            int px = canvasX + editor.getDragPreviewGridX() * LevelEditor.GRID_CELL_SIZE;
            int py = canvasY + editor.getDragPreviewGridY() * LevelEditor.GRID_CELL_SIZE;
            glColor4f(1, 1, 0, 0.5f);
            glBegin(GL_QUADS);
            glVertex2f(px, py);
            glVertex2f(px + 48, py);
            glVertex2f(px + 48, py + 72);
            glVertex2f(px, py + 72);
            glEnd();
        }

        //flushBatch();
    }

    private void drawCollisionLayer() {
        glColor4f(1, 0, 0, 1f);
        glBegin(GL_QUADS);
        for (int y = 0; y < LevelEditor.GRID_HEIGHT; y++) {
            for (int x = 0; x < LevelEditor.GRID_WIDTH; x++) {
                if (editor.getCollisionGrid()[y][x] == 1) {
                    int px = canvasX + x * LevelEditor.GRID_CELL_SIZE;
                    int py = canvasY + y * LevelEditor.GRID_CELL_SIZE;
                    glVertex2f(px, py);
                    glVertex2f(px + LevelEditor.GRID_CELL_SIZE, py);
                    glVertex2f(px + LevelEditor.GRID_CELL_SIZE, py + LevelEditor.GRID_CELL_SIZE);
                    glVertex2f(px, py + LevelEditor.GRID_CELL_SIZE);
                }
            }
        }
        glEnd();
    }

    private void drawGridLineLayer() {
        glColor4f(0.3f, 0.3f, 0.3f, 0.5f);
        glBegin(GL_LINES);
        for (int x = 0; x <= LevelEditor.GRID_WIDTH; x++) {
            int px = canvasX + x * LevelEditor.GRID_CELL_SIZE;
            glVertex2f(px, canvasY);
            glVertex2f(px, canvasY + canvasH);
        }
        for (int y = 0; y <= LevelEditor.GRID_HEIGHT; y++) {
            int py = canvasY + y * LevelEditor.GRID_CELL_SIZE;
            glVertex2f(canvasX, py);
            glVertex2f(canvasX + canvasW, py);
        }
        glEnd();
    }

    public void setCameraPos(float x, float y, float z){
        this.cameraPosX = x;
        this.cameraPosY = y;
        this.cameraPosZ = z;
    }

    public float getCameraPosX(){
        return this.cameraPosX;
    }

    public float getCameraPosY(){
        return this.cameraPosY;
    }

    public float getCameraPosZ(){
        return this.cameraPosZ;
    }

    public void setCameraRot(float x, float y, float z){
        this.cameraRotX = x;
        this.cameraRotY = y;
        this.cameraRotZ = z;
    }

    public float getCameraRotX(){
        return this.cameraRotX;
    }

    public float getCameraRotY(){
        return this.cameraRotY;
    }

    public float getCameraRotZ(){
        return this.cameraRotZ;
    }

    public void setCameraRotX(float x){
        this.cameraRotX = x;
    }

    public void setCameraRotY(float y){
        this.cameraRotX = y;
    }

    public void setCameraRotZ(float z){
        this.cameraRotX = z;
    }

    private void draw3DLayer() {
        glEnable(GL_DEPTH_TEST);
        glClear(GL_DEPTH_BUFFER_BIT);

        // 1. Setup Projection (Perspective)
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();

        float aspect = (float) canvasW / (float) canvasH;
        renderer3d.setupPerspective(45.0f, aspect, 0.1f, 1000.0f);

        // 2. Setup Camera (ModelView)
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        // --- CAMERA LOGIC START ---
        // We rotate and translate the WORLD in the opposite direction of the camera.
        // Order matters: Rotate first, then Translate (First Person view).
        
        glRotatef(-cameraRotX, 1.0f, 0.0f, 0.0f); // Pitch
        glRotatef(-cameraRotY, 0.0f, 1.0f, 0.0f); // Yaw
        glRotatef(-cameraRotZ, 0.0f, 0.0f, 1.0f); // Roll
        
        glTranslatef(-cameraPosX, -cameraPosY, -cameraPosZ);
        // --- CAMERA LOGIC END ---

        // 3. Render Objects
        for (Object3D obj3d : renderer3d.getLoaded3DObjects()) {
            if(editor.getLevels().get(editor.getCurrentLevelIndex()).getObject3DList().contains(obj3d.getName())){
                renderer3d.clipObjectToCanvasBounds((float) canvasX, (float) canvasY, (float) canvasW, (float) canvasH, obj3d);
            }
        }

        // 4. Cleanup
        glPopMatrix(); // Pop ModelView
        
        glMatrixMode(GL_PROJECTION);
        glPopMatrix(); // Pop Projection
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    private void drawDoorLayer() {
        for (int i = 0; i < editor.getDoors().size(); i++) {
            Door d = editor.getDoors().get(i);
            int px = canvasX + d.getX();
            int py = canvasY + d.getY();
            drawDoor(px, py);
        }
    }

    private void drawEntityLayer() {
        // entities
        for (int i = 0; i < editor.getEntities().size(); i++) {
            Entity e = editor.getEntities().get(i);
            if (!e.getType().equals("door")) {
                int px = canvasX + e.getX();
                int py = canvasY + e.getY();
                drawEntity(px, py, e);
            }
        }
        // entity names
        for (int i = 0; i < editor.getEntities().size(); i++) {
            Entity e = editor.getEntities().get(i);
            if (!e.getType().equals("door")) {
                int px = canvasX + e.getX();
                int py = canvasY + e.getY();
                if (e.getName() != null && !editor.getIsDraggingEntity() && !editor.getIsDraggingDoor()) {
                    drawNameOverEntity(e.getName(), px, py);
                } else if (!editor.getIsDraggingEntity() && !editor.getIsDraggingDoor()) {
                    drawNameOverEntity(e.getType(), px, py);
                }
            }
        }
    }

    private void drawEntity(int x, int y, Entity e) {
        // TODO: draw entity sprite instead of circle, level 3
        // Entity.java, level 3
        // Draw a circle for entities
        glColor4f(0, 1, 0, 1f);
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
        // Draw a rectangle for doors (48x72 = 2x3 cells)
        glColor4f(1, 1, 0, 1f);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + 48, y);
        glVertex2f(x + 48, y + 72);
        glVertex2f(x, y + 72);
        glEnd();
    }

    private void drawNameOverEntity(String name, int entityXPos, int entityYPos) {
        glColor4f(1, 1, 1, 1); // Ensure white color for text
        int i = 0;
        for (char letter : name.toCharArray()) {
            String fontKey = getCharacterFontKey(letter);
            if (fontKey != null && fontMap.containsKey(fontKey)) {
                draw2D(fontMap.get(fontKey), entityXPos + i, entityYPos - 30, 12, 16);
                i += 12;
            } else {
                i += 12; // Skip unknown characters
            }
        }
    }

    private String getCharacterFontKey(char c) {
        if (c >= 'a' && c <= 'z') {
            return "latin_small_letter_" + c;
        } else if (c >= 'A' && c <= 'Z') {
            return "latin_capital_letter_" + Character.toLowerCase(c);
        } else if (c >= '0' && c <= '9') {
            return "digit_" + toWordDigit(c);
        } else if (c == ' ') {
            return null; // Space doesn't need a texture
        }
        return null;
    }

    private String toWordDigit(char c) {
        String[] words = { "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine" };
        return words[c - '0'];
    }

    public int loadTexture(String filePath) {
        try {
            BufferedImage img = ImageIO.read(new File(filePath));
            int[] pixels = new int[img.getWidth() * img.getHeight()];
            img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

            ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int pixel = pixels[y * img.getWidth() + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                    buffer.put((byte) ((pixel >> 8) & 0xFF)); // G
                    buffer.put((byte) (pixel & 0xFF)); // B
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
                }
            }
            buffer.flip();

            int texId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); // Pixel art look
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, img.getWidth(), img.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE,
                    buffer);

            return texId;
        } catch (IOException e) {
            System.err.println("Failed to load: " + filePath);
            return -1;
        }
    }

    // ONLY used for loading a new 3d object in loadlevel
    public int loadTexture3DObj(String filePath) {
        filePath = RESOURCE_PATH + "\\textures\\" + filePath;
        try {
            BufferedImage img = ImageIO.read(new File(filePath));
            int[] pixels = new int[img.getWidth() * img.getHeight()];
            img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

            ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int pixel = pixels[y * img.getWidth() + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                    buffer.put((byte) ((pixel >> 8) & 0xFF)); // G
                    buffer.put((byte) (pixel & 0xFF)); // B
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
                }
            }
            buffer.flip();

            int texId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); // Pixel art look
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, img.getWidth(), img.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE,
                    buffer);

            return texId;
        } catch (IOException e) {
            System.err.println("Failed to load: " + filePath);
            return -1;
        }
    }

}
