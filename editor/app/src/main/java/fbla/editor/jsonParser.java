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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class jsonParser {
    private File levelsJson;
    private File object3dsJson;
    private final Gson gson = new Gson();
    private List<Level> levels = new ArrayList<>();
    private List<Object3D> object3Ds = new ArrayList<>();

    public jsonParser(File levelsJson) {
        if (levelsJson == null) throw new IllegalArgumentException("levelsJson cannot be null");
        this.levelsJson = levelsJson;
        parse();
    }

    public jsonParser(File levelsJson, File object3dsJson) {
        if(object3dsJson == null) throw new IllegalArgumentException("object3dsJson cannot be null");
        this.object3dsJson = object3dsJson;
        parse3dObjects();
    }

    private void parse() {
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

    private void parse3dObjects() {
        try (FileReader reader = new FileReader(object3dsJson)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null) {
                object3Ds = Collections.emptyList();
                return;
            }

            JsonElement objectsElement = root.get("3d_objects");
            if (objectsElement == null || objectsElement.isJsonNull()) {
                object3Ds = Collections.emptyList();
                return;
            }

            Type listType = new TypeToken<List<Object3D>>() {}.getType();
            object3Ds = gson.fromJson(objectsElement, listType);
            if (object3Ds == null) object3Ds = Collections.emptyList();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            object3Ds = Collections.emptyList();
        } catch (IOException e) {
            e.printStackTrace();
            object3Ds = Collections.emptyList();
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

    public List<Object3D> getAllObject3ds(){
        return this.object3Ds;
    }

    public List<Object3D> getObject3dsInLevel(int levelIndex){
        List<Object3D> objects = new ArrayList<>();
        for (Object3D object3d : this.object3Ds) {
            if(object3d.getLevelIndex() == levelIndex){
                objects.add(object3d);
            }
        }
        return objects;
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