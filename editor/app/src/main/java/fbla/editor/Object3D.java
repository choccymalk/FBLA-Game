package fbla.editor;

public class Object3D {
    public String name;
    public int vertexCount;
    public int vboId = -1; // -1 means no VBO is assigned yet
    public int textureId;
    public float x, y, z;
    public float scaleX = 1f, scaleY = 1f, scaleZ = 1f;
    public float rotationX = 0f, rotationY = 0f, rotationZ = 0f;
    public int levelIndex = -1;
    public String modelPath = "";
    public String texturePath = "";

    public Object3D(String name, int vertexCount) {
            this.name = name;
            this.vertexCount = vertexCount;
    }

    public float getX(){
        return this.x;
    }

    public float getY(){
        return this.y;
    }

    public float getZ(){
        return this.z;
    }

    public void setX(float x){
        this.x = x;
    }
    
    public void setY(float y){
        this.y = y;
    }

    public void setZ(float z){
        this.z = z;
    }

    public float getScaleX(){
        return this.scaleX;
    }

    public float getScaleY(){
        return this.scaleY;
    }

    public float getScaleZ(){
        return this.scaleZ;
    }

    public void setScaleX(float x){
        this.scaleX = x;
    }

    public void setScaleY(float y){
        this.scaleY = y;
    }

    public void setScaleZ(float z){
        this.scaleZ = z;
    }

    public float getRotationX(){
        return this.rotationX;
    }

    public float getRotationY(){
        return this.rotationY;
    }

    public float getRotationZ(){
        return this.rotationZ;
    }

    public void setRoationX(float x){
        this.rotationX = x;
    }

    public void setRoationY(float y){
        this.rotationY = y;
    }
    
    public void setRoationZ(float z){
        this.rotationZ = z;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getModelPath(){
        return this.modelPath;
    }

    public void setModelPath(String modelPath){
        this.modelPath = modelPath;
    }

    public String getTexturePath(){
        return this.texturePath;
    }

    public void setTexturePath(String texturePath){
        this.texturePath = texturePath;
    }
}
