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

public class SchoolMiniGame {
    private final main game;
    private final ImGuiImplGl3 imguiGl3;
    private int grade1Tex;
    private int grade2Tex;
    private int grade3Tex;
    private int gradingPapersFullTex;
    private int playerScore = 0;
    private boolean showGrade1 = true;
    private boolean showGrade2 = true;
    private boolean showGrade3 = true;
    private boolean showDropZone1 = true;
    private boolean showDropZone2 = true;
    private boolean showDropZone3 = true;
    private int gradeDroppedZone1Tex;
    private int gradeDroppedZone2Tex;
    private int gradeDroppedZone3Tex;

    public SchoolMiniGame(main game, ImGuiImplGl3 imguiGl3, GameRenderer renderer) {
        this.game = game;
        this.imguiGl3 = imguiGl3;
        this.grade1Tex = renderer.loadTexture(game.RESOURCE_PATH + "\\textures\\GradingPapersGrade1.png");
        this.grade2Tex = renderer.loadTexture(game.RESOURCE_PATH + "\\textures\\GradingPapersGrade2.png");
        this.grade3Tex = renderer.loadTexture(game.RESOURCE_PATH + "\\textures\\GradingPapersGrade3.png");
        this.gradingPapersFullTex = renderer.loadTexture(game.RESOURCE_PATH + "\\textures\\GradingPapersNoGrade.png");
    }

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
        ImGui.begin("Grading Papers Minigame",
                ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse
                        | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoDecoration);

        if (showGrade1) {
            ImGui.setCursorPosX(469f);
            ImGui.setCursorPosY(550f);
            ImGui.image(grade1Tex, 90, 80);
            if (ImGui.beginDragDropSource(ImGuiDragDropFlags.SourceAllowNullID)) {
                // Pass the identifier of the image (e.g., path or string ID)
                ImGui.setDragDropPayload("TEXTURE_ID", grade1Tex, ImGuiCond.None);
                // ImGui.setDragDropPayload("DND_TEXT_TYPE", "unique drag me");
                ImGui.text("Dragging Image...");
                ImGui.endDragDropSource();
            }
        }

        if (showGrade2) {
            ImGui.setCursorPosX(264f);
            ImGui.setCursorPosY(540f);
            ImGui.image(grade2Tex, 90, 80);
            if (ImGui.beginDragDropSource(ImGuiDragDropFlags.SourceAllowNullID)) {
                // Pass the identifier of the image (e.g., path or string ID)
                ImGui.setDragDropPayload("TEXTURE_ID", grade2Tex, ImGuiCond.None);
                // ImGui.setDragDropPayload("DND_TEXT_TYPE", "unique drag me");
                ImGui.text("Dragging Image...");
                ImGui.endDragDropSource();
            }
        }

        if (showGrade3) {
            ImGui.setCursorPosX(938f);
            ImGui.setCursorPosY(533f);
            ImGui.image(grade3Tex, 90, 80);
            if (ImGui.beginDragDropSource(ImGuiDragDropFlags.SourceAllowNullID)) {
                // Pass the identifier of the image (e.g., path or string ID)
                ImGui.setDragDropPayload("TEXTURE_ID", grade3Tex, ImGuiCond.None);
                // ImGui.setDragDropPayload("DND_TEXT_TYPE", "unique drag me");
                ImGui.text("Dragging Image...");
                ImGui.endDragDropSource();
            }
        }

