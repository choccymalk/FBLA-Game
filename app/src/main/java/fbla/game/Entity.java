package fbla.game;

import com.google.gson.annotations.SerializedName;

import java.io.Serial;
import java.util.List;

public class Entity {
    // Fields that come from JSON
    @SerializedName("type")
    private String type;

    @SerializedName("x")
    private int x;

    @SerializedName("y")
    private int y;

    @SerializedName("dialogue_tree")
    private dialogueTree dialogueTree;

    @SerializedName("image")
    private String imagePath;

    @SerializedName("animation_states")
    private animationStates animationStates;

    // Fields you want to set at runtime (JSON doesn't contain them).
    // Mark transient if you do NOT want Gson to attempt to read/write them.
    private transient int textureId;
    private transient int width;
    private transient int height;
    private transient int targetLevel; // only used if the entity is a door
    private transient int targetX; // only used if the entity is a door
    private transient int targetY; // only used if the entity is a door

    // No-arg constructor required by Gson
    public Entity() {}

    // Optional convenience constructor for runtime creation
    public Entity(String type, int textureId, int x, int y, int width, int height) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.textureId = textureId;
        this.width = width;
        this.height = height;
    }

    // only used if creating door
    public Entity(String type, int textureId, int x, int y, int width, int height, int targetLevel, int targetX, int targetY) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.textureId = textureId;
        this.width = width;
        this.height = height;
        this.targetLevel = targetLevel;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    // Getters for JSON fields
    public String getType() { return type; }
    public int getX() { return x; }
    public int getY() { return y; }
    public dialogueTree getDialogueTree() { return dialogueTree; }
    // Getters/setters for runtime fields
    public int getTextureId() { return textureId; }
    public void setTextureId(int textureId) { this.textureId = textureId; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public String getImagePath() { return imagePath; }

    public animationStates getAnimationStates() { return animationStates; }

    // only used if the entity is a door
    public int getTargetLevel(){
        return targetLevel;
    }
    public void setTargetLevel(int targetLevel){
        this.targetLevel = targetLevel;
    }
    public int getTargetX(){
        return targetX;
    }
    public void setTargetX(int targetX){
        this.targetX = targetX;
    }

    // Optional convenience
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
