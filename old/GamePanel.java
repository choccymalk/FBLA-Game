package fbla.game;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.util.List;

public class GamePanel extends JPanel {
    private GameLogic gameLogic;
    
    public GamePanel(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Draw background
        if (gameLogic.getBackground() != null) {
            g.drawImage(gameLogic.getBackground(), 0, 0, 
                       this.getWidth(), this.getHeight(), this);
        }
        
        // Draw all entities
        List<GameEntity> entities = gameLogic.getEntityManager().getAllEntities();
        for (GameEntity entity : entities) {
            entity.render(g);
        }
    }
}