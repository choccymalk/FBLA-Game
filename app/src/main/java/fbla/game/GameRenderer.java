package fbla.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.*;

import fbla.game.main.GameState;

public class GameRenderer {
    private final main game;
    private final ImGuiImplGlfw imguiGlfw;
    private final ImGuiImplGl3 imguiGl3;

    private static final int GRID_CELL_SIZE = 24;
    private boolean DRAW_DEBUG_GRID;
    private static final int DOOR_HEIGHT = 144;
    private static final int DOOR_WIDTH = 96;

    private static final int MAX_QUADS = 1000; // Max sprites per draw call
    private static final int VERTICES_PER_QUAD = 4;
    private static final int FLOATS_PER_VERTEX = 4; // x, y, u, v
    private final FloatBuffer batchBuffer = org.lwjgl.BufferUtils
            .createFloatBuffer(MAX_QUADS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX);

    private int currentBatchTex = -1;
    private int quadCount = 0;

    private int inGameFrameCount = 0;

    private GameState lastStateForOptionsMenu = GameState.TITLE_SCREEN;

    // 3D Object storage

    public class Object3D {
        public String name;
        public int vertexCount;
        public int vboId = -1; // -1 means no VBO is assigned yet
        public int textureId;
        public float x, y, z;
        public float scaleX = 1f, scaleY = 1f, scaleZ = 1f;
        public float rotationX = 0f, rotationY = 0f, rotationZ = 0f;

        public Object3D(String name, int vertexCount) {
            this.name = name;
            this.vertexCount = vertexCount;
        }
    }

    private List<Object3D> loaded3DObjects = new ArrayList<>();

    public GameRenderer(main game, ImGuiImplGlfw imguiGlfw, ImGuiImplGl3 imguiGl3) {
        this.game = game;
        this.imguiGlfw = imguiGlfw;
        this.imguiGl3 = imguiGl3;
    }

    public void render(int winW, int winH, BufferedImage backgroundBI, int backgroundTex, BufferedImage playerBI,
            BufferedImage gridBI, int gridTex, BufferedImage titleScreenBI, int titleScreenTex,
            BufferedImage titleScreenGameLogoBI, int titleScreenGameLogoTex) {
        // 1. Reset State
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        lastBoundTex = -1; // Reset texture tracking for the new frame

        // 2. Setup Ortho Projection (2D)
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, winW, winH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        switch (game.getCurrentGameState()) {
            case TITLE_SCREEN:
                // renderTitleScreen(winW, winH, titleScreenTex, titleScreenBI);
                // Background is 2D
                draw2D(titleScreenTex, 0, 0, winW, winH);
                flushBatch(); // Finish 2D before ImGui
                renderTitleScreenUI(winW, winH);
                break;
            case IN_GAME:
                renderInGame(winW, winH, backgroundTex, gridTex);
                if (game.messageBoxDisplayed) {
                    renderMessageBox(winW, winH);
                }
                break;
            case PAUSED:
                renderPauseMenu(winW, winH);
                break;
            case OPTIONS:
                draw2D(titleScreenTex, 0, 0, winW, winH);
                flushBatch(); // Finish 2D before ImGui
                renderOptions(winW, winH, titleScreenTex, titleScreenBI);
                break;
        }
    }

    private void renderTitleScreenUI(int winW, int winH) {// , int titleScreenTex, BufferedImage titleScreenBI) {
        // drawTexturedQuad(titleScreenTex, 0f, 0f, (float) titleScreenBI.getWidth(),
        // (float) titleScreenBI.getHeight());
        drawImGui();

        ImGui.setNextWindowPos(winW * 0.05f, winH * 0.4f);
        ImGui.setNextWindowSize(winW / 2, 330);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 0f);
        ImGui.pushStyleColor(ImGuiCol.Button, 0f, 0f, 0f, 1f);
        ImGui.begin("Main Menu", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoDecoration);

        String[] titleScreenOptions = { "Start Game", "Options", "Exit" };
        for (int i = 0; i < titleScreenOptions.length; i++) {
            if (ImGui.button(titleScreenOptions[i], (winW / 2) - 20, 100)) {
                game.handleTitleScreenOption(i);
            }
        }

        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleColor();
        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void renderInGameUI() {
        // no in game ui right now
    }

