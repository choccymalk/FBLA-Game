package fbla.game;

import java.awt.image.BufferedImage;
import java.awt.Graphics;

public abstract class GameEntity {
    protected BufferedImage image;
    protected int x, y;
    protected int width, height;
    protected int xVelocity, yVelocity;
    protected int maxSpeed;
    protected GameConstants.EntityType type;
    protected boolean active;
    
    public GameEntity(int x, int y, int maxSpeed, GameConstants.EntityType type) {
        this.x = x;
        this.y = y;
        this.maxSpeed = maxSpeed;
        this.type = type;
        this.active = true;
        this.xVelocity = 0;
        this.yVelocity = 0;
    }
    
    // Abstract method that subclasses must implement for specific behavior
    public abstract void update(int screenWidth, int screenHeight);
    
    // Common update logic for position and boundary checking
    protected void updatePosition(int screenWidth, int screenHeight) {
        x += xVelocity;
        y += yVelocity;
        
        // Keep entity within screen bounds
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > screenWidth - width) x = screenWidth - width;
        if (y > screenHeight - height) y = screenHeight - height;
    }
    
    public void render(Graphics g) {
        if (active && image != null) {
            g.drawImage(image, x, y, width, height, null);
        }
    }
    
    // Check collision with another entity
    public boolean collidesWith(GameEntity other) {
        return active && other.active &&
               x < other.x + other.width &&
               x + width > other.x &&
               y < other.y + other.height &&
               y + height > other.y;
    }
    
    // Getters and setters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getXVelocity() { return xVelocity; }
    public int getYVelocity() { return yVelocity; }
    public GameConstants.EntityType getType() { return type; }
    public boolean isActive() { return active; }
    public BufferedImage getImage() { return image; }
    
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setXVelocity(int xVelocity) { 
        this.xVelocity = Math.max(-maxSpeed, Math.min(maxSpeed, xVelocity)); 
    }
    public void setYVelocity(int yVelocity) { 
        this.yVelocity = Math.max(-maxSpeed, Math.min(maxSpeed, yVelocity)); 
    }
    public void setActive(boolean active) { this.active = active; }
    public void setImage(BufferedImage image) { 
        this.image = image;
        if (image != null) {
            this.width = (int)(image.getWidth() * GameConstants.IMAGE_SCALE_FACTOR);
            this.height = (int)(image.getHeight() * GameConstants.IMAGE_SCALE_FACTOR);
        }
    }
}