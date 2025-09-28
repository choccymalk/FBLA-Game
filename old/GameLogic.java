package fbla.game;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class GameLogic {
    private BufferedImage background;
    private EntityManager entityManager;
    private WindowManager windowManager;
    
    public GameLogic(WindowManager windowManager) {
        this.windowManager = windowManager;
        this.entityManager = new EntityManager();
        loadBackground();
    }
    
    private void loadBackground() {
        try {
            File backgroundFile = new File(GameConstants.BACKGROUND_IMAGE_PATH);
            background = ImageIO.read(backgroundFile);
        } catch (IOException e) {
            System.err.println("Error loading background image: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void update() {
        entityManager.updateAll(windowManager.getWidth(), windowManager.getHeight());
    }
    
    // Getters
    public BufferedImage getBackground() { return background; }
    public EntityManager getEntityManager() { return entityManager; }
}