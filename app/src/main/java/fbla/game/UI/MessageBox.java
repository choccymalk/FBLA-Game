package fbla.game.UI;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import fbla.game.main;

public class MessageBox {
    private final main game;
    private final ImGuiImplGl3 imguiGl3;

    public MessageBox(main game, ImGuiImplGl3 imguiGl3) {
        this.game = game;
        this.imguiGl3 = imguiGl3;
    }

    /**
     * Render the message box UI for NPC dialogue
     * 
     * @param winW Window width
     * @param winH Window height
     */
    public void render(int winW, int winH) {
        ImGui.setNextWindowPos(winW / 2 - 300, winH - 250);
        ImGui.setNextWindowSize(600, 200);
        ImGui.begin(game.currentNPC.getName(),
                ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);

        ImGui.textWrapped(game.currentFullMessage);
        ImGui.separator();

        try {
            if (!game.messageBoxOptionsDisplayed) {
                if (ImGui.button("Close (E)", 100, 30)) {
                    game.closeMessage();
                }
            } else if (game.currentResponseOptions != null) {
                for (int i = 0; i < game.currentResponseOptions.length; i++) {
                    if (ImGui.button(game.currentResponseOptions[i], 500, 30)) {
                        game.selectedResponseIndex = i;
                        game.dialogueHandler(game.currentTree, i + 1, game.currentNPC);
                    }
                }
            } else {
                if (ImGui.button("Close (E)", 100, 30)) {
                    game.closeMessage();
                }
            }
        } catch (Exception e) {
            if (ImGui.button("Close (E)", 100, 30)) {
                game.closeMessage();
            }
        }

        ImGui.end();
        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }
}