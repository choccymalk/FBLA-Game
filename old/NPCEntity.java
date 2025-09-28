package fbla.game;

import java.util.Random;

public class NPCEntity extends GameEntity {
    private Random random;
    private int directionChangeCounter;
    private int directionChangeInterval;
    
    public NPCEntity(int x, int y) {
        super(x, y, GameConstants.NPC_MOVE_SPEED, GameConstants.EntityType.NPC);
        this.random = new Random();
        this.directionChangeInterval = 60 + random.nextInt(120); // Change direction every 1-3 seconds
        this.directionChangeCounter = 0;
        
        // Start with random initial direction
        changeDirection();
    }
    
    @Override
    public void update(int screenWidth, int screenHeight) {
        directionChangeCounter++;
        
        // Change direction periodically or when hitting a wall
        if (directionChangeCounter >= directionChangeInterval || 
            isAtBoundary(screenWidth, screenHeight)) {
            changeDirection();
            directionChangeCounter = 0;
            directionChangeInterval = 60 + random.nextInt(120);
        }
        
        updatePosition(screenWidth, screenHeight);
    }
    
    private void changeDirection() {
        // Choose random direction: 0=stop, 1=up, 2=down, 3=left, 4=right, 5-8=diagonal
        int direction = random.nextInt(9);
        
        switch (direction) {
            case 0: // Stop
                setXVelocity(0);
                setYVelocity(0);
                break;
            case 1: // Up
                setXVelocity(0);
                setYVelocity(-maxSpeed);
                break;
            case 2: // Down
                setXVelocity(0);
                setYVelocity(maxSpeed);
                break;
            case 3: // Left
                setXVelocity(-maxSpeed);
                setYVelocity(0);
                break;
            case 4: // Right
                setXVelocity(maxSpeed);
                setYVelocity(0);
                break;
            case 5: // Up-Left
                setXVelocity(-maxSpeed);
                setYVelocity(-maxSpeed);
                break;
            case 6: // Up-Right
                setXVelocity(maxSpeed);
                setYVelocity(-maxSpeed);
                break;
            case 7: // Down-Left
                setXVelocity(-maxSpeed);
                setYVelocity(maxSpeed);
                break;
            case 8: // Down-Right
                setXVelocity(maxSpeed);
                setYVelocity(maxSpeed);
                break;
        }
    }
    
    private boolean isAtBoundary(int screenWidth, int screenHeight) {
        return x <= 0 || y <= 0 || x >= screenWidth - width || y >= screenHeight - height;
    }
}