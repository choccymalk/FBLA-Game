import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.glfw.GLFW.*;

public class Window {
    private final long windowHandle;

    public Window(int width, int height, String title) {
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_FALSE);

        windowHandle = glfwCreateWindow(width, height, title, 0L, 0L);
        if (windowHandle == 0L) {
            glfwTerminate();
            throw new RuntimeException("Failed to create GLFW window");
        }

        glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();

        glfwSwapInterval(1);

        glfwSetFramebufferSizeCallback(windowHandle, (win, w, h) ->
                GL11.glViewport(0, 0, w, h));
        GL11.glViewport(0, 0, width, height);
    }

    public void show() {
        glfwShowWindow(windowHandle);
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public void update() {
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }

    public void cleanup() {
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
    }
}