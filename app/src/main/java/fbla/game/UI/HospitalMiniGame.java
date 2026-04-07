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

public class HospitalMiniGame {
    private final main game;
    private final ImGuiImplGl3 imguiGl3;
    private String scheduledItem1;
    private String scheduledItem2;
    private String scheduledItem3;
    private int playerScore = 0;
    private boolean showScheduledItem1 = true;
    private boolean showScheduledItem2 = true;
    private boolean showScheduledItem3 = true;
    private boolean showDropZone1 = true;
    private boolean showDropZone2 = true;
    private boolean showDropZone3 = true;
    private String itemDropZone1;
    private String itemDropZone2;
    private String itemDropZone3;
    private int textBoxTex;

    public HospitalMiniGame(main game, ImGuiImplGl3 imguiGl3, GameRenderer renderer) {
        this.game = game;
        this.imguiGl3 = imguiGl3;
        this.scheduledItem1 = "Evan B.\n1-2 P.M.";
        this.scheduledItem2 = "John A.,\n2-3 P.M.";
        this.scheduledItem3 = "George P.\n3-4 P.M.";
        this.textBoxTex = renderer.loadTexture(game.RESOURCE_PATH + "\\textures\\SpeechBox.png");
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
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0f, 0f, 0f, 0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0f, 0f, 0f, 0f);
        ImGui.pushStyleColor(ImGuiCol.Button, 0xFFFFFFFF);
        ImGui.pushStyleColor(ImGuiCol.Text, 0xFF000000);
        ImGui.begin("Hospital Minigame",
                ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse
                        | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoDecoration);

        if (showScheduledItem1) {
            ImGui.setCursorPosX(906f);
            ImGui.setCursorPosY(179f);
            ImGui.text(scheduledItem1);
            if (ImGui.beginDragDropSource(ImGuiDragDropFlags.SourceAllowNullID)) {
                // Pass the identifier of the image (e.g., path or string ID)
                ImGui.setDragDropPayload("DND_TEXT_TYPE", scheduledItem1, ImGuiCond.None);
                ImGui.text("Dragging Item...");
                ImGui.endDragDropSource();
            }
        }

        if (showScheduledItem2) {
            ImGui.setCursorPosX(906f);
            ImGui.setCursorPosY(282f);
            ImGui.text(scheduledItem2);
            if (ImGui.beginDragDropSource(ImGuiDragDropFlags.SourceAllowNullID)) {
                // Pass the identifier of the image (e.g., path or string ID)
                ImGui.setDragDropPayload("DND_TEXT_TYPE", scheduledItem2, ImGuiCond.None);
                ImGui.text("Dragging Item...");
                ImGui.endDragDropSource();
            }
        }

        if (showScheduledItem3) {
            ImGui.setCursorPosX(906f);
            ImGui.setCursorPosY(376f);
            ImGui.text(scheduledItem3);
            if (ImGui.beginDragDropSource(ImGuiDragDropFlags.SourceAllowNullID)) {
                // Pass the identifier of the image (e.g., path or string ID)
                ImGui.setDragDropPayload("DND_TEXT_TYPE", scheduledItem3, ImGuiCond.None);
                ImGui.text("Dragging Item...");
                ImGui.endDragDropSource();
            }
        }

        if (showDropZone1) {
            ImGui.setCursorPosX(326f);
            ImGui.setCursorPosY(238f);
            ImGui.pushStyleColor(ImGuiCol.Button, 0x00FFFFFF);
            ImGui.button(" ", 397, 115);
            if (ImGui.beginDragDropTarget()) {
                String payload = ImGui.acceptDragDropPayload("DND_TEXT_TYPE", String.class);
                // String payload = ImGui.acceptDragDropPayload("DND_TEXT_TYPE", String.class);
                if (payload != null) {
                    // Handle dropped image (payload contains myTextureId)
                    System.out.println("Item dropped!");
                    System.out.println(payload);
                    if (payload.equals(scheduledItem2)) {
                        playerScore++;
                        System.out.println("player got one point");
                        showScheduledItem2 = false;
                    } else if (payload.equals(scheduledItem1)) {
                        showScheduledItem1 = false;
                    } else if (payload.equals(scheduledItem3)) {
                        showScheduledItem3 = false;
                    } else {
                        System.out.println("drop zone 1 is in an unfortunate state");
                    }
                    showDropZone1 = false;
                    itemDropZone1 = payload;
                }
                ImGui.endDragDropTarget();
            }
            ImGui.popStyleColor();
        } else {
            ImGui.setCursorPosX(332f);
            ImGui.setCursorPosY(245f);
            ImGui.text(itemDropZone1);
        }

