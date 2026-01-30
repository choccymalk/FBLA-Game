package fbla.editor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class jsonParser {
    private final File levelsJson;
    private final Gson gson = new Gson();
    private List<Level> levels = Collections.emptyList();

    public jsonParser(File levelsJson) {
        if (levelsJson == null) throw new IllegalArgumentException("levelsJson cannot be null");
        this.levelsJson = levelsJson;
        parse();
    }

    public void parse() {
        try (FileReader reader = new FileReader(levelsJson)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null) {
                levels = Collections.emptyList();
                return;
            }

            JsonElement levelsElement = root.get("levels");
            if (levelsElement == null || levelsElement.isJsonNull()) {
                levels = Collections.emptyList();
                return;
            }

            Type listType = new TypeToken<List<Level>>() {}.getType();
            levels = gson.fromJson(levelsElement, listType);
            if (levels == null) levels = Collections.emptyList();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            levels = Collections.emptyList();
        } catch (IOException e) {
            e.printStackTrace();
            levels = Collections.emptyList();
        }
    }

    // Core getter for all levels
    public List<Level> getLevels() {
        return Collections.unmodifiableList(levels);
    }

    // Safe getters that check bounds and nulls
    public int[][] getCollisionGrid(int levelIndex) {
        checkIndex(levelIndex);
        return levels.get(levelIndex).getCollisionGrid();
    }

    public List<Entity> getEntities(int levelIndex) {
        checkIndex(levelIndex);
        return levels.get(levelIndex).getEntities();
    }

    public String getBackgroundImage(int levelIndex) {
        checkIndex(levelIndex);
        return levels.get(levelIndex).getBackgroundImage();
    }

    public List<Door> getDoors(int levelIndex) {
        checkIndex(levelIndex);
        return levels.get(levelIndex).getDoors();
    }

    public Level getLevel(int index) {
        checkIndex(index);
        return levels.get(index);
    }

    private void checkIndex(int idx) {
        if (levels == null || levels.isEmpty()) {
            throw new IndexOutOfBoundsException("No levels loaded");
        }
        if (idx < 0 || idx >= levels.size()) {
            throw new IndexOutOfBoundsException("Level index out of bounds: " + idx);
        }
    }
}

/* example json this class should parse, this is read from File levelsJson
  {
    "levels": [
        {
            "collision_grid": [
                [1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
                [1, 0, 0, 0, 0, 0, 0, 0, 0, 1],
                [1, 0, 1, 1, 1, 1, 1, 1, 0, 1],
                [1, 0, 1, 0, 0, 0, 0, 1, 0, 1],
                [1, 0, 1, 0, 1, 1, 0, 1, 0, 1],
                [1, 0, 0, 0, 0, 0, 0, 0, 2, 1],
                [1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
            ],
            "entities": [
                {"type": "player", "x": 1, "y": 1},
                {"type": "npc", "x": 3, "y": 3, "dialogue_sequential": ["Hello there!", "Welcome to the game!", "You are currently in level 0."]}
            ],
            "background_image": "background1.png",
            "doors": [
                {"x": 8, "y": 5, "target_level": 2, "target_x": 1, "target_y": 1}
            ]
        }
    ]
    }
 */