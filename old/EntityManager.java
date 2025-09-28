package fbla.game;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EntityManager {
    private List<GameEntity> entities;
    private PlayerEntity player;
    private BufferedImage playerImage;
    private BufferedImage npcImage;
    private Random random;
    
    public EntityManager() {
        entities = new ArrayList<>();
        random = new Random();
        loadImages();
        createEntities();
    }
    
    private void loadImages() {
        try {
            File playerFile = new File(GameConstants.PLAYER_IMAGE_PATH);
            if (playerFile.exists()) {
                playerImage = ImageIO.read(playerFile);
            }
            
            // Try to load NPC image, fall back to player image if not found
            File npcFile = new File(GameConstants.NPC_IMAGE_PATH);
            if (npcFile.exists()) {
                npcImage = ImageIO.read(npcFile);
            } else {
                npcImage = playerImage; // Use same image if NPC image not found
            }
            
        } catch (IOException e) {
            System.err.println("Error loading entity images: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createEntities() {
        // Create player at center-ish position
        player = new PlayerEntity(100, 100);
        player.setImage(playerImage);
        entities.add(player);
        
        // Create some NPCs at random positions
        createRandomNPCs(5);
    }
    
    public void createRandomNPCs(int count) {
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(600) + 50; // Random x between 50-650
            int y = random.nextInt(400) + 50; // Random y between 50-450
            
            NPCEntity npc = new NPCEntity(x, y);
            npc.setImage(npcImage);
            entities.add(npc);
        }
    }
    
    public void addEntity(GameEntity entity) {
        entities.add(entity);
    }
    
    public void removeEntity(GameEntity entity) {
        entities.remove(entity);
    }
    
    public void updateAll(int screenWidth, int screenHeight) {
        // Update all entities
        for (GameEntity entity : entities) {
            if (entity.isActive()) {
                entity.update(screenWidth, screenHeight);
            }
        }
        
        // Check for collisions (example implementation)
        checkCollisions();
        
        // Remove inactive entities
        entities.removeIf(entity -> !entity.isActive());
    }
    
    private void checkCollisions() {
        // Example: Check if player collides with any NPCs
        for (GameEntity entity : entities) {
            if (entity != player && entity.getType() == GameConstants.EntityType.NPC) {
                if (player.collidesWith(entity)) {
                    // Handle collision - for now, just print a message
                    System.out.println("Player collided with NPC!");
                    // You could add more complex collision handling here
                }
            }
        }
    }
    
    // Getters
    public List<GameEntity> getAllEntities() { return new ArrayList<>(entities); }
    public PlayerEntity getPlayer() { return player; }
    
    public List<GameEntity> getEntitiesByType(GameConstants.EntityType type) {
        List<GameEntity> result = new ArrayList<>();
        for (GameEntity entity : entities) {
            if (entity.getType() == type) {
                result.add(entity);
            }
        }
        return result;
    }
}