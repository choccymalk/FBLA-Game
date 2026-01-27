package fbla.game;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * SaveGame class handles serialization and deserialization of game state.
 * Stores entity positions, current level, player position, and other game data.
 * 
 * Save file format: JSON
 * Default save location: user.home/Desktop/FBLA-Game/saves/
 */
public class SaveGame {
    private static final String SAVE_DIR = System.getProperty("user.home")
            + "\\Desktop\\FBLA-Game\\saves";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String FILE_EXTENSION = ".save";

    private String saveFileName;
    private LocalDateTime saveTime;
    private GameState gameStateData;

    /**
     * Container class for storing all game state information
     */
    public static class GameState {
        public int currentLevelIndex;
        public int playerX;
        public int playerY;
        public List<EntityState> entities;
        public LocalDateTime timestamp;
        public String playerName;

        public GameState() {
            this.entities = new ArrayList<>();
            this.timestamp = LocalDateTime.now();
            this.playerName = "Player";
        }

        @Override
        public String toString() {
            return "GameState{" +
                    "level=" + currentLevelIndex +
                    ", playerPos=(" + playerX + "," + playerY + ")" +
                    ", entities=" + entities.size() +
                    ", time=" + timestamp +
                    '}';
        }
    }

    /**
     * Container class for storing individual entity state
     */
    public static class EntityState {
        public String entityType;
        public int positionX;
        public int positionY;
        public String entityName;
        public int textureId;
        public int width;
        public int height;
        public Map<String, Object> customData; // For storing NPC-specific data

        public EntityState() {
            this.customData = new HashMap<>();
        }

        public EntityState(Entity entity) {
            this();
            this.entityType = entity.getType();
            this.positionX = entity.getX();
            this.positionY = entity.getY();
            if(entity.getName() != null){
                this.entityName = entity.getName(); // Can be extended if Entity has a name field
            } else {
                this.entityName = entity.getType();
            }
            this.textureId = entity.getTextureId();
            this.width = entity.getWidth();
            this.height = entity.getHeight();
        }

        @Override
        public String toString() {
            return "EntityState{" +
                    "type='" + entityType + '\'' +
                    ", pos=(" + positionX + "," + positionY + ")" +
                    '}';
        }
    }

    // ========== CONSTRUCTOR ==========

    /**
     * Create a new SaveGame instance. File name is auto-generated with timestamp.
     */
    public SaveGame() {
        this.saveTime = LocalDateTime.now();
        this.saveFileName = "save_" + saveTime.format(DATE_FORMAT) + FILE_EXTENSION;
        this.gameStateData = new GameState();
        ensureSaveDirectoryExists();
    }

    /**
     * Create a SaveGame instance with a specific file name.
     * 
     * @param fileName Name of the save file (without directory path)
     */
    public SaveGame(String fileName) {
        this.saveTime = LocalDateTime.now();
        if (!fileName.endsWith(FILE_EXTENSION)) {
            this.saveFileName = fileName + FILE_EXTENSION;
        } else {
            this.saveFileName = fileName;
        }
        this.gameStateData = new GameState();
        ensureSaveDirectoryExists();
    }

    // ========== PUBLIC SAVE METHODS ==========