        if (showDropZone1) {
            ImGui.setCursorPosX(180f);
            ImGui.setCursorPosY(60f);
            ImGui.pushStyleColor(ImGuiCol.Button, 0x00FFFFFF);
            ImGui.button(" ", 220, 346);
            if (ImGui.beginDragDropTarget()) {
                Object payload = ImGui.acceptDragDropPayload("TEXTURE_ID");
                // String payload = ImGui.acceptDragDropPayload("DND_TEXT_TYPE", String.class);
                if (payload != null) {
                    // Handle dropped image (payload contains myTextureId)
                    System.out.println("Image dropped!");
                    System.out.println(payload.toString());
                    if (payload.toString().equals(Integer.toString(grade1Tex))) {
                        playerScore++;
                        System.out.println("player got one point");
                        showGrade1 = false;
                    } else if (payload.toString().equals(Integer.toString(grade2Tex))) {
                        showGrade2 = false;
                    } else if (payload.toString().equals(Integer.toString(grade3Tex))) {
                        showGrade3 = false;
                    } else {
                        System.out.println("drop zone 1 is in an unfortunate state");
                    }
                    showDropZone1 = false;
                    gradeDroppedZone1Tex = Integer.parseInt(payload.toString());
                }
                ImGui.endDragDropTarget();
            }
            ImGui.popStyleColor();
        } else {
            ImGui.setCursorPosX(180f);
            ImGui.setCursorPosY(60f);
            ImGui.image(gradeDroppedZone1Tex, 45, 40);
        }

        if (showDropZone2) {
            ImGui.setCursorPosX(505f);
            ImGui.setCursorPosY(60f);
            ImGui.pushStyleColor(ImGuiCol.Button, 0x00FFFFFF);
            ImGui.button(" ", 220, 346);
            if (ImGui.beginDragDropTarget()) {
                Object payload = ImGui.acceptDragDropPayload("TEXTURE_ID");
                // String payload = ImGui.acceptDragDropPayload("DND_TEXT_TYPE", String.class);
                if (payload != null) {
                    // Handle dropped image (payload contains myTextureId)
                    System.out.println("Image dropped!");
                    System.out.println(payload.toString());
                    if (payload.toString().equals(Integer.toString(grade2Tex))) {
                        playerScore++;
                        System.out.println("player got one point");
                        showGrade2 = false;
                    } else if (payload.toString().equals(Integer.toString(grade3Tex))) {
                        showGrade3 = false;
                    } else if (payload.toString().equals(Integer.toString(grade1Tex))) {
                        showGrade1 = false;
                    } else {
                        System.out.println("drop zone 2 is in an unfortunate state");
                    }
                    showDropZone2 = false;
                    gradeDroppedZone2Tex = Integer.parseInt(payload.toString());
                }
                ImGui.endDragDropTarget();
            }
            ImGui.popStyleColor();
        } else {
            ImGui.setCursorPosX(505f);
            ImGui.setCursorPosY(60f);
            ImGui.image(gradeDroppedZone2Tex, 45, 40);
        }

        if (showDropZone3) {
            ImGui.setCursorPosX(834f);
            ImGui.setCursorPosY(60f);
            ImGui.pushStyleColor(ImGuiCol.Button, 0x00FFFFFF);
            ImGui.button(" ", 220, 346);
            if (ImGui.beginDragDropTarget()) {
                Object payload = ImGui.acceptDragDropPayload("TEXTURE_ID");
                // String payload = ImGui.acceptDragDropPayload("DND_TEXT_TYPE", String.class);
                if (payload != null) {
                    // Handle dropped image (payload contains myTextureId)
                    System.out.println("Image dropped!");
                    System.out.println(payload.toString());
                    if (payload.toString().equals(Integer.toString(grade3Tex))) {
                        playerScore++;
                        System.out.println("player got one point");
                        showGrade3 = false;
                    } else if (payload.toString().equals(Integer.toString(grade2Tex))) {
                        showGrade2 = false;
                    } else if (payload.toString().equals(Integer.toString(grade1Tex))) {
                        showGrade1 = false;
                    } else {
                        System.out.println("drop zone 3 is in an unfortunate state");
                    }
                    showDropZone3 = false;
                    gradeDroppedZone3Tex = Integer.parseInt(payload.toString());
                }
                ImGui.endDragDropTarget();
            }
            ImGui.popStyleColor();
        } else {
            ImGui.setCursorPosX(834f);
            ImGui.setCursorPosY(60f);
            ImGui.image(gradeDroppedZone3Tex, 45, 40);
        }

        if (!showDropZone1 && !showDropZone2 && !showDropZone3) {
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
