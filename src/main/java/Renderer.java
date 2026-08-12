import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class Renderer {
    private final Matrix4f projectionMatrix;
    private final Matrix4f modelViewMatrix;
    private final float[] matrixBuffer = new float[16];

    public Renderer() {
        projectionMatrix = new Matrix4f()
                .perspective((float) Math.toRadians(60.0), 1.0f, 0.1f, 100.0f);
        modelViewMatrix = new Matrix4f();

        // Dark blue background
        GL11.glClearColor(0.0f, 0.0f, 0.1f, 1.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // Set projection matrix once
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        projectionMatrix.get(matrixBuffer);
        GL11.glLoadMatrixf(matrixBuffer);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    public void render(Window window) {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        double time = GLFW.glfwGetTime();
        float angleX = (float) Math.toRadians(time * 30.0);
        float angleY = (float) Math.toRadians(time * 45.0);

        modelViewMatrix.identity()
                .translate(0.0f, 0.0f, -5.0f)
                .rotateX(angleX)
                .rotateY(angleY);

        modelViewMatrix.get(matrixBuffer);
        GL11.glLoadMatrixf(matrixBuffer);

        GL11.glBegin(GL11.GL_QUADS);
        for (int i = 0; i < CubeData.VERTICES.length / 3; i++) {
            GL11.glColor3f(
                    CubeData.COLORS[i * 3],
                    CubeData.COLORS[i * 3 + 1],
                    CubeData.COLORS[i * 3 + 2]
            );
            GL11.glVertex3f(
                    CubeData.VERTICES[i * 3],
                    CubeData.VERTICES[i * 3 + 1],
                    CubeData.VERTICES[i * 3 + 2]
            );
        }
        GL11.glEnd();
    }

    public void cleanup() {
        // Nothing to clean up for fixed-function rendering.
    }
}