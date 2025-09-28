package fbla.game;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class InputHandler implements KeyListener {
    private EntityManager entityManager;
    
    public InputHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        PlayerEntity player = entityManager.getPlayer();
        
        if (player != null) {
            switch (keyCode) {
                case KeyEvent.VK_UP:
                    player.setYVelocity(-GameConstants.PLAYER_MOVE_SPEED);
                    break;
                case KeyEvent.VK_DOWN:
                    player.setYVelocity(GameConstants.PLAYER_MOVE_SPEED);
                    break;
                case KeyEvent.VK_LEFT:
                    player.setXVelocity(-GameConstants.PLAYER_MOVE_SPEED);
                    break;
                case KeyEvent.VK_RIGHT:
                    player.setXVelocity(GameConstants.PLAYER_MOVE_SPEED);
                    break;
            }
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        PlayerEntity player = entityManager.getPlayer();
        
        if (player != null) {
            switch (keyCode) {
                case KeyEvent.VK_UP:
                    if (player.getYVelocity() < 0) {
                        player.setYVelocity(0);
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if (player.getYVelocity() > 0) {
                        player.setYVelocity(0);
                    }
                    break;
                case KeyEvent.VK_LEFT:
                    if (player.getXVelocity() < 0) {
                        player.setXVelocity(0);
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (player.getXVelocity() > 0) {
                        player.setXVelocity(0);
                    }
                    break;
            }
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
}