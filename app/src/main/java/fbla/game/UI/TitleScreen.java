package fbla.game.UI;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;
import imgui.gl3.ImGuiImplGl3;
import fbla.game.main;

public class TitleScreen {
    private final main game;
    private final ImGuiImplGl3 imguiGl3;

    public TitleScreen(main game, ImGuiImplGl3 imguiGl3) {
        this.game = game;
        this.imguiGl3 = imguiGl3;
    }

    /**
     * Render the title screen UI
     * 
     * @param winW Window width
     * @param winH Window height
     */
    public void render(int winW, int winH) {
        ImGui.setNextWindowPos(winW * 0.05f, winH * 0.4f);
        ImGui.setNextWindowSize(winW / 2, 330);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0f, 0f, 0f, 0f);
        ImGui.pushStyleColor(ImGuiCol.Button, 0f, 0f, 0f, 1f);
        ImGui.begin("Main Menu", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoDecoration);

        String[] titleScreenOptions = { "Start Game", "Options", "Exit" };
        for (int i = 0; i < titleScreenOptions.length; i++) {
            if (ImGui.button(titleScreenOptions[i], (winW / 2) - 20, 100)) {
                game.handleTitleScreenOption(i);
            }
        }

        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleColor();
        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }
}
