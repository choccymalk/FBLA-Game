package fbla.game;

import static org.lwjgl.opengl.GL11.*;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class GameRenderer {
    private final main game;
    private final ImGuiImplGlfw imguiGlfw;
    private final ImGuiImplGl3 imguiGl3;
    
    private static final int GRID_CELL_SIZE = 24;
    private static final boolean DRAW_DEBUG_GRID = false;
    private static final int DOOR_HEIGHT = 144;
    private static final int DOOR_WIDTH = 96;
    
    private int inGameFrameCount = 0;
    
    // 3D Object storage
    public class Object3D {
        public String name;
        public List<float[]> vertices;
        public List<int[]> faces;
        public int textureId;
        public float x, y, z;
        public float scaleX = 1f, scaleY = 1f, scaleZ = 1f;
        public float rotationX = 0f, rotationY = 0f, rotationZ = 0f;
        
        public Object3D(String name) {
            this.name = name;
            this.vertices = new ArrayList<>();
            this.faces = new ArrayList<>();
            this.x = 0;
            this.y = 0;
            this.z = 0;
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
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, winW, winH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        switch (game.getCurrentGameState()) {
            case TITLE_SCREEN:
                renderTitleScreen(winW, winH, titleScreenTex, titleScreenBI);
                break;
            case IN_GAME:
                renderInGame(winW, winH, backgroundTex, backgroundBI, gridTex, gridBI, playerBI);
                if (game.messageBoxDisplayed) {
                    renderMessageBox(winW, winH);
                }
                break;
            case PAUSED:
                renderPauseMenu(winW, winH);
                break;
            case OPTIONS:
                renderOptions(winW, winH, titleScreenTex, titleScreenBI);
                break;
        }
    }

    private void renderTitleScreen(int winW, int winH, int titleScreenTex, BufferedImage titleScreenBI) {
        drawTexturedQuad(titleScreenTex, 0, 0, winW, winH, titleScreenBI.getWidth(), titleScreenBI.getHeight());
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

    private void renderInGame(int winW, int winH, int backgroundTex, BufferedImage backgroundBI, 
                              int gridTex, BufferedImage gridBI, BufferedImage playerBI) {
        inGameFrameCount++;
        drawTexturedQuad(backgroundTex, 0, 0, winW, winH, backgroundBI.getWidth(), backgroundBI.getHeight());

        if (DRAW_DEBUG_GRID) {
            drawTexturedQuad(gridTex, 0, 0, winW, winH, gridBI.getWidth(), gridBI.getHeight());
        }

        // Render 3D objects
        for (Object3D obj : loaded3DObjects) {
            render3DObjectFlat(obj);
        }

        List<Entity> entities = game.getEntities();
        for (Entity e : entities) {
            drawTexturedQuad(e.getTextureId(), e.getX(), e.getY(), e.getWidth(), e.getHeight(),
                    e.getWidth(), e.getHeight());
        }

        if (!game.messageBoxDisplayed) {
            drawImGui();
            ImGui.render();
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
        drawTexturedQuad(titleScreenTex, 0, 0, winW, winH, titleScreenBI.getWidth(), titleScreenBI.getHeight());
        drawImGui();
        ImGui.setNextWindowPos(winW / 2 - 150, winH / 2 - 100);
        ImGui.setNextWindowSize(300, 330);
        ImGui.begin("Options", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);

        ImGui.dragFloat("Volume", game.optionsVolume, 1f, 0.0f, 100.0f, "%.3f");
        if (ImGui.button("Test Volume", 200, 40)) {
            game.playTalkingSound();
        }

        ImGui.dragFloat("Framerate", game.optionsFrameRate, 1f, 1.0f, 60.0f, "%.3f");
        if (ImGui.checkbox("Fullscreen", game.isFullscreen)) {
            game.fstoggle.setFullscreen(game.isFullscreen.get());
            System.out.println(game.isFullscreen.get());
        }

        if (ImGui.button("Back", 200, 40)) {
            game.setCurrentGameState(main.GameState.TITLE_SCREEN);
        }

        game.soundPlayerVolume = (int) game.optionsVolume[0];
        game.FRAMERATE = (int) game.optionsFrameRate[0];

        ImGui.end();
        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void renderPauseMenu(int winW, int winH) {
        drawImGui();
        ImGui.setNextWindowPos(winW / 2 - 150, winH / 2 - 100);
        ImGui.setNextWindowSize(300, 200);
        ImGui.begin("Paused", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);

        ImGui.text("Game Paused");
        ImGui.separator();

        if (ImGui.button("Resume", 200, 40)) {
            game.setCurrentGameState(main.GameState.IN_GAME);
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

    // ========== 3D OBJECT METHODS ==========

    /**
     * Loads a 3D object from an OBJ file
     * @param filePath Path to the .obj file
     * @param textureId OpenGL texture ID to apply to the object
     * @param objectName Name identifier for the object
     * @return Object3D instance, or null if loading fails
     */
    public Object3D load3DObject(String filePath, int textureId, String objectName) {
        Object3D obj = new Object3D(objectName);
        obj.textureId = textureId;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                String[] parts = line.split("\\s+");
                if (parts.length == 0) continue;
                
                // Parse vertices
                if (parts[0].equals("v") && parts.length >= 4) {
                    float[] vertex = new float[3];
                    vertex[0] = Float.parseFloat(parts[1]);
                    vertex[1] = Float.parseFloat(parts[2]);
                    vertex[2] = Float.parseFloat(parts[3]);
                    obj.vertices.add(vertex);
                }
                // Parse faces
                else if (parts[0].equals("f") && parts.length >= 4) {
                    int[] face = new int[parts.length - 1];
                    for (int i = 1; i < parts.length; i++) {
                        String vertexData = parts[i].split("/")[0];
                        face[i - 1] = Integer.parseInt(vertexData) - 1;
                    }
                    obj.faces.add(face);
                }
            }
            
            loaded3DObjects.add(obj);
            System.out.println("Loaded 3D object: " + objectName + " with " + obj.vertices.size() + " vertices");
            return obj;
        } catch (IOException e) {
            System.err.println("Failed to load 3D object from " + filePath + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Renders a 3D object by projecting its faces to 2D screen space
     * Each face is rendered as a textured triangle/quad
     * @param obj The Object3D to render
     */
    private void render3DObjectFlat(Object3D obj) {
        if (obj.vertices.isEmpty() || obj.faces.isEmpty()) return;
        
        glBindTexture(GL_TEXTURE_2D, obj.textureId);
        glPushMatrix();
        
        // Project and transform all vertices
        float[][] projectedVertices = new float[obj.vertices.size()][2];
        
        for (int i = 0; i < obj.vertices.size(); i++) {
            float[] vertex = obj.vertices.get(i);
            
            // Apply scaling
            float vx = vertex[0] * obj.scaleX;
            float vy = vertex[1] * obj.scaleY;
            float vz = vertex[2] * obj.scaleZ;
            
            // Apply rotations (Z, then Y, then X)
            float[] rotated = applyRotations(vx, vy, vz, obj.rotationZ, obj.rotationY, obj.rotationX);
            
            // Apply simple perspective (optional, can be disabled)
            float perspective = 1.0f + (rotated[2] * 0.001f);
            
            // Project to 2D and translate to world position
            projectedVertices[i][0] = obj.x + (rotated[0] / perspective);
            projectedVertices[i][1] = obj.y + (rotated[1] / perspective);
        }
        
        // Render each face
        glBegin(GL_TRIANGLES);
        for (int[] face : obj.faces) {
            if (face.length == 3) {
                // Triangle face
                renderFace(projectedVertices, face);
            } else if (face.length == 4) {
                // Quad face - split into two triangles
                int[] tri1 = {face[0], face[1], face[2]};
                int[] tri2 = {face[0], face[2], face[3]};
                renderFace(projectedVertices, tri1);
                renderFace(projectedVertices, tri2);
            } else if (face.length > 4) {
                // Polygon with more than 4 vertices - fan triangulation
                for (int i = 1; i < face.length - 1; i++) {
                    int[] tri = {face[0], face[i], face[i + 1]};
                    renderFace(projectedVertices, tri);
                }
            }
        }
        glEnd();
        
        glPopMatrix();
    }
    
    /**
     * Helper method to apply rotation transformations in 3D space
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
        
        return new float[]{x3, y3, z3};
    }
    
    /**
     * Renders a single triangular face with texture coordinates
     * @param projectedVertices Array of projected 2D vertices
     * @param face Array of vertex indices for this face
     */
    private void renderFace(float[][] projectedVertices, int[] face) {
        for (int i = 0; i < face.length; i++) {
            int vertexIndex = face[i];
            if (vertexIndex < 0 || vertexIndex >= projectedVertices.length) continue;
            
            // Simple texture mapping: map vertex position to texture coordinates
            float texU = (projectedVertices[vertexIndex][0] % 1.0f + 1.0f) % 1.0f;
            float texV = (projectedVertices[vertexIndex][1] % 1.0f + 1.0f) % 1.0f;
            
            glTexCoord2f(texU, texV);
            glVertex2f(projectedVertices[vertexIndex][0], projectedVertices[vertexIndex][1]);
        }
    }

    /**
     * Move a 3D object to a new position
     * @param obj The Object3D to move
     * @param x New X coordinate
     * @param y New Y coordinate
     * @param z New Z coordinate
     */
    public void move3DObject(Object3D obj, float x, float y, float z) {
        obj.x = x;
        obj.y = y;
        obj.z = z;
    }

    /**
     * Set the scale of a 3D object
     * @param obj The Object3D to scale
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
     * @param obj The Object3D to rotate
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
     * @param objectName The name of the object to remove
     */
    public void remove3DObject(String objectName) {
        loaded3DObjects.removeIf(obj -> obj.name.equals(objectName));
    }
}