package fbla.game.UI;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import fbla.game.main;
import fbla.game.main.GameState;

public class OptionsMenu {
    private final main game;
    private final ImGuiImplGl3 imguiGl3;

    public OptionsMenu(main game, ImGuiImplGl3 imguiGl3) {
        this.game = game;
        this.imguiGl3 = imguiGl3;
    }

    /**
     * Render the options menu UI
     * 
     * @param winW                    Window width
     * @param winH                    Window height
     * @param lastStateForOptionsMenu Last game state before opening options
     * @param drawDebugGrid           Reference to debug grid drawing flag
     */
    public void render(int winW, int winH, GameState lastStateForOptionsMenu, boolean[] drawDebugGrid) {
        ImGui.setNextWindowPos(winW / 2 - 150, winH / 2 - 100);
        ImGui.setNextWindowSize(300, 330);
        ImGui.begin("Options", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);

        ImGui.dragFloat("Volume", game.optionsVolume, 1f, 0.0f, 100.0f, "%.0f");
        if (ImGui.button("Test Volume", 200, 40)) {
            game.playTalkingSound();
        }

        ImGui.dragFloat("Framerate", game.optionsFrameRate, 1f, 1.0f, 60.0f, "%.0f");
        ImGui.dragFloat("Update Rate", game.optionsUpdateRate, 1f, 1.0f, 60.0f, "%.0f");
        
        if (ImGui.checkbox("Fullscreen", game.getIsFullscreen())) {
            game.fstoggle.setFullscreen(game.getIsFullscreen().get());
            System.out.println(game.getIsFullscreen().get());
        }
        
        if (ImGui.checkbox("Draw Debug Grid", game.getShouldDebugGridBeDrawn())) {
            drawDebugGrid[0] = game.getShouldDebugGridBeDrawn().get();
            game.DRAW_DEBUG_GRID = game.getShouldDebugGridBeDrawn().get();
        }

        if (ImGui.button("Back", 200, 40)) {
            game.setCurrentGameState(lastStateForOptionsMenu);
        }

        game.soundPlayerVolume = (int) game.optionsVolume[0];
        game.FRAMERATE = (int) game.optionsFrameRate[0];
        game.UPDATE_RATE = (int) game.optionsUpdateRate[0];

        ImGui.end();
        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }
}