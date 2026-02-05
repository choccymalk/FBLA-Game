package fbla.editor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.*;

import org.lwjgl.BufferUtils;

public class Renderer3D {

    private List<Object3D> loaded3DObjects = new ArrayList<>();

    public Renderer3D() {

    }

    public void setupPerspective(float fov, float aspect, float zNear, float zFar) {
        float fh = (float) Math.tan(Math.toRadians(fov / 2.0f)) * zNear;
        float fw = fh * aspect;
        glFrustum(-fw, fw, -fh, fh, zNear, zFar);
    }

    /**
     * Loads a 3D object from an OBJ file
     * Parses vertices, texture coordinates, and faces
     * 
     * @param filePath   Path to the .obj file
     * @param textureId  OpenGL texture ID to apply to the object
     * @param objectName Name identifier for the object
     * @return Object3D instance, or null if loading fails
     */
    public Object3D load3DObject(String filePath, int textureId, String objectName) {
        List<float[]> tempVertices = new ArrayList<>();
        List<float[]> tempTexCoords = new ArrayList<>();
        List<Float> packedData = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("\\s+");
                if (parts[0].equals("v")) {
                    tempVertices.add(new float[] { Float.parseFloat(parts[1]), Float.parseFloat(parts[2]),
                            Float.parseFloat(parts[3]) });
                } else if (parts[0].equals("vt")) {
                    tempTexCoords.add(new float[] { Float.parseFloat(parts[1]), 1.0f - Float.parseFloat(parts[2]) });
                } // Inside load3DObject, replace the 'f' parsing block:
                else if (parts[0].equals("f")) {
                    // Standard OBJ faces can have 3, 4, or more vertices.
                    // We must convert them to triangles (1-2-3, 1-3-4, 1-4-5...)
                    for (int i = 2; i < parts.length - 1; i++) {
                        packVertex(parts[1], tempVertices, tempTexCoords, packedData);
                        packVertex(parts[i], tempVertices, tempTexCoords, packedData);
                        packVertex(parts[i + 1], tempVertices, tempTexCoords, packedData);
                    }
                }
            }

            float[] vertexData = new float[packedData.size()];
            for (int i = 0; i < packedData.size(); i++) {
                vertexData[i] = packedData.get(i);
            }

