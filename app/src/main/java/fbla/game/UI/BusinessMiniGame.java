package fbla.game.UI;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiDragDropFlags;
import imgui.flag.ImGuiCond;
import imgui.gl3.ImGuiImplGl3;
import fbla.game.GameRenderer;
import fbla.game.main;
import fbla.game.main.GameState;

public class BusinessMiniGame {
    private final main game;
    private final ImGuiImplGl3 imguiGl3;
    private String[] playerChoices;
    private String[] screen0Choices = {"Grandmas", "Clowns", "Fish"};
    private String[] screen1Choices = {"Phones", "Books", "Instruments"};
    private String[] screen2Choices = {"Door to Door", "Online", "Brick and Mortar"};
    private int currentScreen = 0;
    private int screen0Tex;
    private int screen1Tex;
    private int screen2Tex;
    
    public BusinessMiniGame(main game, ImGuiImplGl3 imguiGl3, GameRenderer renderer) {
        this.game = game;
        this.imguiGl3 = imguiGl3;
        this.screen0Tex = renderer.loadTexture(game.RESOURCE_PATH + "\\textures\\OfficeGameProduct2.png");
        this.screen1Tex = renderer.loadTexture(game.RESOURCE_PATH + "\\textures\\OfficeGameAudience2.png");
        this.screen2Tex = renderer.loadTexture(game.RESOURCE_PATH + "\\textures\\OfficeGameTypeOfSale2.png");
    }
    // TODO: change cursor positions
    /**
     * Render the minigame UI
     * 
     * @param winW Window width
     * @param winH Window height
     */
    public void render(int winW, int winH) {
        ImGui.setNextWindowPos(0f, 0f);
        ImGui.setNextWindowSize(winW, winH);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 0f);
        ImGui.pushStyleColor(ImGuiCol.Button, 0x00000000);
        ImGui.pushStyleColor(ImGuiCol.Text, 0xFF000000);
        ImGui.begin("Business Minigame",
                ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse
                        | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoDecoration);
        
        if(currentScreen == 0){
            ImGui.setCursorPos(0, 0);
            ImGui.image(screen0Tex, winW, winH);
            ImGui.setCursorPos(300, 300);
            if(ImGui.button(screen0Choices[0], 100, 100)){
                playerChoices[currentScreen] = screen0Choices[0];
                currentScreen++;
            }
            ImGui.setCursorPos(400, 300);
            if(ImGui.button(screen0Choices[1], 100, 100)){
                playerChoices[currentScreen] = screen0Choices[1];
                currentScreen++;
            }
            ImGui.setCursorPos(500, 300);
            if(ImGui.button(screen0Choices[2], 100, 100)){
                playerChoices[currentScreen] = screen0Choices[2];
                currentScreen++;
            }
        }

        if(currentScreen == 1){
            ImGui.setCursorPos(0, 0);
            ImGui.image(screen1Tex, winW, winH);
            ImGui.setCursorPos(300, 300);
            ImGui.setCursorPos(300, 300);
            if(ImGui.button(screen1Choices[0], 100, 100)){
                playerChoices[currentScreen] = screen1Choices[0];
                currentScreen++;
            }
            ImGui.setCursorPos(400, 300);
            if(ImGui.button(screen1Choices[1], 100, 100)){
                playerChoices[currentScreen] = screen1Choices[1];
                currentScreen++;
            }
            ImGui.setCursorPos(500, 300);
            if(ImGui.button(screen1Choices[2], 100, 100)){
                playerChoices[currentScreen] = screen1Choices[2];
                currentScreen++;
            }
        }

        if(currentScreen == 2){
            ImGui.setCursorPos(0, 0);
            ImGui.image(screen2Tex, winW, winH);
            ImGui.setCursorPos(300, 300);
            ImGui.setCursorPos(300, 300);
            if(ImGui.button(screen2Choices[0], 100, 100)){
                playerChoices[currentScreen] = screen2Choices[0];
                currentScreen++;
            }
            ImGui.setCursorPos(400, 300);
            if(ImGui.button(screen2Choices[1], 100, 100)){
                playerChoices[currentScreen] = screen2Choices[1];
                currentScreen++;
            }
            ImGui.setCursorPos(500, 300);
            if(ImGui.button(screen2Choices[2], 100, 100)){
                playerChoices[currentScreen] = screen2Choices[2];
                currentScreen++;
            }
        }

        if (playerChoices.length == 3) {
            game.setBusinessMiniGameChoices(playerChoices);
            game.setCurrentGameState(GameState.IN_GAME);
        }

        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleColor();
        ImGui.popStyleColor();
        ImGui.popStyleColor();
        ImGui.popStyleColor();
        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }
}
