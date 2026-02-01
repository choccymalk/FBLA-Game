package fbla.editor;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

public class entityAnimation {
    // commented out because it needs the game rendering class, which the level editor does not have
    /*private final Entity entity;
    private final Map<String, int[]> animationStates = new HashMap<>();
    private String currentState = "idle";
    
    private long lastUpdate = System.currentTimeMillis();
    private int frameIndex = 0;
    private static final int MS_PER_FRAME = 100; // Adjust for speed

    public entityAnimation(Entity e, String resourcePath, GameRenderer renderer) {
        this.entity = e;
        // Pre-load all animations for this entity
        loadState("idle", e.getAnimationStates().getIdleImagesPaths(), resourcePath, renderer);
        loadState("walkingUp", e.getAnimationStates().getWalkingUpImagesPaths(), resourcePath, renderer);
        loadState("walkingDown", e.getAnimationStates().getWalkingDownImagesPaths(), resourcePath, renderer);
        loadState("walkingLeft", e.getAnimationStates().getWalkingLeftImagesPaths(), resourcePath, renderer);
        loadState("walkingRight", e.getAnimationStates().getWalkingRightImagesPaths(), resourcePath, renderer);
    }

    private void loadState(String state, List<String> paths, String path, GameRenderer renderer) {
        if (paths == null || paths.isEmpty()) return;
        int[] textureIds = new int[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            // Use a helper to load the texture ONCE
            textureIds[i] = renderer.loadTexture(path + "\\textures\\" + paths.get(i));
        }
        animationStates.put(state, textureIds);
    }

    public void tick(String newState) {
        if (!newState.equals(currentState)) {
            currentState = newState;
            frameIndex = 0;
        }

        long now = System.currentTimeMillis();
        if (now - lastUpdate >= MS_PER_FRAME) {
            int[] frames = animationStates.get(currentState);
            if (frames != null) {
                frameIndex = (frameIndex + 1) % frames.length;
                entity.setTextureId(frames[frameIndex]);
            }
            lastUpdate = now;
        }
    }*/
   // unneeded for the editor
}