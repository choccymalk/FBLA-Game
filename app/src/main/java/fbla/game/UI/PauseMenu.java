package fbla.game.UI;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import fbla.game.main;
import fbla.game.main.GameState;

public class PauseMenu {
    private final main game;
    private final ImGuiImplGl3 imguiGl3;

    public PauseMenu(main game, ImGuiImplGl3 imguiGl3) {
        this.game = game;
        this.imguiGl3 = imguiGl3;
    }

    /**
     * Render the pause menu UI
     * 
     * @param winW Window width
     * @param winH Window height
     * @param lastStateForOptionsMenu Last game state before opening options
     */
    public void render(int winW, int winH, GameState lastStateForOptionsMenu) {
        ImGui.setNextWindowPos(winW / 2 - 150, winH / 2 - 100);
        ImGui.setNextWindowSize(300, 400);
        ImGui.begin("Paused", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);

        ImGui.text("Game Paused");
        ImGui.separator();

        if (ImGui.button("Resume", 200, 40)) {
            game.setCurrentGameState(main.GameState.IN_GAME);
        }
        if (ImGui.button("Save", 200, 40)) {
            game.saveCurrentGame(game, "quicksave");
        }
        if (ImGui.button("Load", 200, 40)) {
            game.loadGameFromFile(game, "quicksave");
        }
        if (ImGui.button("Options", 200, 40)) {
            game.setCurrentGameState(main.GameState.OPTIONS);
        }
        if (ImGui.button("To Title Screen", 200, 40)) {
            game.setCurrentGameState(main.GameState.TITLE_SCREEN);
            
        }

        ImGui.end();
        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }
}