package fbla.game;

import javax.swing.Timer;
import java.io.IOException;

public class GameMain {
    private WindowManager windowManager;
    private GameLogic gameLogic;
    private GamePanel gamePanel;
    private InputHandler inputHandler;
    private Timer gameTimer;
    
    public GameMain() {
        initializeGame();
    }
    
    private void initializeGame() {
        // Create core components
        windowManager = new WindowManager();
        gameLogic = new GameLogic(windowManager);
        gamePanel = new GamePanel(gameLogic);
        inputHandler = new InputHandler(gameLogic.getEntityManager());
        
        // Set up the window
        windowManager.setGamePanel(gamePanel);
        gamePanel.addKeyListener(inputHandler);
        
        // Create and start the game timer
        gameTimer = new Timer(GameConstants.TIMER_DELAY, e -> {
            gameLogic.update();
            gamePanel.repaint();
        });
    }
    
    public void start() {
        windowManager.show();
        gameTimer.start();
    }
    
    // Method to add more NPCs during gameplay
    public void addRandomNPCs(int count) {
        gameLogic.getEntityManager().createRandomNPCs(count);
    }
    
    public static void main(String[] args) throws IOException {
        GameMain game = new GameMain();
        game.start();
        
        // Example: Add more NPCs after 5 seconds
        Timer delayTimer = new Timer(5000, e -> {
            game.addRandomNPCs(3);
            System.out.println("Added 3 more NPCs!");
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }
}