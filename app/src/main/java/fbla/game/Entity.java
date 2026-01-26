package fbla.game;

import com.google.gson.annotations.SerializedName;

import java.io.Serial;
import java.util.ArrayList;
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

    @SerializedName("ai_abilities")
    private AIAbilities AIAbilities;

    @SerializedName("name")
    private String name;

    // Fields you want to set at runtime (JSON doesn't contain them).
    // Mark transient if you do NOT want Gson to attempt to read/write them.
    private transient int textureId;
    private transient int width;
    private transient int height;
    private transient int targetLevel; // only used if the entity is a door
    private transient int targetX; // only used if the entity is a door
    private transient int targetY; // only used if the entity is a door
    private transient entityAnimation animation;

    // No-arg constructor required by Gson
    public Entity() {}

    // Optional convenience constructor for runtime creation
    public Entity(String type, int textureId, int x, int y, int width, int height, entityAnimation animation) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.textureId = textureId;
        this.width = width;
        this.height = height;
        this.animation = animation;
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
    // window space, must be converted to grid space for pathfinding, divide by GRID_CELL_SIZE
    public int getX() { return x; }
    // window space, must be converted to grid space for pathfinding, divide by GRID_CELL_SIZE
    public int getY() { return y; }
    public dialogueTree getDialogueTree() { return dialogueTree; }
    public AIAbilities getAIAbilities() { return AIAbilities; }
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
    public int getTargetY(){
        return targetY;
    }
    public void setTargetY(int targetY){
        this.targetY = targetY;
    }
    public String getName(){
        return this.name;
    }
    public entityAnimation getEntityAnimation(){
        return animation;
    }
    public void setEntityAnimation(entityAnimation animation){
        this.animation = animation;
    }

    // Optional convenience
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
