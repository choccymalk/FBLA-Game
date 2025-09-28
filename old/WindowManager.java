package fbla.game;

import javax.swing.*;
import java.awt.*;

public class WindowManager {
    private JFrame frame;
    private GamePanel gamePanel;
    
    public WindowManager() {
        initializeWindow();
    }
    
    private void initializeWindow() {
        frame = new JFrame(GameConstants.WINDOW_TITLE);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
    }
    
    public void setGamePanel(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        frame.add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.requestFocusInWindow();
    }
    
    public void show() {
        frame.pack();
        frame.setVisible(true);
    }
    
    public JFrame getFrame() {
        return frame;
    }
    
    public int getWidth() {
        return gamePanel != null ? gamePanel.getWidth() : GameConstants.DEFAULT_WIDTH;
    }
    
    public int getHeight() {
        return gamePanel != null ? gamePanel.getHeight() : GameConstants.DEFAULT_HEIGHT;
    }
}