package fbla.game;

public class GameConstants {
    // File paths
    public static final String PLAYER_IMAGE_PATH = "C:\\Users\\Bentley\\Documents\\FBLA-game\\game_resources\\textures\\ios_large_1662954661_image.jpg";
    public static final String NPC_IMAGE_PATH = "C:\\Users\\Bentley\\Documents\\FBLA-game\\game_resources\\textures\\npc.png";
    public static final String BACKGROUND_IMAGE_PATH = "C:\\Users\\Bentley\\Documents\\FBLA-game\\game_resources\\textures\\background.jpg";
    
    // Game settings
    public static final int PLAYER_MOVE_SPEED = 5;
    public static final int NPC_MOVE_SPEED = 2;
    public static final int TIMER_DELAY = 20; // milliseconds
    public static final double IMAGE_SCALE_FACTOR = 0.5;
    
    // Window settings
    public static final String WINDOW_TITLE = "Multi-Object Game";
    public static final int DEFAULT_WIDTH = 400;
    public static final int DEFAULT_HEIGHT = 400;
    
    // Game object types
    public enum EntityType {
        PLAYER,
        NPC,
        ENEMY
    }
}