    private void renderInGame(int winW, int winH, int backgroundTex, int gridTex) {
        // --- STEP 1: 2D BACKGROUND LAYER ---
        glDisable(GL_DEPTH_TEST);
        draw2D(backgroundTex, 0, 0, winW, winH);
        if (DRAW_DEBUG_GRID) {
            draw2D(gridTex, 0, 0, winW, winH);
        }
        flushBatch(); // Must flush 2D before starting 3D

        // --- STEP 2: 3D OBJECT LAYER ---
        // --- START 3D SECTION ---
        glEnable(GL_DEPTH_TEST);
        glClear(GL_DEPTH_BUFFER_BIT); // Clear depth buffer before rendering 3D

        // Save current projection (2D ortho)
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();

        // Setup Perspective: FOV, Aspect Ratio, Near Clip, Far Clip
        float aspect = (float) winW / (float) winH;
        setupPerspective(45.0f, aspect, 0.1f, 1000.0f);

        // Switch to modelview and set up camera
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        
        // IMPORTANT: Move the camera back so we aren't inside the model
        // In perspective, negative Z moves objects AWAY from you
        glTranslatef(0, 0, -25.0f);

        // Render your VBO objects here
        for (Object3D obj : loaded3DObjects) {
            render3DObject(obj);
        }

        glPopMatrix(); // Restore modelview
        
        // Restore 2D projection
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        // --- END 3D SECTION ---

        // --- STEP 3: 2D ENTITY LAYER (Sprites) ---
        // 1. Update Animations (Logic)
        for (Entity e : game.getEntities()) {
            if (!e.getType().equals("door")) {
                e.getEntityAnimation().tick(e.getCurrentAnimationState());
            }
        }

        // 2. Batch Draw (Rendering)
        // draw2D(backgroundTex, 0, 0, winW, winH);
        glDisable(GL_DEPTH_TEST);
        for (Entity e : game.getEntities()) {
            // Just grab the ID that tick() already updated
            draw2D(e.getTextureId(), e.getX(), e.getY(), e.getWidth(), e.getHeight());
        }

        flushBatch(); // Draw all sprites in one command

        // --- STEP 4: UI LAYER (ImGui) ---
        if (game.messageBoxDisplayed) {
            renderMessageBox(winW, winH);
        } else {
            renderInGameUI();
        }
    }