        if (showDropZone2) {
            ImGui.setCursorPosX(326f);
            ImGui.setCursorPosY(368f);
            ImGui.pushStyleColor(ImGuiCol.Button, 0x00FFFFFF);
            ImGui.button(" ", 397, 115);
            if (ImGui.beginDragDropTarget()) {
                String payload = ImGui.acceptDragDropPayload("DND_TEXT_TYPE", String.class);
                // String payload = ImGui.acceptDragDropPayload("DND_TEXT_TYPE", String.class);
                if (payload != null) {
                    // Handle dropped image (payload contains myTextureId)
                    System.out.println("Item dropped!");
                    System.out.println(payload);
                    if (payload.equals(scheduledItem3)) {
                        playerScore++;
                        System.out.println("player got one point");
                        showScheduledItem3 = false;
                    } else if (payload.equals(scheduledItem1)) {
                        showScheduledItem1 = false;
                    } else if (payload.equals(scheduledItem2)) {
                        showScheduledItem2 = false;
                    } else {
                        System.out.println("drop zone 2 is in an unfortunate state");
                    }
                    showDropZone2 = false;
                    itemDropZone2 = payload;
                }
                ImGui.endDragDropTarget();
            }
            ImGui.popStyleColor();
        } else {
            ImGui.setCursorPosX(332f);
            ImGui.setCursorPosY(376f);
            ImGui.text(itemDropZone2);
        }

        if (showDropZone3) {
            ImGui.setCursorPosX(326f);
            ImGui.setCursorPosY(496f);
            ImGui.pushStyleColor(ImGuiCol.Button, 0x00FFFFFF);
            ImGui.button(" ", 397, 115);
            if (ImGui.beginDragDropTarget()) {
                String payload = ImGui.acceptDragDropPayload("DND_TEXT_TYPE", String.class);
                // String payload = ImGui.acceptDragDropPayload("DND_TEXT_TYPE", String.class);
                if (payload != null) {
                    // Handle dropped image (payload contains myTextureId)
                    System.out.println("Item dropped!");
                    System.out.println(payload);
                    if (payload.equals(scheduledItem3)) {
                        playerScore++;
                        System.out.println("player got one point");
                        showScheduledItem3 = false;
                    } else if (payload.equals(scheduledItem2)) {
                        showScheduledItem2 = false;
                    } else if (payload.equals(scheduledItem1)) {
                        showScheduledItem1 = false;
                    } else {
                        System.out.println("drop zone 3 is in an unfortunate state");
                    }
                    showDropZone3 = false;
                    itemDropZone3 = payload;
                }
                ImGui.endDragDropTarget();
            }
            ImGui.popStyleColor();
        } else {
            ImGui.setCursorPosX(332f);
            ImGui.setCursorPosY(510f);
            ImGui.text(itemDropZone3);
        }

        if (!showDropZone1 && !showDropZone2 && !showDropZone3) {
            game.setHospitalMiniGameScore(playerScore);
            ImGui.image(textBoxTex, 776, 228);
            ImGui.setCursorPosX(183f);
            ImGui.setCursorPosY(526f);
            ImGui.text("You got " + Integer.toString(playerScore) + " points");
            ImGui.pushStyleColor(ImGuiCol.Button, 0xFF000000);
            ImGui.setCursorPosX(183f);
            ImGui.setCursorPosY(546f);
            ImGui.pushStyleColor(ImGuiCol.Text, 0xFFFFFFFF);
            if (ImGui.button("Done", 70, 40)) {
                game.setCurrentGameState(GameState.IN_GAME);
            }
            ImGui.popStyleColor();
            ImGui.popStyleColor();
        }

        ImGui.setCursorPos(226, 279);
        ImGui.text("1-2 P.M.");
        ImGui.setCursorPos(226, 412);
        ImGui.text("2-3 P.M.");
        ImGui.setCursorPos(226, 547);
        ImGui.text("3-4 P.M.");

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