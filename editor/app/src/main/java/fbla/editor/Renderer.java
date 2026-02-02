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

    public Renderer(LevelEditor editor) {
        this.editor = editor;
    }

    public int getBackgroundImageTextureId() {
        return this.backgroundImageTextureId;
    }

    public void setBackgroundImageTextureId(int id) {
        this.backgroundImageTextureId = id;
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

        // Update drag preview position
        editor.updateDragPreview();

        // Render canvas (OpenGL)
        renderCanvas();

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
        int canvasX = 320;
        int canvasY = 20;
        int canvasW = LevelEditor.GRID_WIDTH * LevelEditor.GRID_CELL_SIZE;
        int canvasH = LevelEditor.GRID_HEIGHT * LevelEditor.GRID_CELL_SIZE;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Draw background image if loaded
        if (backgroundImageTextureId != -1) {
            draw2D(backgroundImageTextureId, canvasX, canvasY, canvasW, canvasH);
            flushBatch();
        }

        // Draw collision grid
        if (editor.getShowCollisionGrid()) {
            glColor4f(1, 0, 0, 0.3f);
            glBegin(GL_QUADS);
            for (int y = 0; y < LevelEditor.GRID_HEIGHT; y++) {
                for (int x = 0; x < LevelEditor.GRID_WIDTH; x++) {
                    if (editor.getCollisionGrid()[y][x] == 1) {
                        int px = canvasX + x * LevelEditor.GRID_CELL_SIZE;
                        ;
                        int py = canvasY + y * LevelEditor.GRID_CELL_SIZE;
                        ;
                        glVertex2f(px, py);
                        glVertex2f(px + LevelEditor.GRID_CELL_SIZE, py);
                        glVertex2f(px + LevelEditor.GRID_CELL_SIZE, py + LevelEditor.GRID_CELL_SIZE);
                        glVertex2f(px, py + LevelEditor.GRID_CELL_SIZE);
                    }
                }
            }
            glEnd();
        }

        // Draw grid lines
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

        // Draw doors
        if (editor.getShowDoors()) {
            for (int i = 0; i < editor.getDoors().size(); i++) {
                Door d = editor.getDoors().get(i);
                int px = canvasX + d.getX();
                int py = canvasY + d.getY();
                drawDoor(px, py);
            }
        }

        // Draw entities
        if (editor.getShowEntities()) {
            for (int i = 0; i < editor.getEntities().size(); i++) {
                Entity e = editor.getEntities().get(i);
                if (!e.getType().equals("door")) {
                    int px = canvasX + e.getX();
                    int py = canvasY + e.getY();
                    drawEntity(px, py, e);
                    if (e.getName() != null && !editor.getIsDraggingEntity() && !editor.getIsDraggingDoor()) {
                        drawNameOverEntity(e.getName(), px, py);
                    }
                }
            }
        }

        // Draw drag preview
        if (editor.getIsDraggingEntity() && editor.getDragPreviewGridX() >= 0 && editor.getDragPreviewGridY() >= 0) {
            int px = canvasX + editor.getDragPreviewGridX() * LevelEditor.GRID_CELL_SIZE;
            int py = canvasY + editor.getDragPreviewGridY() * LevelEditor.GRID_CELL_SIZE;
            // glColor4f(0, 1, 0, 0.5f);
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
            // glColor4f(1, 1, 0, 0.5f);
            glBegin(GL_QUADS);
            glVertex2f(px, py);
            glVertex2f(px + 48, py);
            glVertex2f(px + 48, py + 72);
            glVertex2f(px, py + 72);
            glEnd();
        }

    }

    private void drawEntity(int x, int y, Entity e) {
        // TODO: draw entity sprite instead of circle, level 3
        // Entity.java, level 3
        // Draw a circle for entities
        glColor4f(0, 1, 0, 0.3f);
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
        glColor4f(1, 1, 0, 0.3f);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + 48, y);
        glVertex2f(x + 48, y + 72);
        glVertex2f(x, y + 72);
        glEnd();
    }

    private void drawNameOverEntity(String name, int entityXPos, int entityYPos) {
        // each character in the fontmap is 8 px wide and 11 px tall
        int i = 0;
        for (char letter : name.toCharArray()) {
            switch (letter) {
                case 'a':
                    draw2D(fontMap.get("latin_small_letter_a"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'b':
                    draw2D(fontMap.get("latin_small_letter_b"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'c':
                    draw2D(fontMap.get("latin_small_letter_c"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'd':
                    draw2D(fontMap.get("latin_small_letter_d"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'e':
                    draw2D(fontMap.get("latin_small_letter_e"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'f':
                    draw2D(fontMap.get("latin_small_letter_f"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'g':
                    draw2D(fontMap.get("latin_small_letter_g"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'h':
                    draw2D(fontMap.get("latin_small_letter_h"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'i':
                    draw2D(fontMap.get("latin_small_letter_i"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'j':
                    draw2D(fontMap.get("latin_small_letter_j"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'k':
                    draw2D(fontMap.get("latin_small_letter_k"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'l':
                    draw2D(fontMap.get("latin_small_letter_l"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'm':
                    draw2D(fontMap.get("latin_small_letter_m"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'n':
                    draw2D(fontMap.get("latin_small_letter_n"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'o':
                    draw2D(fontMap.get("latin_small_letter_o"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'p':
                    draw2D(fontMap.get("latin_small_letter_p"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'q':
                    draw2D(fontMap.get("latin_small_letter_q"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'r':
                    draw2D(fontMap.get("latin_small_letter_r"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 's':
                    draw2D(fontMap.get("latin_small_letter_s"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 't':
                    draw2D(fontMap.get("latin_small_letter_t"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'u':
                    draw2D(fontMap.get("latin_small_letter_u"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'v':
                    draw2D(fontMap.get("latin_small_letter_v"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'w':
                    draw2D(fontMap.get("latin_small_letter_w"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'x':
                    draw2D(fontMap.get("latin_small_letter_x"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'y':
                    draw2D(fontMap.get("latin_small_letter_y"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'z':
                    draw2D(fontMap.get("latin_small_letter_z"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'A':
                    draw2D(fontMap.get("latin_capital_letter_a"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'B':
                    draw2D(fontMap.get("latin_capital_letter_b"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'C':
                    draw2D(fontMap.get("latin_capital_letter_c"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'D':
                    draw2D(fontMap.get("latin_capital_letter_d"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'E':
                    draw2D(fontMap.get("latin_capital_letter_e"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'F':
                    draw2D(fontMap.get("latin_capital_letter_f"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'G':
                    draw2D(fontMap.get("latin_capital_letter_g"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'H':
                    draw2D(fontMap.get("latin_capital_letter_h"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'I':
                    draw2D(fontMap.get("latin_capital_letter_i"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'J':
                    draw2D(fontMap.get("latin_capital_letter_j"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'K':
                    draw2D(fontMap.get("latin_capital_letter_k"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'L':
                    draw2D(fontMap.get("latin_capital_letter_l"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'M':
                    draw2D(fontMap.get("latin_capital_letter_m"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'N':
                    draw2D(fontMap.get("latin_capital_letter_n"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'O':
                    draw2D(fontMap.get("latin_capital_letter_o"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'P':
                    draw2D(fontMap.get("latin_capital_letter_p"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'Q':
                    draw2D(fontMap.get("latin_capital_letter_q"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'R':
                    draw2D(fontMap.get("latin_capital_letter_r"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'S':
                    draw2D(fontMap.get("latin_capital_letter_s"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'T':
                    draw2D(fontMap.get("latin_capital_letter_t"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'U':
                    draw2D(fontMap.get("latin_capital_letter_u"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'V':
                    draw2D(fontMap.get("latin_capital_letter_v"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'W':
                    draw2D(fontMap.get("latin_capital_letter_w"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'X':
                    draw2D(fontMap.get("latin_capital_letter_x"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'Y':
                    draw2D(fontMap.get("latin_capital_letter_y"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case 'Z':
                    draw2D(fontMap.get("latin_capital_letter_z"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case '0':
                    draw2D(fontMap.get("digit_zero"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case '1':
                    draw2D(fontMap.get("digit_one"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case '2':
                    draw2D(fontMap.get("digit_two"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case '3':
                    draw2D(fontMap.get("digit_three"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case '4':
                    draw2D(fontMap.get("digit_four"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case '5':
                    draw2D(fontMap.get("digit_five"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case '6':
                    draw2D(fontMap.get("digit_six"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case '7':
                    draw2D(fontMap.get("digit_seven"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case '8':
                    draw2D(fontMap.get("digit_eight"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case '9':
                    draw2D(fontMap.get("digit_nine"), entityXPos + i, entityYPos + 14, 12, 16);
                    i += 12;
                    break;
                case ' ':
                    // Space character - just advance position without drawing
                    i += 12;
                    break;
                default:
                    // For any unsupported character, you could draw a default/placeholder
                    // or just skip it. Here we'll just advance the position.
                    i += 12;
                    break;
            }
        }
        flushBatch();
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

}