    private void renderMessageBox(int winW, int winH) {
        drawImGui();
        ImGui.setNextWindowPos(winW / 2 - 300, winH - 250);
        ImGui.setNextWindowSize(600, 200);
        ImGui.begin(game.currentNPC.getName(),
                ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);

        ImGui.textWrapped(game.currentFullMessage);
        ImGui.separator();

        try {
            if (!game.messageBoxOptionsDisplayed) {
                if (ImGui.button("Close (E)", 100, 30)) {
                    game.closeMessage();
                }
            } else if (game.currentResponseOptions != null) {
                for (int i = 0; i < game.currentResponseOptions.length; i++) {
                    if (ImGui.button(game.currentResponseOptions[i], 500, 30)) {
                        game.selectedResponseIndex = i;
                        game.dialogueHandler(game.currentTree, i + 1, game.currentNPC);
                    }
                }
            } else {
                if (ImGui.button("Close (E)", 100, 30)) {
                    game.closeMessage();
                }
            }
        } catch (Exception e) {
            if (ImGui.button("Close (E)", 100, 30)) {
                game.closeMessage();
            }
        }

        ImGui.end();
        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void renderOptions(int winW, int winH, int titleScreenTex, BufferedImage titleScreenBI) {
        drawImGui();
        ImGui.setNextWindowPos(winW / 2 - 150, winH / 2 - 100);
        ImGui.setNextWindowSize(300, 330);
        ImGui.begin("Options", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);

        ImGui.dragFloat("Volume", game.optionsVolume, 1f, 0.0f, 100.0f, "%.0f");
        if (ImGui.button("Test Volume", 200, 40)) {
            game.playTalkingSound();
        }

        ImGui.dragFloat("Framerate", game.optionsFrameRate, 1f, 1.0f, 60.0f, "%.0f");
        ImGui.dragFloat("Update Rate", game.optionsUpdateRate, 1f, 1.0f, 60.0f, "%.0f");
        if (ImGui.checkbox("Fullscreen", game.isFullscreen)) {
            game.fstoggle.setFullscreen(game.isFullscreen.get());
            System.out.println(game.isFullscreen.get());
        }
        if (ImGui.checkbox("Draw Debug Grid", game.shouldDebugGridBeDrawn)) {
            DRAW_DEBUG_GRID = game.shouldDebugGridBeDrawn.get();
            game.DRAW_DEBUG_GRID = game.shouldDebugGridBeDrawn.get();
        }

        if (ImGui.button("Back", 200, 40)) {
            game.setCurrentGameState(lastStateForOptionsMenu);
        }

        game.soundPlayerVolume = (int) game.optionsVolume[0];
        game.FRAMERATE = (int) game.optionsFrameRate[0];
        game.UPDATE_RATE = (int) game.optionsUpdateRate[0];

        ImGui.end();
        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void renderPauseMenu(int winW, int winH) {
        drawImGui();
        ImGui.setNextWindowPos(winW / 2 - 150, winH / 2 - 100);
        ImGui.setNextWindowSize(300, 400);
        ImGui.begin("Paused", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);

        ImGui.text("Game Paused");
        ImGui.separator();

        if (ImGui.button("Resume", 200, 40)) {
            game.setCurrentGameState(main.GameState.IN_GAME);
        }
        if (ImGui.button("Save", 200, 40)) {
            game.saveCurrentGame(game, "quicksave");
        }
        if (ImGui.button("Load", 200, 40)) {
            game.loadGameFromFile(game, "quicksave");
        }
        if (ImGui.button("Options", 200, 40)) {
            lastStateForOptionsMenu = GameState.PAUSED;
            game.setCurrentGameState(main.GameState.OPTIONS);
        }
        if (ImGui.button("To Title Screen", 200, 40)) {
            game.setCurrentGameState(main.GameState.TITLE_SCREEN);
        }

        ImGui.end();
        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void drawImGui() {
        imguiGlfw.newFrame();
        imguiGl3.newFrame();
        ImGui.newFrame();
    }

    private int lastBoundTex = -1;

    public void drawTexturedQuad(int texId, float x, float y, float w, float h) {
        // Only bind if the texture actually changed
        if (texId != lastBoundTex) {
            glBindTexture(GL_TEXTURE_2D, texId);
            lastBoundTex = texId;
        }

        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2f(x, y);
        glTexCoord2f(1, 0);
        glVertex2f(x + w, y);
        glTexCoord2f(1, 1);
        glVertex2f(x + w, y + h);
        glTexCoord2f(0, 1);
        glVertex2f(x, y + h);
        glEnd();
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

    // ========== 3D OBJECT METHODS ==========

    private void setupPerspective(float fov, float aspect, float zNear, float zFar) {
        float fh = (float) Math.tan(Math.toRadians(fov / 2.0f)) * zNear;
        float fw = fh * aspect;
        glFrustum(-fw, fw, -fh, fh, zNear, zFar);
    }

    /**
     * Loads a 3D object from an OBJ file
     * Parses vertices, texture coordinates, and faces
     * 
     * @param filePath   Path to the .obj file
     * @param textureId  OpenGL texture ID to apply to the object
     * @param objectName Name identifier for the object
     * @return Object3D instance, or null if loading fails
     */
    public Object3D load3DObject(String filePath, int textureId, String objectName) {
        List<float[]> tempVertices = new ArrayList<>();
        List<float[]> tempTexCoords = new ArrayList<>();
        List<Float> packedData = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("\\s+");
                if (parts[0].equals("v")) {
                    tempVertices.add(new float[] { Float.parseFloat(parts[1]), Float.parseFloat(parts[2]),
                            Float.parseFloat(parts[3]) });
                } else if (parts[0].equals("vt")) {
                    tempTexCoords.add(new float[] { Float.parseFloat(parts[1]), 1.0f - Float.parseFloat(parts[2]) });
                } // Inside load3DObject, replace the 'f' parsing block:
                else if (parts[0].equals("f")) {
                    // Standard OBJ faces can have 3, 4, or more vertices.
                    // We must convert them to triangles (1-2-3, 1-3-4, 1-4-5...)
                    for (int i = 2; i < parts.length - 1; i++) {
                        packVertex(parts[1], tempVertices, tempTexCoords, packedData);
                        packVertex(parts[i], tempVertices, tempTexCoords, packedData);
                        packVertex(parts[i + 1], tempVertices, tempTexCoords, packedData);
                    }
                }
            }

            float[] vertexData = new float[packedData.size()];
            for (int i = 0; i < packedData.size(); i++) {
                vertexData[i] = packedData.get(i);
            }

            Object3D obj = new Object3D(objectName, vertexData.length / 5);
            obj.textureId = textureId;

            // --- UPLOAD TO GPU VBO ---
            FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexData.length);
            buffer.put(vertexData).flip();

            obj.vboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, obj.vboId);
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            loaded3DObjects.add(obj);
            return obj;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Helper to resolve OBJ indices and pack them into our [X,Y,Z,U,V] format
     */
    private void packVertex(String vertexPart, List<float[]> vList, List<float[]> vtList, List<Float> target) {
        String[] subParts = vertexPart.split("/");

        // OBJ indices are 1-based
        int vIdx = Integer.parseInt(subParts[0]) - 1;
        float[] pos = vList.get(vIdx);

        target.add(pos[0]); // X
        target.add(pos[1]); // Y
        target.add(pos[2]); // Z

        if (subParts.length > 1 && !subParts[1].isEmpty()) {
            int vtIdx = Integer.parseInt(subParts[1]) - 1;
            float[] tex = vtList.get(vtIdx);
            target.add(tex[0]); // U
            target.add(tex[1]); // V
        } else {
            target.add(0f);
            target.add(0f);
        }
    }

    /**
     * Renders a 3D object by using vertex arrays with VBO
     * 
     * @param obj The Object3D to render
     */
    private void render3DObject(Object3D obj) {
        if (obj.vboId == -1)
            return;

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, obj.textureId);

        // Disable culling to render both sides of faces
        glDisable(GL_CULL_FACE);

        glPushMatrix();
        glTranslatef(obj.x, obj.y, obj.z);
        glRotatef(obj.rotationX, 1, 0, 0);
        glRotatef(obj.rotationY, 0, 1, 0);
        glRotatef(obj.rotationZ, 0, 0, 1);
        glScalef(obj.scaleX, obj.scaleY, obj.scaleZ);

        glBindBuffer(GL_ARRAY_BUFFER, obj.vboId);

        // STRIDE: 5 floats * 4 bytes = 20 bytes per vertex
        int stride = 20;

        glEnableClientState(GL_VERTEX_ARRAY);
        // 3 components (X,Y,Z), offset 0
        glVertexPointer(3, GL_FLOAT, stride, 0);

        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        // 2 components (U,V), offset 12 bytes (3 floats * 4 bytes)
        glTexCoordPointer(2, GL_FLOAT, stride, 12);

        glDrawArrays(GL_TRIANGLES, 0, obj.vertexCount);

        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glPopMatrix();
    }

    /**
     * Helper method to apply rotation transformations in 3D space
     * 
     * @return Rotated [x, y, z] coordinates
     */
    private float[] applyRotations(float x, float y, float z, float rotZ, float rotY, float rotX) {
        // Rotation around Z axis
        float cosZ = (float) Math.cos(Math.toRadians(rotZ));
        float sinZ = (float) Math.sin(Math.toRadians(rotZ));
        float x1 = x * cosZ - y * sinZ;
        float y1 = x * sinZ + y * cosZ;
        float z1 = z;

        // Rotation around Y axis
        float cosY = (float) Math.cos(Math.toRadians(rotY));
        float sinY = (float) Math.sin(Math.toRadians(rotY));
        float x2 = x1 * cosY + z1 * sinY;
        float y2 = y1;
        float z2 = -x1 * sinY + z1 * cosY;

        // Rotation around X axis
        float cosX = (float) Math.cos(Math.toRadians(rotX));
        float sinX = (float) Math.sin(Math.toRadians(rotX));
        float x3 = x2;
        float y3 = y2 * cosX - z2 * sinX;
        float z3 = y2 * sinX + z2 * cosX;

        return new float[] { x3, y3, z3 };
    }

    /**
     * Move a 3D object to a new position
     * 
     * @param obj The Object3D to move
     * @param x   New X coordinate
     * @param y   New Y coordinate
     * @param z   New Z coordinate
     */
    public void move3DObject(Object3D obj, float x, float y, float z) {
        obj.x = x;
        obj.y = y;
        obj.z = z;
    }

    /**
     * Set the scale of a 3D object
     * 
     * @param obj    The Object3D to scale
     * @param scaleX Scale factor for X axis
     * @param scaleY Scale factor for Y axis
     * @param scaleZ Scale factor for Z axis
     */
    public void scale3DObject(Object3D obj, float scaleX, float scaleY, float scaleZ) {
        obj.scaleX = scaleX;
        obj.scaleY = scaleY;
        obj.scaleZ = scaleZ;
    }

    /**
     * Rotate a 3D object
     * 
     * @param obj  The Object3D to rotate
     * @param rotX Rotation around X axis (degrees)
     * @param rotY Rotation around Y axis (degrees)
     * @param rotZ Rotation around Z axis (degrees)
     */
    public void rotate3DObject(Object3D obj, float rotX, float rotY, float rotZ) {
        obj.rotationX = rotX;
        obj.rotationY = rotY;
        obj.rotationZ = rotZ;
    }

    /**
     * Get a loaded 3D object by name
     * 
     * @param objectName The name of the object
     * @return The Object3D, or null if not found
     */
    public Object3D get3DObject(String objectName) {
        for (Object3D obj : loaded3DObjects) {
            if (obj.name.equals(objectName)) {
                return obj;
            }
        }
        return null;
    }

    /**
     * Remove a 3D object from the scene
     * 
     * @param objectName The name of the object to remove
     */
    public void remove3DObject(String objectName) {
        for (int i = 0; i < loaded3DObjects.size(); i++) {
            Object3D obj = loaded3DObjects.get(i);
            if (obj.name.equals(objectName)) {
                loaded3DObjects.remove(i);
                return;
            }
        }
    }
}