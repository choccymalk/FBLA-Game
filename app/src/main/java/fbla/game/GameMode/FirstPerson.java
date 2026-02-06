package fbla.game.GameMode;

import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;

import fbla.game.GameRenderer;

public class FirstPerson {
    private long window = -1;
    private GameRenderer renderer;

    public FirstPerson(long window, GameRenderer renderer) {
        this.window = window;
        this.renderer = renderer;
    }

    public void panCamera(int mouseX, int mouseY) {

        // Calculate mouse movement delta
        int deltaX = mouseX - 640;
        int deltaY = mouseY - 400;

        // Sensitivity factor for rotation (adjust as needed)
        float sensitivity = 0.2f;

        // Calculate new rotation angles
        float newRotationY = 0 - deltaX * sensitivity;
        float newRotationX = 0 - deltaY * sensitivity; // Invert Y axis for natural rotation

        // Optional: Clamp vertical rotation to prevent camera flipping
        // Typically limit pitch to [-89°, 89°] to avoid gimbal lock issues
        newRotationX = Math.max(-89.0f, Math.min(89.0f, newRotationX));

        // Set the new camera rotation
        // Z rotation not changed for camera panning
        renderer.setCameraRot(newRotationX, newRotationY, 0);
    }

    // x is left and right
    // y is up and down (usually camera isn't move up or down, maybe for crouching
    // or sitting)
    // z is forward and back
    public void moveForwardAndBackward(float moveAmount) {
        // Get current camera rotation
        float rotX = renderer.getCameraRotX(); // Pitch (up/down)
        float rotY = renderer.getCameraRotY(); // Yaw (left/right)

        // Convert angles to radians
        float yawRad = (float) Math.toRadians(rotY);

        // Calculate forward direction vector (ignore pitch for horizontal movement)
        float moveX = (float) Math.sin(yawRad) * moveAmount;
        float moveZ = (float) Math.cos(yawRad) * moveAmount;

        // Update camera position
        renderer.setCameraPos(
                renderer.getCameraPosX() + moveX, // usually would be negative because sin points right, but it's
                                                  // already set negative in the camera transformation matrix in the
                                                  // renderer
                renderer.getCameraPosY(),
                renderer.getCameraPosZ() + moveZ);
    }

    public void moveLeftAndRight(float moveAmount) {
        // Get current camera rotation
        float rotY = renderer.getCameraRotY(); // Yaw (left/right)

        // Convert angle to radians
        float yawRad = (float) Math.toRadians(rotY);

        // Calculate right direction vector (perpendicular to forward)
        // Right vector = forward rotated 90 degrees clockwise
        float moveX = (float) Math.cos(yawRad) * moveAmount;
        float moveZ = (float) Math.sin(yawRad) * moveAmount;

        // Update camera position
        renderer.setCameraPos(
                renderer.getCameraPosX() + moveX,
                renderer.getCameraPosY(),
                renderer.getCameraPosZ() - moveZ // Note: negative because sin points forward
        );
    }

    // Optional: Helper method for strafing (combining left/right with proper
    // vectors)
    public void strafe(float amount) {
        moveLeftAndRight(amount);
    }

    // Optional: Method for moving up/down (for flying or crouching)
    public void moveUpAndDown(float moveAmount) {
        renderer.setCameraPos(
                renderer.getCameraPosX(),
                renderer.getCameraPosY() + moveAmount,
                renderer.getCameraPosZ());
    }

    // Alternative: Combined movement method that uses all axes
    public void move(float forwardAmount, float rightAmount, float upAmount) {
        if (forwardAmount != 0) {
            moveForwardAndBackward(forwardAmount);
        }
        if (rightAmount != 0) {
            moveLeftAndRight(rightAmount);
        }
        if (upAmount != 0) {
            moveUpAndDown(upAmount);
        }
    }
}