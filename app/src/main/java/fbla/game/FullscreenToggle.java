package fbla.game;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import java.nio.IntBuffer;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;

public class FullscreenToggle {

    private long window;
    private int[] windowPosX = new int[1];
    private int[] windowPosY = new int[1];
    private int[] windowWidth = new int[1];
    private int[] windowHeight = new int[1];

    // Constructor or initialization method would set the 'window' handle
    public FullscreenToggle(long window){
        this.window = window;
    }

    public void setFullscreen(boolean fullscreen) {
        if (isFullscreen() == fullscreen) {
            return;
        }

        if (fullscreen) {
            // Backup current window position and size
            glfwGetWindowPos(window, windowPosX, windowPosY);
            glfwGetWindowSize(window, windowWidth, windowHeight);

            // Get the primary monitor and its video mode
            long monitor = glfwGetPrimaryMonitor();
            if (monitor == 0) {
                return; // Handle error
            }
            GLFWVidMode mode = glfwGetVideoMode(monitor);
            if (mode == null) {
                return; // Handle error
            }

            // Switch to fullscreen
            glfwSetWindowMonitor(window, monitor, 0, 0, 1280, 720, mode.refreshRate());
        } else {
            // Restore to windowed mode using the backed-up position and size
            glfwSetWindowMonitor(window, 0, windowPosX[0], windowPosY[0], windowWidth[0], windowHeight[0], 0);
        }
    }

    public boolean isFullscreen() {
        try{
            if(glfwGetWindowMonitor(window) == NULL){
                return false;
            } else if(glfwGetWindowMonitor(window) != 0){
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
