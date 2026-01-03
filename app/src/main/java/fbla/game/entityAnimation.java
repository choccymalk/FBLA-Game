package fbla.game;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

public class entityAnimation {
    Entity entity;
    String RESOURCE_PATH;
    private long lastUpdate = System.currentTimeMillis();
    private long now;
    private int index = 0; // Keep track of the current image
    public entityAnimation(Entity e, String resourcePath){
        entity = e;
        RESOURCE_PATH = resourcePath;
    }
    // update frame to be called in main game loop
    public void tick(String currentEntityState){
        List<String> imagePaths;
        if(currentEntityState.equals("idle")){
            imagePaths = entity.getAnimationStates().getIdleImagesPaths();
        } else if(currentEntityState.equals("walkingUp")){
            imagePaths = entity.getAnimationStates().getWalkingUpImagesPaths();
        } else if(currentEntityState.equals("walkingDown")){
            imagePaths = entity.getAnimationStates().getWalkingDownImagesPaths();
        } else if(currentEntityState.equals("walkingLeft")){
            imagePaths = entity.getAnimationStates().getWalkingLeftImagesPaths();
        } else if(currentEntityState.equals("walkingRight")){
            imagePaths = entity.getAnimationStates().getWalkingRightImagesPaths();
        } else {
            imagePaths = entity.getAnimationStates().getIdleImagesPaths();
        }
        now = System.currentTimeMillis();
        if (now - lastUpdate >= 24) {
            try {
                // don't spam console with player animation frames
                //if(!entity.getType().equals("player")){
                //    System.out.println("Loading animation frame: " + RESOURCE_PATH + "\\textures\\" + imagePaths.get(index % imagePaths.size()) + " for entity at (" + entity.getX() + ", " + entity.getY() + ")" + " with state " + currentEntityState + " and index " + index + " with texture ID " + entity.getTextureId() + " with type " + entity.getType());
                //}
                //System.out.println("Loading animation frame: " + RESOURCE_PATH + "\\textures\\" + imagePaths.get(index % imagePaths.size()) + " for entity at (" + entity.getX() + ", " + entity.getY() + ")" + " with state " + currentEntityState + " and index " + index + " with texture ID " + entity.getTextureId() + " with type " + entity.getType());
                entity.setTextureId(createTextureFromBufferedImage(ImageIO.read(new File(RESOURCE_PATH + "\\textures\\" + imagePaths.get(index % imagePaths.size())))));
                drawTexturedQuad(entity.getTextureId(), entity.getX(), entity.getY(), entity.getWidth(), entity.getHeight(), entity.getWidth(), entity.getHeight());
            } catch (IOException e) {
                // do nothing
                System.out.println("Failed to load animation frame: " + e.getMessage());
            }
            index++;
            lastUpdate = now; // Update lastUpdate *after* using it
        }
    }

    private int createTextureFromBufferedImage(BufferedImage img) {
        int[] pixels = new int[img.getWidth() * img.getHeight()];
        img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

        ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);

        // ARGB to RGBA
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int pixel = pixels[y * img.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                buffer.put((byte) (pixel & 0xFF));         // B
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
            }
        }
        buffer.flip();
        // crashing here when updating npc animation texture? output from jvm: 
        // FATAL ERROR in native method: Thread[#48,Thread-2,5,main]: No context is current or a function that is not available in the current context was called. The JVM will abort execution.
        // turns out that opengl calls need to be done in the main thread, not in other threads (like during npc animation updates)
        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, img.getWidth(), img.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        return texId;
    }

    private void drawTexturedQuad(int texId, int x, int y, int w, int h, int texW, int texH) {
        //System.out.println("Drawing textured quad at (" + x + ", " + y + ") with size (" + w + ", " + h + ") using texture ID " + texId);
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
}
