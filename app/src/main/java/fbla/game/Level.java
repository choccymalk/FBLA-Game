package fbla.game;

import java.util.Arrays;
import java.util.List;

import fbla.game.*;

public class Level {
    int[][] grid;
    List<Entity> entities;
    String background_image;
    List<Door> doors;

    public int[][] getGrid() {
        return grid;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public String getBackground_image() {
        return background_image;
    }

    public List<Door> getDoors() {
        return doors;
    }

    public Level(int[][] grid, List<Entity> entities, String background_image, List<Door> doors) {
        this.grid = grid;
        this.entities = entities;
        this.background_image = background_image;
        this.doors = doors;
    }

    @Override
    public String toString() {
        return "Level{" +
                "grid=" + Arrays.deepToString(grid) +
                ", entities=" + entities +
                ", background_image='" + background_image + '\'' +
                ", doors=" + doors +
                '}';
    }
}