    /**
     * Save the current game state from the main game instance to a file.
     * 
     * @param main The main game instance
     * @return true if save was successful, false otherwise
     */
    public boolean saveGame(main main) {
        try {
            populateGameState(main);
            return writeToFile();
        } catch (Exception e) {
            System.err.println("Error saving game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Save game state with a custom file name.
     * 
     * @param main The main game instance
     * @param fileName Custom file name for this save
     * @return true if save was successful, false otherwise
     */
    public boolean saveGameAs(main main, String fileName) {
        this.saveFileName = fileName.endsWith(FILE_EXTENSION) ? fileName : fileName + FILE_EXTENSION;
        return saveGame(main);
    }

    // ========== PUBLIC LOAD METHODS ==========

    /**
     * Load game state from a save file.
     * 
     * @param fileName Name of the save file to load
     * @return true if load was successful, false otherwise
     */
    public boolean loadGame(String fileName) {
        try {
            this.saveFileName = fileName.endsWith(FILE_EXTENSION) ? fileName : fileName + FILE_EXTENSION;
            return readFromFile();
        } catch (Exception e) {
            System.err.println("Error loading game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Apply loaded game state to the main game instance.
     * Must be called after loadGame() and before continuing gameplay.
     * 
     * @param main The main game instance to update
     */
    public void applyLoadedState(main main) {
        if (gameStateData == null) {
            System.err.println("No game state loaded. Call loadGame() first.");
            return;
        }

        // Set the level (this will load entities)
        // Note: You may need to adjust this based on your actual implementation
        main.currentLevelIndex = gameStateData.currentLevelIndex;

        // Update player position
        main.playerX = gameStateData.playerX;
        main.playerY = gameStateData.playerY;

        // Update entity positions
        if (gameStateData.entities != null && gameStateData.entities.size() > 0) {
            for (int i = 0; i < gameStateData.entities.size() && i < main.entities.size(); i++) {
                EntityState entityState = gameStateData.entities.get(i);
                Entity entity = main.entities.get(i);
                entity.setPosition(entityState.positionX, entityState.positionY);
                entity.setWidth(entityState.width);
                entity.setHeight(entityState.height);
            }
        }

        System.out.println("Game state applied: " + gameStateData);
    }

    // ========== STATIC UTILITY METHODS ==========

    /**
     * Get a list of all available save files.
     * 
     * @return List of save file names
     */
    public static List<String> getAvailableSaves() {
        List<String> saveFiles = new ArrayList<>();
        try {
            ensureSaveDirectoryExists();
            Files.list(Paths.get(SAVE_DIR))
                    .filter(path -> path.toString().endsWith(FILE_EXTENSION))
                    .forEach(path -> saveFiles.add(path.getFileName().toString()));
        } catch (IOException e) {
            System.err.println("Error listing save files: " + e.getMessage());
        }
        return saveFiles;
    }

    /**
     * Delete a save file.
     * 
     * @param fileName Name of the save file to delete
     * @return true if deletion was successful, false otherwise
     */
    public static boolean deleteSave(String fileName) {
        try {
            String fullPath = SAVE_DIR + "\\" + (fileName.endsWith(FILE_EXTENSION) ? fileName : fileName + FILE_EXTENSION);
            return Files.deleteIfExists(Paths.get(fullPath));
        } catch (IOException e) {
            System.err.println("Error deleting save file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the save directory path.
     * 
     * @return The save directory path
     */
    public static String getSaveDirectory() {
        return SAVE_DIR;
    }

    // ========== GETTERS ==========

    public GameState getGameStateData() {
        return gameStateData;
    }

    public String getSaveFileName() {
        return saveFileName;
    }

    public LocalDateTime getSaveTime() {
        return saveTime;
    }

    public String getSaveFilePath() {
        return SAVE_DIR + "\\" + saveFileName;
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Populate the gameStateData from the main game instance.
     */
    private void populateGameState(main main) {
        gameStateData.currentLevelIndex = main.currentLevelIndex;
        gameStateData.playerX = main.playerX;
        gameStateData.playerY = main.playerY;
        gameStateData.timestamp = LocalDateTime.now();

        // Store all entity states
        gameStateData.entities.clear();
        for (Entity entity : main.getEntities()) {
            gameStateData.entities.add(new EntityState(entity));
        }

        System.out.println("Game state populated for save: " + gameStateData);
    }

    /**
     * Write the current game state to a JSON file.
     */
    private boolean writeToFile() throws IOException {
        ensureSaveDirectoryExists();

        String filePath = getSaveFilePath();
        JSONObject json = gameStateToJSON();

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(json.toString(2)); // Pretty print with 2-space indent
            System.out.println("Game saved successfully to: " + filePath);
            return true;
        }
    }

    /**
     * Read game state from a JSON file.
     */
    private boolean readFromFile() throws IOException {
        String filePath = getSaveFilePath();
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            System.err.println("Save file not found: " + filePath);
            return false;
        }

        String content = new String(Files.readAllBytes(path));
        try {
            JSONObject json = new JSONObject(content);
            gameStateData = jsonToGameState(json);
            System.out.println("Game loaded successfully from: " + filePath);
            System.out.println("Loaded state: " + gameStateData);
            return true;
        } catch (JSONException e) {
            System.err.println("Error parsing save file JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Convert GameState to JSON object.
     */
    private JSONObject gameStateToJSON() {
        JSONObject json = new JSONObject();

        json.put("version", "1.0");
        json.put("timestamp", gameStateData.timestamp.format(DATE_FORMAT));
        json.put("playerName", gameStateData.playerName);
        json.put("currentLevelIndex", gameStateData.currentLevelIndex);
        json.put("playerX", gameStateData.playerX);
        json.put("playerY", gameStateData.playerY);

        // Convert entities to JSON array
        JSONArray entitiesArray = new JSONArray();
        for (EntityState entity : gameStateData.entities) {
            JSONObject entityJson = new JSONObject();
            entityJson.put("type", entity.entityType);
            entityJson.put("name", entity.entityName);
            entityJson.put("x", entity.positionX);
            entityJson.put("y", entity.positionY);
            entityJson.put("width", entity.width);
            entityJson.put("height", entity.height);
            entityJson.put("textureId", entity.textureId);

            // Store custom data if present
            if (!entity.customData.isEmpty()) {
                entityJson.put("customData", new JSONObject(entity.customData));
            }

            entitiesArray.put(entityJson);
        }
        json.put("entities", entitiesArray);

        return json;
    }

    /**
     * Convert JSON object to GameState.
     */
    private GameState jsonToGameState(JSONObject json) {
        GameState state = new GameState();

        state.currentLevelIndex = json.getInt("currentLevelIndex");
        state.playerX = json.getInt("playerX");
        state.playerY = json.getInt("playerY");
        state.playerName = json.optString("playerName", "Player");

        // Parse entities array
        JSONArray entitiesArray = json.getJSONArray("entities");
        for (int i = 0; i < entitiesArray.length(); i++) {
            JSONObject entityJson = entitiesArray.getJSONObject(i);
            EntityState entity = new EntityState();
            entity.entityType = entityJson.getString("type");
            entity.entityName = entityJson.getString("name");
            entity.positionX = entityJson.getInt("x");
            entity.positionY = entityJson.getInt("y");
            entity.width = entityJson.getInt("width");
            entity.height = entityJson.getInt("height");
            entity.textureId = entityJson.getInt("textureId");

            // Load custom data if present
            if (entityJson.has("customData")) {
                JSONObject customDataJson = entityJson.getJSONObject("customData");
                for (String key : customDataJson.keySet()) {
                    entity.customData.put(key, customDataJson.get(key));
                }
            }

            state.entities.add(entity);
        }

        return state;
    }

    /**
     * Ensure the save directory exists, creating it if necessary.
     */
    private static void ensureSaveDirectoryExists() {
        try {
            Files.createDirectories(Paths.get(SAVE_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create save directory: " + e.getMessage());
        }
    }

    // ========== EXAMPLE USAGE ==========
    /**
     * Example of how to use the SaveGame class in the main game:
     * 
     * // SAVING
     * SaveGame saveGame = new SaveGame("my_save_1");
     * if (saveGame.saveGame(mainInstance)) {
     *     System.out.println("Game saved!");
     * }
     * 
     * // LOADING
     * SaveGame loadGame = new SaveGame();
     * if (loadGame.loadGame("my_save_1")) {
     *     loadGame.applyLoadedState(mainInstance);
     * }
     * 
     * // LIST ALL SAVES
     * List<String> saves = SaveGame.getAvailableSaves();
     * for (String save : saves) {
     *     System.out.println("Available save: " + save);
     * }
     */
}