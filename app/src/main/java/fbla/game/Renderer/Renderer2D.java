package fbla.game.Renderer;

import static org.lwjgl.opengl.GL11.*;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

public class Renderer2D {
    private static final int MAX_QUADS = 1000; // Max sprites per draw call
    private static final int VERTICES_PER_QUAD = 4;
    private static final int FLOATS_PER_VERTEX = 4; // x, y, u, v
    private final FloatBuffer batchBuffer = BufferUtils
            .createFloatBuffer(MAX_QUADS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX);

    private int currentBatchTex = -1;
    private int quadCount = 0;
    private int lastBoundTex = -1;

    /**
     * Draw a 2D textured quad immediately (not batched)
     * 
     * @param texId Texture ID
     * @param x     X position
     * @param y     Y position
     * @param w     Width
     * @param h     Height
     */
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

    /**
     * Add a 2D quad to the batch
     * 
     * @param texId Texture ID
     * @param x     X position
     * @param y     Y position
     * @param w     Width
     * @param h     Height
     */
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

    /**
     * Flush all batched quads to the GPU
     */
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
    
    /**
     * Reset texture tracking
     */
    public void resetTextureTracking() {
        lastBoundTex = -1;
    }
}