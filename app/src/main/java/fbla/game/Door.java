package fbla.game;

import com.google.gson.annotations.SerializedName;

public class Door {
    @SerializedName("x")
    private int x;

    @SerializedName("y")
    private int y;

    @SerializedName("target_level")
    private int targetLevel;

    @SerializedName("target_x")
    private int targetX;

    @SerializedName("target_y")
    private int targetY;

    @SerializedName("image")
    private String imagePath;

    private transient int textureId;

    public Door(int x, int y, int targetX, int targetY, int targetLevel, String imagePath){
        this.x = x;
        this.y = y;
        this.targetX = targetX;
        this.targetY = targetY;
        this.imagePath = imagePath;
        this.targetLevel = targetLevel;
    }

    // Getters:
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetY() {
        return targetY;
    }

    public void setTextureId(int id){
        this.textureId = id;
    }
    
    public int getTextureId(){
        return textureId;
    }

    public String getImagePath(){
        return imagePath;
    }

}