            Object3D obj = new Object3D(objectName, vertexData.length / 5);
            //obj.textureId = textureId;
            obj.setTextureId(textureId);
            // --- UPLOAD TO GPU VBO ---
            FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexData.length);
            buffer.put(vertexData).flip();

            //obj.vboId = glGenBuffers();
            obj.setVboId(glGenBuffers());
            glBindBuffer(GL_ARRAY_BUFFER, obj.getVboId());
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            loaded3DObjects.add(obj);
            return obj;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ONLY used if loading a level
    public Object3D load3DObject(String filePath, int textureId, String objectName, float x, float y, float z, float sx, float sy, float sz, float ax, float ay, float az) {
        List<float[]> tempVertices = new ArrayList<>();
        List<float[]> tempTexCoords = new ArrayList<>();
        List<Float> packedData = new ArrayList<>();
        filePath = LevelEditor.RESOURCE_PATH + "\\models\\" + filePath;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("\\s+");
                if (parts[0].equals("v")) {
                    tempVertices.add(new float[] { Float.parseFloat(parts[1]), Float.parseFloat(parts[2]),
                            Float.parseFloat(parts[3]) });
                } else if (parts[0].equals("vt")) {
                    tempTexCoords.add(new float[] { Float.parseFloat(parts[1]), 1.0f - Float.parseFloat(parts[2]) });
                } // Inside load3DObject, replace the 'f' parsing block:
                else if (parts[0].equals("f")) {
                    // Standard OBJ faces can have 3, 4, or more vertices.
                    // We must convert them to triangles (1-2-3, 1-3-4, 1-4-5...)
                    for (int i = 2; i < parts.length - 1; i++) {
                        packVertex(parts[1], tempVertices, tempTexCoords, packedData);
                        packVertex(parts[i], tempVertices, tempTexCoords, packedData);
                        packVertex(parts[i + 1], tempVertices, tempTexCoords, packedData);
                    }
                }
            }

            float[] vertexData = new float[packedData.size()];
            for (int i = 0; i < packedData.size(); i++) {
                vertexData[i] = packedData.get(i);
            }

            Object3D obj = new Object3D(objectName, vertexData.length / 5);
            obj.setTextureId(textureId);

            // --- UPLOAD TO GPU VBO ---
            FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexData.length);
            buffer.put(vertexData).flip();

            obj.setVboId(glGenBuffers());
            glBindBuffer(GL_ARRAY_BUFFER, obj.getVboId());
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            move3DObject(obj, x, y, z);
            scale3DObject(obj, sx, sy, sz);
            rotate3DObject(obj, ax, ay, az);

            loaded3DObjects.add(obj);
            return obj;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public List<Object3D> getLoaded3DObjects() {
        return this.loaded3DObjects;
    }

    /**
     * Helper to resolve OBJ indices and pack them into our [X,Y,Z,U,V] format
     */
    public void packVertex(String vertexPart, List<float[]> vList, List<float[]> vtList, List<Float> target) {
        String[] subParts = vertexPart.split("/");

        // OBJ indices are 1-based
        int vIdx = Integer.parseInt(subParts[0]) - 1;
        float[] pos = vList.get(vIdx);

        target.add(pos[0]); // X
        target.add(pos[1]); // Y
        target.add(pos[2]); // Z

        if (subParts.length > 1 && !subParts[1].isEmpty()) {
            int vtIdx = Integer.parseInt(subParts[1]) - 1;
            float[] tex = vtList.get(vtIdx);
            target.add(tex[0]); // U
            target.add(tex[1]); // V
        } else {
            target.add(0f);
            target.add(0f);
        }
    }

    /**
     * Renders a 3D object by using vertex arrays with VBO
     * 
     * @param obj The Object3D to render
     */
    public void render3DObject(Object3D obj) {
        if (obj.getVboId() == -1)
            return;

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, obj.getTextureId());

        // Disable culling to render both sides of faces
        //glDisable(GL_CULL_FACE);

        glPushMatrix();
        glTranslatef(obj.getX(), obj.getY(), obj.getZ());
        glRotatef(obj.getRotationX(), 1, 0, 0);
        glRotatef(obj.getRotationY(), 0, 1, 0);
        glRotatef(obj.getRotationZ(), 0, 0, 1);
        glScalef(obj.getScaleX(), obj.getScaleY(), obj.getScaleZ());

        glBindBuffer(GL_ARRAY_BUFFER, obj.getVboId());

        // STRIDE: 5 floats * 4 bytes = 20 bytes per vertex
        int stride = 20;

        glEnableClientState(GL_VERTEX_ARRAY);
        // 3 components (X,Y,Z), offset 0
        glVertexPointer(3, GL_FLOAT, stride, 0);

        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        // 2 components (U,V), offset 12 bytes (3 floats * 4 bytes)
        glTexCoordPointer(2, GL_FLOAT, stride, 12);

        glDrawArrays(GL_TRIANGLES, 0, obj.getVertexCount());

        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glPopMatrix();
    }

    // used to pretend that the canvas is the only part of the window and that the
    // origin of the canvas is the origin of the window
    // used to pretend that the canvas is the only part of the window and that the
    // origin of the canvas is the origin of the window
    public void clipObjectToCanvasBounds(float canvasPositionInWindowX, float canvasPositionInWindowY, float canvasW,
            float canvasH, Object3D obj) {

        // 1. Calculate OpenGL Coordinates (Bottom-Left Origin)
        // ImGui uses Top-Left, OpenGL uses Bottom-Left. We must flip Y.
        float windowHeight = ImGui.getIO().getDisplaySizeY();
        int glX = (int) canvasPositionInWindowX;
        int glY = (int) (windowHeight - canvasPositionInWindowY - canvasH);
        int glW = (int) canvasW;
        int glH = (int) canvasH;

        // 2. Save Previous State
        // We are about to change global GL settings (Viewport, Matrix, Scissor).
        // We must save them so we don't break the ImGui rendering that happens after
        // this.
        IntBuffer prevViewport = BufferUtils.createIntBuffer(16);
        glGetIntegerv(GL_VIEWPORT, prevViewport);

        glPushAttrib(GL_SCISSOR_BIT | GL_ENABLE_BIT); // Save Scissor test state and enabled caps

        // 3. Set Viewport and Scissor
        // Viewport: Maps (-1 to 1) NDCs to the specific pixels of the canvas.
        // This ensures (0,0,0) is the CENTER of the canvas, not the window.
        glViewport(glX, glY, glW, glH);

        // Scissor: Actually cuts off pixels outside the canvas bounds.
        glEnable(GL_SCISSOR_TEST);
        glScissor(glX, glY, glW, glH);

        // 4. Update Projection Matrix for correct Aspect Ratio
        // If we don't do this, the object will look squashed if the canvas isn't 16:9.
        /*glMatrixMode(GL_PROJECTION);
        glPushMatrix(); // Save the Editor's projection (likely orthographic for UI)
        glLoadIdentity();

        float aspectRatio = (float) glW / (float) glH;
        // Re-run perspective setup for just this canvas
        // (Using standard game defaults: FOV 70, Near 0.1, Far 1000)
        setupPerspective(70.0f, aspectRatio, 0.1f, 1000.0f);

        glMatrixMode(GL_MODELVIEW);*/
        // actually don't do this, as the canvas will always be 16:9
        // IMPORTANT: SKIP STEP 4!

        // (Modelview is handled inside render3DObject via glPushMatrix, so we are safe
        // there)

        // 5. Render
        render3DObject(obj);

        // 6. Restore State
        // Restore Projection Matrix
        // glMatrixMode(GL_PROJECTION);
        // glPopMatrix();
        // glMatrixMode(GL_MODELVIEW); // Go back to modelview mode for safety

        // Restore Viewport
        glViewport(prevViewport.get(0), prevViewport.get(1), prevViewport.get(2), prevViewport.get(3));

        // Restore Scissor/Enable bits
        glPopAttrib();
    }

    /**
     * Helper method to apply rotation transformations in 3D space
     * 
     * @return Rotated [x, y, z] coordinates
     */
    public float[] applyRotations(float x, float y, float z, float rotZ, float rotY, float rotX) {
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

        return new float[] { x3, y3, z3 };
    }

    /**
     * Move a 3D object to a new position
     * 
     * @param obj The Object3D to move
     * @param x   New X coordinate
     * @param y   New Y coordinate
     * @param z   New Z coordinate
     */
    public void move3DObject(Object3D obj, float x, float y, float z) {
        obj.setX(x);
        obj.setY(y);
        obj.setZ(z);
    }

    /**
     * Set the scale of a 3D object
     * 
     * @param obj    The Object3D to scale
     * @param scaleX Scale factor for X axis
     * @param scaleY Scale factor for Y axis
     * @param scaleZ Scale factor for Z axis
     */
    public void scale3DObject(Object3D obj, float scaleX, float scaleY, float scaleZ) {
        obj.setScaleX(scaleX);
        obj.setScaleY(scaleY);
        obj.setScaleZ(scaleZ);
    }

    /**
     * Rotate a 3D object
     * 
     * @param obj  The Object3D to rotate
     * @param rotX Rotation around X axis (degrees)
     * @param rotY Rotation around Y axis (degrees)
     * @param rotZ Rotation around Z axis (degrees)
     */
    public void rotate3DObject(Object3D obj, float rotX, float rotY, float rotZ) {
        obj.setRoationX(rotX);
        obj.setRoationY(rotY);
        obj.setRoationZ(rotZ);
    }

    /**
     * Get a loaded 3D object by name
     * 
     * @param objectName The name of the object
     * @return The Object3D, or null if not found
     */
    public Object3D get3DObject(String objectName) {
        for (Object3D obj : loaded3DObjects) {
            if (obj.getName().equals(objectName)) {
                return obj;
            }
        }
        return null;
    }

    /**
     * Remove a 3D object from the scene
     * 
     * @param objectName The name of the object to remove
     */
    public void remove3DObject(String objectName) {
        for (int i = 0; i < loaded3DObjects.size(); i++) {
            Object3D obj = loaded3DObjects.get(i);
            if (obj.getName().equals(objectName)) {
                loaded3DObjects.remove(i);
                return;
            }
        }
    }

    public void removeAll3DObjects(){
        for (Object3D object3d : loaded3DObjects) {
            System.out.println("removed 3d obj with name " + object3d.getName());
            loaded3DObjects.remove(object3d);
        }
    }
}
