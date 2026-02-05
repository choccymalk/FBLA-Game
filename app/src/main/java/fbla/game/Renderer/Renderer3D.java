package fbla.game.Renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

import fbla.game.main;

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
        filePath = main.RESOURCE_PATH + "\\models\\" + filePath;
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
        for (int i = 0; i < loaded3DObjects.size(); i++) {
            loaded3DObjects.remove(i);
        }
    }

    // ONLY used for loading a new 3d object in loadlevel
    public int loadTexture3DObj(String filePath) {
        filePath = main.RESOURCE_PATH + "\\textures\\" + filePath;
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
