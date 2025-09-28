package fbla.game;

public class PlayerEntity extends GameEntity {
    
    public PlayerEntity(int x, int y) {
        super(x, y, GameConstants.PLAYER_MOVE_SPEED, GameConstants.EntityType.PLAYER);
    }
    
    @Override
    public void update(int screenWidth, int screenHeight) {
        // Player movement is controlled by input, so we just update position
        updatePosition(screenWidth, screenHeight);
    }
    
    // Player-specific methods can be added here
    public void stopMovement() {
        setXVelocity(0);
        setYVelocity(0);
    }
}