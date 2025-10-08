package fbla.game;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Level {
    @SerializedName("collision_grid")
    private int[][] collisionGrid;

    @SerializedName("entities")
    private List<Entity> entities;

    @SerializedName("background_image")
    private String backgroundImage;

    @SerializedName("doors")
    private List<Door> doors;

    // Gson requires a no-arg constructor or it will use reflection; leaving none is fine.
    // Provide getters:
    public int[][] getCollisionGrid() {
        return collisionGrid;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public List<Door> getDoors() {
        return doors;
    }
}
