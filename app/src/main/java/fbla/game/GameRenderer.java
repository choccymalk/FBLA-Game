package fbla.game;

import static org.lwjgl.opengl.GL11.*;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;

import fbla.game.main.GameState;
import fbla.game.Renderer.Renderer2D;
import fbla.game.Renderer.Renderer3D;
import fbla.game.Renderer.Object3D;
import fbla.game.UI.TitleScreen;
import fbla.game.UI.InGame;
import fbla.game.UI.PauseMenu;
import fbla.game.UI.OptionsMenu;
import fbla.game.UI.MessageBox;

public class GameRenderer {
    private final main game;
    private final ImGuiImplGlfw imguiGlfw;
    private final ImGuiImplGl3 imguiGl3;

    // Component renderers
    private final Renderer2D renderer2D;
    private final Renderer3D renderer3d;

    // UI components
    private final TitleScreen titleScreen;
    private final InGame inGameUI;
    private final PauseMenu pauseMenu;
    private final OptionsMenu optionsMenu;
    private final MessageBox messageBox;

    private boolean DRAW_DEBUG_GRID;
    private GameState lastStateForOptionsMenu = GameState.TITLE_SCREEN;

    public GameRenderer(main game, ImGuiImplGlfw imguiGlfw, ImGuiImplGl3 imguiGl3) {
        this.game = game;
        this.imguiGlfw = imguiGlfw;
        this.imguiGl3 = imguiGl3;

        // Initialize component renderers
        this.renderer2D = new Renderer2D();
        this.renderer3d = game.getRenderer3d();

        // Initialize UI components
        this.titleScreen = new TitleScreen(game, imguiGl3);
        this.inGameUI = new InGame(game);
        this.pauseMenu = new PauseMenu(game, imguiGl3);
        this.optionsMenu = new OptionsMenu(game, imguiGl3);
        this.messageBox = new MessageBox(game, imguiGl3);
    }

    public void render(int winW, int winH, BufferedImage backgroundBI, int backgroundTex, BufferedImage playerBI,
            BufferedImage gridBI, int gridTex, BufferedImage titleScreenBI, int titleScreenTex,
            BufferedImage titleScreenGameLogoBI, int titleScreenGameLogoTex) {
        // 1. Reset State
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        renderer2D.resetTextureTracking(); // Reset texture tracking for the new frame

        // 2. Setup Ortho Projection (2D)
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, winW, winH, 0, -1, 1);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        switch (game.getCurrentGameState()) {
            case TITLE_SCREEN:
                renderer2D.draw2D(titleScreenTex, 0, 0, winW, winH);
                renderer2D.flushBatch(); // Finish 2D before ImGui
                drawImGui();
                titleScreen.render(winW, winH);
                break;
            case IN_GAME:
                renderInGame(winW, winH, backgroundTex, gridTex);
                if (game.messageBoxDisplayed) {
                    drawImGui();
                    messageBox.render(winW, winH);
                }
                break;
            case PAUSED:
                drawImGui();
                pauseMenu.render(winW, winH, lastStateForOptionsMenu);
                break;
            case OPTIONS:
                renderer2D.draw2D(titleScreenTex, 0, 0, winW, winH);
                renderer2D.flushBatch(); // Finish 2D before ImGui
                drawImGui();
                boolean[] debugGridArray = { DRAW_DEBUG_GRID };
                optionsMenu.render(winW, winH, lastStateForOptionsMenu, debugGridArray);
                DRAW_DEBUG_GRID = debugGridArray[0];
                break;
        }
    }

    private void renderInGame(int winW, int winH, int backgroundTex, int gridTex) {
        // --- STEP 1: 2D BACKGROUND LAYER ---
        glDisable(GL_DEPTH_TEST);

        if (backgroundTex != -1) {
            glColor4f(1, 1, 1, 1); // Reset color to white for textures
            renderer2D.draw2D(backgroundTex, 0, 0, winW, winH);
            renderer2D.flushBatch();
        }
        if (DRAW_DEBUG_GRID) {
            renderer2D.draw2D(gridTex, 0, 0, winW, winH);
        }
        // renderer2D.flushBatch(); // Must flush 2D before starting 3D

        // --- STEP 2: 2D ENTITY LAYER (Sprites) ---
        // 1. Update Animations (Logic)
        for (Entity e : game.getEntities()) {
            if (!e.getType().equals("door")) {
                e.getEntityAnimation().tick(e.getCurrentAnimationState());
            }
        }

        // 2. Batch Draw (Rendering)
        for (Entity e : game.getEntities()) {
            renderer2D.draw2D(e.getTextureId(), e.getX(), e.getY(), e.getWidth(), e.getHeight());
        }

        renderer2D.flushBatch(); // Draw all sprites in one command

        // --- STEP 3: 3D LAYER ---
        draw3DLayer();

        // --- STEP 4: UI LAYER ---
        if (!game.messageBoxDisplayed) {
            inGameUI.render();
        }
    }

    private void drawImGui() {
        imguiGlfw.newFrame();
        imguiGl3.newFrame();
        ImGui.newFrame();
    }

    private void draw3DLayer() {
        glEnable(GL_DEPTH_TEST);
        glClear(GL_DEPTH_BUFFER_BIT); // Clear depth buffer before rendering 3D

        // Save current projection (2D ortho)
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();

        // Setup Perspective: FOV, Aspect Ratio, Near Clip, Far Clip
        float aspect = (float) 1280 / (float) 720;
        renderer3d.setupPerspective(45.0f, aspect, 0.1f, 1000.0f);

        // Switch to modelview and set up camera
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        // --- CAMERA LOGIC START ---
        // We rotate and translate the WORLD in the opposite direction of the camera.
        // Order matters: Rotate first, then Translate (First Person view).

        glRotatef(-0, 1.0f, 0.0f, 0.0f); // Pitch
        glRotatef(-0, 0.0f, 1.0f, 0.0f); // Yaw
        glRotatef(-0, 0.0f, 0.0f, 1.0f); // Roll
        // camera pos x, camera pos y, camera pos z. all must be negative
        glTranslatef(-0, -0, -10);

        // Render your VBO objects here
        for (Object3D obj3d : renderer3d.getLoaded3DObjects()) {
            // this checks to see if the object has the same level index as the current loaded level index
            // not robust because if we wanted to load an object on the fly, the object we load might not
            // have the same level index as the level we are on
            /*if (obj3d.getLevelIndex() == game.getCurrentLevelIndex()) {
                renderer3d.render3DObject(obj3d);
            }*/

            // this is more robust, it checks to see if the level contains the name of the object, we can easily
            // load a new object by adding the name of the object to the level's list
            if(game.getLevels().get(game.getCurrentLevelIndex()).getObject3DList().contains(obj3d.getName())){
                renderer3d.render3DObject(obj3d);
            }
        }

        glPopMatrix(); // Restore modelview

        // Restore 2D projection
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
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

    // ========== 3D OBJECT DELEGATION METHODS ==========

    /**
     * Loads a 3D object from an OBJ file
     * Delegates to Renderer3D
     */
    public Object3D load3DObject(String filePath, int textureId, String objectName) {
        return renderer3d.load3DObject(filePath, textureId, objectName);
    }

    /**
     * Move a 3D object to a new position
     * Delegates to Renderer3D
     */
    public void move3DObject(Object3D obj, float x, float y, float z) {
        renderer3d.move3DObject(obj, x, y, z);
    }

    /**
     * Set the scale of a 3D object
     * Delegates to Renderer3D
     */
    public void scale3DObject(Object3D obj, float scaleX, float scaleY, float scaleZ) {
        renderer3d.scale3DObject(obj, scaleX, scaleY, scaleZ);
    }

    /**
     * Rotate a 3D object
     * Delegates to Renderer3D
     */
    public void rotate3DObject(Object3D obj, float rotX, float rotY, float rotZ) {
        renderer3d.rotate3DObject(obj, rotX, rotY, rotZ);
    }

    /**
     * Get a loaded 3D object by name
     * Delegates to Renderer3D
     */
    public Object3D get3DObject(String objectName) {
        return renderer3d.get3DObject(objectName);
    }

    /**
     * Remove a 3D object from the scene
     * Delegates to Renderer3D
     */
    public void remove3DObject(String objectName) {
        renderer3d.remove3DObject(objectName);
    }

    // ========== GETTER/SETTER METHODS ==========

    public void setLastStateForOptionsMenu(GameState state) {
        this.lastStateForOptionsMenu = state;
    }

    public GameState getLastStateForOptionsMenu() {
        return this.lastStateForOptionsMenu;
    }

    public Renderer2D getRenderer2D() {
        return renderer2D;
    }

    public Renderer3D getRenderer3D() {
        return renderer3d;
    }
}