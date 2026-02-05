package fbla.editor;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.flag.ImGuiWindowFlags;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.type.ImString;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.ImGui.*;
import imgui.flag.*;
import imgui.flag.ImGuiWindowFlags.*;
import imgui.ImGuiIO.*;
import imgui.ImGuiIO;
import imgui.ImFontConfig;
import imgui.ImFontGlyphRangesBuilder;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.ImGuiStyle;
import imgui.ImVec4;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBDXT;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.BufferUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fbla.editor.jsonParser;
import fbla.editor.Level;
import fbla.editor.Entity;
import fbla.editor.Door;

public class UI {

    // UI state
    private ImString selectedEntityType = new ImString("npc", 256);
    private ImString entityName = new ImString("placeholder", 256);
    private ImString entityImage = new ImString("npc.png");
    private ImFloat entityX = new ImFloat(0);
    private ImFloat entityY = new ImFloat(0);
    private int selectedEntityIndex = -1;
    private boolean showCollisionGrid = true;
    private boolean showEntities = true;
    private boolean showDoors = true;
    private boolean show3DObjects = true;

    private ImString object3DName = new ImString("3d object");
    private ImString object3DModelPath = new ImString();
    private ImString objectTexturePath = new ImString();
    // C:\Users\Bentley\Desktop\FBLA-Game\game_resources\textures\fish_texture.png
    // C:\Users\Bentley\Desktop\FBLA-Game\game_resources\models\fish.obj
    private ImFloat objectX = new ImFloat(0);
    private ImFloat objectY = new ImFloat(0);
    private ImFloat objectZ = new ImFloat(0);
    private ImFloat objectScaleX = new ImFloat(1);
    private ImFloat objectScaleY = new ImFloat(1);
    private ImFloat objectScaleZ = new ImFloat(1);
    private ImFloat objectRotationX = new ImFloat(0);
    private ImFloat objectRotationY = new ImFloat(0);
    private ImFloat objectRotationZ = new ImFloat(0);

    // Door creation state
    private ImString doorTargetLevel = new ImString("0", 256);
    private ImString doorTargetX = new ImString("0", 256);
    private ImString doorTargetY = new ImString("0", 256);
    private ImString doorImage = new ImString("door.png");

    // Grid editing state
    private ImBoolean paintMode = new ImBoolean(false);
    private ImBoolean eraserMode = new ImBoolean(false);

    private boolean showDialogueEditor = false;
    private boolean showAnimationEditor = false;
    private boolean showEntityEditor = false;
    private int editingEntityIndex = -1;
    private dialogueTree editingDialogueNode = null;
    private List<dialogueTree> dialogueNodeStack = new ArrayList<>();
    private ImString dialogueNpcText = new ImString("", 512);
    private ImString dialogueNpcAction = new ImString("", 256);
    private ImString dialogueResponseText = new ImString("", 256);
    private int editingResponseIndex = -1;
    private Map<String, Integer> entityAnimationStateTextures = new HashMap<>();
    private ImString newFramePathIdle = new ImString();
    private ImString newFramePathWalkingUp = new ImString();
    private ImString newFramePathWalkingLeft = new ImString();
    private ImString newFramePathWalkingRight = new ImString();
    private ImString newFramePathWalkingDown = new ImString();
    private ImString newEntityType = new ImString();
    private ImString newEntityName = new ImString();
    private ImFloat newDoorTargetLevel = new ImFloat();

    private ImString newObject3DName = new ImString();
    private ImFloat newObjectX = new ImFloat(0);
    private ImFloat newObjectY = new ImFloat(0);
    private ImFloat newObjectZ = new ImFloat(0);
    private ImFloat newObjectScaleX = new ImFloat(1);
    private ImFloat newObjectScaleY = new ImFloat(1);
    private ImFloat newObjectScaleZ = new ImFloat(1);
    private ImFloat newObjectRotationX = new ImFloat(0);
    private ImFloat newObjectRotationY = new ImFloat(0);
    private ImFloat newObjectRotationZ = new ImFloat(0);

    private int editingObject3DIndex = -1;
    private boolean showObject3DEditor = false;

    private String RESOURCE_PATH = LevelEditor.RESOURCE_PATH;

    private LevelEditor editor;

    private ImFloat cameraPosTransformX = new ImFloat();
    private ImFloat cameraPosTransformY = new ImFloat();
    private ImFloat cameraPosTransformZ = new ImFloat();

    public UI(LevelEditor editor) {
        this.editor = editor;
    }

    public boolean getPaintMode() {
        return paintMode.get();
    }

    public boolean getEraserMode() {
        return eraserMode.get();
    }

    public boolean getShowDoors() {
        return this.showDoors;
    }

    public boolean getShowEntities() {
        return this.showEntities;
    }

    public boolean getShowCollisionGrid() {
        return this.showCollisionGrid;
    }

    private void openDialogueEditor(int entityIndex) {
        editingEntityIndex = entityIndex;
        Entity entity = editor.getEntities().get(entityIndex);
        dialogueTree tree = entity.getDialogueTree();

        if (tree == null) {
            tree = new dialogueTree();
            editor.setPrivateField(entity, "dialogueTree", tree);
        }

        editingDialogueNode = tree;
        dialogueNodeStack.clear();
        dialogueNodeStack.add(tree);
        showDialogueEditor = true;
        updateDialogueInputs();
    }

    private void updateDialogueInputs() {
        if (editingDialogueNode != null) {
            dialogueNpcText.set(editingDialogueNode.getNpcText() != null ? editingDialogueNode.getNpcText() : "");
            dialogueNpcAction.set(editingDialogueNode.getNpcAction() != null ? editingDialogueNode.getNpcAction() : "");
        }
        dialogueResponseText.set("");
        editingResponseIndex = -1;
    }

    public void renderUI() {
        // Main control panel
        ImGui.setNextWindowPos(10, 20, 0);
        ImGui.setNextWindowSize(400, 1045, 0);

        ImGui.begin("Level Editor");

        // Level info
        ImGui.text("Level " + editor.getCurrentLevelIndex() + " of " + (editor.getLevels().size() - 1));
        ImGui.separator();

        // Navigation
        if (ImGui.button("< Prev", 90, 30)) {
            if (editor.getCurrentLevelIndex() > 0)
                editor.loadLevel(editor.getCurrentLevelIndex() - 1);
        }
        ImGui.sameLine();
        if (ImGui.button("Next >", 90, 30)) {
            if (editor.getCurrentLevelIndex() < editor.getLevels().size() - 1)
                editor.loadLevel(editor.getCurrentLevelIndex() + 1);
        }
        ImGui.sameLine();
        if (ImGui.button("Save", 90, 30)) {
            editor.saveAllLevels();
            editor.saveAllObject3Ds();
        }
        ImGui.sameLine();
        ImGui.text("Autosave Off");

        // Add new level button (Level 2 TODO)
        if (ImGui.button("New Level", 90, 30)) {
            editor.addNewLevel();
        }
        ImGui.sameLine();
        if (ImGui.button("Delete Level", 90, 30)) {
            editor.deleteCurrentLevel();
        }

        ImGui.separator();

        // Background image section (Level 2 TODO)
        ImGui.text("Background Image:");
        ImGui.inputText("Image Path##bg", editor.getBackgroundImagePath());
        ImGui.sameLine();
        if (ImGui.button("Load##bg", 50, 20)) {
            String bgPath = editor.getBackgroundImagePath().get();
            if (!bgPath.isEmpty()) {
                loadBackgroundImage(bgPath);
            }
        }
        if (ImGui.button("Clear##bg", 50, 20)) {
            editor.getBackgroundImagePath().set("");
            editor.getRenderer().setBackgroundImageTextureId(-1);
            editor.setPrivateField(editor.getLevels().get(editor.getCurrentLevelIndex()), "backgroundImage", "");
        }

        ImGui.separator();

        // Grid tools
        ImGui.text("Grid Editing:");
        ImGui.checkbox("Paint", paintMode);
        ImGui.sameLine();
        ImGui.checkbox("Erase", eraserMode);

        if (paintMode.get() && eraserMode.get()) {
            eraserMode.set(false);
        }

        ImGui.separator();

        // Visibility
        ImGui.text("Show:");
        if (ImGui.checkbox("Collision", new ImBoolean(showCollisionGrid))) {
            showCollisionGrid = !showCollisionGrid;
        }
        if (ImGui.checkbox("Entities", new ImBoolean(showEntities))) {
            showEntities = !showEntities;
        }
        if (ImGui.checkbox("Doors", new ImBoolean(showDoors))) {
            showDoors = !showDoors;
        }
        if (ImGui.checkbox("3D Objects", new ImBoolean(show3DObjects))) {
            show3DObjects = !show3DObjects;
        }

        ImGui.separator();

        // Add entity
        ImGui.text("Add Entity:");
        ImGui.inputText("Type##ent", selectedEntityType);
        ImGui.inputText("Name##ent", entityName);
        ImGui.inputFloat("X##ent", entityX);
        ImGui.inputFloat("Y##ent", entityY);
        ImGui.inputText("Entity Image##ent", entityImage);
        if (ImGui.button("Add Entity##btn", 150, 25)) {
            editor.addEntity((int) entityX.get(), (int) entityY.get(), selectedEntityType.get(), entityName.get(),
                    entityImage.get());
            entityX.set(0f);
            entityY.set(0f);
        }

        ImGui.separator();

        // Add door
        ImGui.text("Add Door:");
        ImGui.inputText("Target Lvl", doorTargetLevel);
        ImGui.inputText("Target X", doorTargetX);
        ImGui.inputText("Target Y", doorTargetY);
        ImGui.inputText("Door Image##ent", doorImage);
        if (ImGui.button("Add Door##btn", 150, 25)) {
            try {
                int targetLvl = Integer.parseInt(doorTargetLevel.get());
                int targetX = Integer.parseInt(doorTargetX.get());
                int targetY = Integer.parseInt(doorTargetY.get());
                editor.addDoor((int) entityX.get(), (int) entityY.get(), targetLvl, targetX, targetY, doorImage.get());
            } catch (NumberFormatException e) {
                System.err.println("Invalid door values");
            }
        }

        ImGui.separator();

        // Entity list with edit buttons
        ImGui.text("Entities (" + editor.getEntities().size() + "):");
        for (int i = 0; i < editor.getEntities().size(); i++) {
            Entity e = editor.getEntities().get(i);
            String label = "[" + i + "] " + e.getType() + " @" + e.getX() + "," + e.getY();
            ImGui.text(label);
            ImGui.sameLine();
            if (ImGui.button("D##dial" + i, 25, 20)) {
                openDialogueEditor(i);
            }
            ImGui.sameLine();
            if (ImGui.button("A##anim" + i, 25, 20)) {
                openAnimationEditor(i);
            }
            ImGui.sameLine();
            if (ImGui.button("E##anim" + i, 25, 20)) {
                openEntityEditor(i);
            }
            ImGui.sameLine();
            if (ImGui.button("X##ent" + i, 25, 20)) {
                editor.clearEntityFromCollisionGrid(editor.getEntities().get(i));
                editor.getEntities().remove(i);
                break;
            }
        }

        ImGui.separator();

        // Door list
        ImGui.text("Doors (" + editor.getDoors().size() + "):");
        for (int i = 0; i < editor.getDoors().size(); i++) {
            Door d = editor.getDoors().get(i);
            String label = "[" + i + "] L" + d.getTargetLevel() + " @" + d.getX() + "," + d.getY();
            ImGui.text(label);
            ImGui.sameLine();
            if (ImGui.button("X##door" + i, 25, 20)) {
                editor.getDoors().remove(i);
                break;
            }
        }

        ImGui.end();

        // Main control panel
        ImGui.setNextWindowPos(415, 750, 0);
        ImGui.setNextWindowSize(1272, 315, 0);

        // 3d objects section

        ImGui.begin("3D Objects");

        // Add entity
        ImGui.text("Add 3D Object:");
        ImGui.inputText("Name##ent", object3DName);
        ImGui.inputFloat("X##ent", objectX);
        ImGui.inputFloat("Y##ent", objectY);
        ImGui.inputFloat("Z##ent", objectZ);
        ImGui.inputFloat("Scale X##ent", objectScaleX);
        ImGui.inputFloat("Scale Y##ent", objectScaleY);
        ImGui.inputFloat("Scale Z##ent", objectScaleZ);
        ImGui.inputFloat("Rotation X##ent", objectRotationX);
        ImGui.inputFloat("Rotation Y##ent", objectRotationY);
        ImGui.inputFloat("Rotation Z##ent", objectRotationZ);
        ImGui.inputText("Object Texture (Relative to " + RESOURCE_PATH + "\\textures\\)" + "##ent", objectTexturePath);
        ImGui.inputText("Object Model (Relative to " + RESOURCE_PATH + "\\models\\)" + "##ent", object3DModelPath);
        if (ImGui.button("Add Object##btn", 150, 25)) {
            int textureId = editor.getRenderer().loadTexture(RESOURCE_PATH + "\\textures\\" + objectTexturePath.get());
            editor.add3DObject(objectX.get(), objectY.get(), objectZ.get(), objectScaleX.get(), objectScaleY.get(),
                    objectScaleZ.get(), objectRotationX.get(), objectRotationY.get(), objectRotationZ.get(),
                    object3DName.get(), object3DModelPath.get(), textureId, objectTexturePath.get());

        }

        ImGui.separator();

        // Entity list with edit buttons
        if (editor.getObject3ds() != null) {
            ImGui.text("3D Objects (" + editor.getObject3ds().size() + "):");
            for (int i = 0; i < editor.getObject3ds().size(); i++) {
                Object3D o3d = editor.getObject3ds().get(i);
                String label = "[" + i + "] " + o3d.getName() + " @" + o3d.getX() + "," + o3d.getY() + "," + o3d.getZ();
                ImGui.text(label);
                ImGui.sameLine();
                if (ImGui.button("O##dial" + i, 25, 20)) {
                    openObject3DEditor(i);
                }
                ImGui.sameLine();
                if (ImGui.button("X##ent" + i, 25, 20)) {
                    editor.getObject3ds().remove(o3d);
                    editor.getLevels().get(editor.getCurrentLevelIndex()).getObject3DList().remove(o3d);
                    break;
                }
            }
        }

        ImGui.end();

        ImGui.setNextWindowPos(1702, 20, 0);
        ImGui.setNextWindowSize(200, 700, 0);

        ImGui.begin("Camera Position");

        if(ImGui.button("camera pos x add 0.5")){
            editor.getRenderer().setCameraPos(editor.getRenderer().getCameraPosX() + 0.5f, editor.getRenderer().getCameraPosY(), editor.getRenderer().getCameraPosZ());
        }

        if(ImGui.button("camera pos x sub 0.5")){
            editor.getRenderer().setCameraPos(editor.getRenderer().getCameraPosX() - 0.5f, editor.getRenderer().getCameraPosY(), editor.getRenderer().getCameraPosZ());
        }

        if(ImGui.button("camera pos y add 0.5")){
            editor.getRenderer().setCameraPos(editor.getRenderer().getCameraPosX(), editor.getRenderer().getCameraPosY() + 0.5f, editor.getRenderer().getCameraPosZ());
        }

        if(ImGui.button("camera pos y sub 0.5")){
            editor.getRenderer().setCameraPos(editor.getRenderer().getCameraPosX(), editor.getRenderer().getCameraPosY() - 0.5f, editor.getRenderer().getCameraPosZ());
        }

        if(ImGui.button("camera pos z add 0.5")){
            editor.getRenderer().setCameraPos(editor.getRenderer().getCameraPosX(), editor.getRenderer().getCameraPosY(), editor.getRenderer().getCameraPosZ() + 0.5f);
        }

        if(ImGui.button("camera pos z sub 0.5")){
            editor.getRenderer().setCameraPos(editor.getRenderer().getCameraPosX(), editor.getRenderer().getCameraPosY(), editor.getRenderer().getCameraPosZ() - 0.5f);
        }

        if(ImGui.button("camera pos reset to 0")){
            editor.getRenderer().setCameraPos(0, 0, 0);
        }

        ImGui.text("X: " + editor.getRenderer().getCameraPosX());
        ImGui.text("Y: " + editor.getRenderer().getCameraPosY());
        ImGui.text("Z: " + editor.getRenderer().getCameraPosZ());

        ImGui.text("Camera will be fixed at");
        ImGui.text("(0,0,10) in game");

        if(ImGui.button("camera rot x add 1")){
            editor.getRenderer().setCameraRot(editor.getRenderer().getCameraRotX() + 1f, editor.getRenderer().getCameraRotY(), editor.getRenderer().getCameraRotZ());
        }

        if(ImGui.button("camera rot x sub 1")){
            editor.getRenderer().setCameraRot(editor.getRenderer().getCameraRotX() - 1f, editor.getRenderer().getCameraRotY(), editor.getRenderer().getCameraRotZ());
        }

        if(ImGui.button("camera rot y add 1")){
            editor.getRenderer().setCameraRot(editor.getRenderer().getCameraRotX(), editor.getRenderer().getCameraRotY() + 1f, editor.getRenderer().getCameraRotZ());
        }

        if(ImGui.button("camera rot y sub 1")){
            editor.getRenderer().setCameraRot(editor.getRenderer().getCameraRotX(), editor.getRenderer().getCameraRotY() - 1f, editor.getRenderer().getCameraRotZ());
        }

        if(ImGui.button("camera rot z add 1")){
            editor.getRenderer().setCameraRot(editor.getRenderer().getCameraRotX(), editor.getRenderer().getCameraRotY(), editor.getRenderer().getCameraRotZ() + 1f);
        }

        if(ImGui.button("camera rot z sub 1")){
            editor.getRenderer().setCameraRot(editor.getRenderer().getCameraRotX(), editor.getRenderer().getCameraRotY(), editor.getRenderer().getCameraRotZ() - 1f);
        }

        if(ImGui.button("camera rot reset to 0")){
            editor.getRenderer().setCameraRot(0, 0, 0);
        }

        ImGui.text("X Rot: " + editor.getRenderer().getCameraRotX());
        ImGui.text("Y Rot: " + editor.getRenderer().getCameraRotY());
        ImGui.text("Z Rot: " + editor.getRenderer().getCameraRotZ());
        
        ImGui.end();

        // Render dialogue editor window if open
        if (showDialogueEditor) {
            renderDialogueEditor();
        }

        // Render animation editor window if open
        if (showAnimationEditor) {
            renderAnimationEditor();
        }

        if (showEntityEditor) {
            renderEntityEditior();
        }

        if(showObject3DEditor){
            renderObject3DEditor();
        }

    }

    private void renderDialogueEditor() {
        ImGui.setNextWindowPos(650, 20, 0);
        ImGui.setNextWindowSize(730, 860, 0);

        if (!ImGui.begin("Dialogue Editor", new ImBoolean(true))) {
            ImGui.end();
            return;
        }

        if (editingEntityIndex >= 0 && editingEntityIndex < editor.getEntities().size()) {
            Entity entity = editor.getEntities().get(editingEntityIndex);
            ImGui.text("Editing: " + entity.getType());
            ImGui.separator();

            // Navigation breadcrumb
            ImGui.text("Path: Root");
            for (int i = 1; i < dialogueNodeStack.size(); i++) {
                ImGui.sameLine();
                ImGui.text(" > Response " + i);
            }
            ImGui.separator();

            // Current node editor
            ImGui.text("NPC Text:");
            ImGui.inputTextMultiline("##npcText", dialogueNpcText, 700, 80);

            ImGui.text("NPC Action (optional):");
            ImGui.inputText("##npcAction", dialogueNpcAction);
            ImGui.sameLine();
            ImGui.textDisabled("(?)");
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("e.g., move_to(10,20) or attack(enemy)");
            }

            // Update the node if user changed values
            if (editingDialogueNode != null) {
                String npcText = dialogueNpcText.get();
                String npcAction = dialogueNpcAction.get();

                if (!npcText.isEmpty()) {
                    editor.setPrivateField(editingDialogueNode, "npcText", npcText);
                }
                if (!npcAction.isEmpty()) {
                    editor.setPrivateField(editingDialogueNode, "npcAction", npcAction);
                }
            }

            ImGui.separator();

            // Responses list
            ImGui.text("Responses:");
            List<Response> responses = editingDialogueNode.getResponses();

            if (responses == null) {
                responses = new ArrayList<>();
                editor.setPrivateField(editingDialogueNode, "responses", responses);
            }

            for (int i = 0; i < responses.size(); i++) {
                Response r = responses.get(i);
                // if it is empty, user can't actually add any new frames
                // TODO: fix user being unable add animation frames to a new entity, level 1
                ImGui.text("[" + i + "] " + (r.getResponseText() != null ? r.getResponseText() : "(empty)"));
                ImGui.sameLine();
                if (ImGui.button("Edit##resp" + i, 50, 20)) {
                    editingResponseIndex = i;
                    dialogueResponseText.set(r.getResponseText() != null ? r.getResponseText() : "");
                }
                ImGui.sameLine();
                if (ImGui.button("Go##resp" + i, 40, 20)) {
                    if (r.getNextNode() != null) {
                        dialogueNodeStack.add(r.getNextNode());
                        editingDialogueNode = r.getNextNode();
                        updateDialogueInputs();
                    }
                }
                ImGui.sameLine();
                if (ImGui.button("Del##resp" + i, 40, 20)) {
                    responses.remove(i);
                    break;
                }
            }

            ImGui.separator();

            // Add new response
            ImGui.text("Add Response:");
            ImGui.inputText("Response Text", dialogueResponseText);
            if (ImGui.button("Add Response##btn", 150, 25)) {
                String responseText = dialogueResponseText.get();
                if (!responseText.isEmpty()) {
                    Response newResponse = new Response();
                    editor.setPrivateField(newResponse, "responseText", responseText);

                    dialogueTree nextNode = new dialogueTree();
                    editor.setPrivateField(nextNode, "responses", new ArrayList<>());
                    editor.setPrivateField(newResponse, "next", nextNode);

                    responses.add(newResponse);
                    dialogueResponseText.set("");
                }
            }

            ImGui.separator();

            // Navigation buttons
            if (dialogueNodeStack.size() > 1) {
                if (ImGui.button("Back", 100, 25)) {
                    dialogueNodeStack.remove(dialogueNodeStack.size() - 1);
                    editingDialogueNode = dialogueNodeStack.get(dialogueNodeStack.size() - 1);
                    updateDialogueInputs();
                }
            }

            if (ImGui.button("Save & Close", 100, 25)) {
                showDialogueEditor = false;
                editingEntityIndex = -1;
                editingDialogueNode = null;
                dialogueNodeStack.clear();
            }
        }

        ImGui.end();
    }

    private void openAnimationEditor(int entityIndex) {
        editingEntityIndex = entityIndex;
        showAnimationEditor = true;
    }

    private void renderAnimationEditor() {
        ImGui.setNextWindowPos(650, 20, 0);
        ImGui.setNextWindowSize(730, 860, 0);

        if (!ImGui.begin("Animation Editor", new ImBoolean(true))) {
            ImGui.end();
            return;
        }

        if (editingEntityIndex >= 0 && editingEntityIndex < editor.getEntities().size()) {
            Entity entity = editor.getEntities().get(editingEntityIndex);
            animationStates animStates = entity.getAnimationStates();

            ImGui.text("Editing: " + entity.getType());
            ImGui.separator();

            if (animStates == null) {
                ImGui.text("No animation states defined for this entity");
            } else {
                // Edit each animation state
                editAnimationStateList("Idle", animStates.getIdleImagesPaths(), entity, "idle");
                ImGui.spacing();
                editAnimationStateList("Walking Up", animStates.getWalkingUpImagesPaths(), entity, "walking_up");
                ImGui.spacing();
                editAnimationStateList("Walking Down", animStates.getWalkingDownImagesPaths(), entity, "walking_down");
                ImGui.spacing();
                editAnimationStateList("Walking Left", animStates.getWalkingLeftImagesPaths(), entity, "walking_left");
                ImGui.spacing();
                editAnimationStateList("Walking Right", animStates.getWalkingRightImagesPaths(), entity,
                        "walking_right");
            }

            ImGui.separator();

            if (ImGui.button("Close", 100, 25)) {
                showAnimationEditor = false;
                editingEntityIndex = -1;
            }
        }

        ImGui.end();
    }

    private void openEntityEditor(int entityIndex) {
        editingEntityIndex = entityIndex;
        showEntityEditor = true;
    }

    public void renderEntityEditior() {
        ImGui.setNextWindowPos(650, 20, 0);
        ImGui.setNextWindowSize(500, 500, 0);

        if (!ImGui.begin("Entity Editor", new ImBoolean(true))) {
            ImGui.end();
            return;
        }

        if (editingEntityIndex >= 0 && editingEntityIndex < editor.getEntities().size()) {
            Entity entity = editor.getEntities().get(editingEntityIndex);
            if (entity.getName() == null) {
                ImGui.text("Editing: " + entity.getType());
                // newEntityName.set("No name defined");
            } else {
                ImGui.text("Editing: " + entity.getName());
                // newEntityName.set(entity.getName());
            }
            ImGui.separator();

            ImGui.inputText("Name", newEntityName);
            ImGui.sameLine();
            if (ImGui.button("Set Name")) {
                entity.setName(newEntityName.get());
                System.out.println(newEntityName.get());
                newEntityName.clear();
            }

            if (ImGui.button("Save & Close", 100, 25)) {
                showEntityEditor = false;
                editingEntityIndex = -1;
            }
        }

        ImGui.end();
    }

    private void openObject3DEditor(int object3DIndex){
        editingObject3DIndex = object3DIndex;
        showObject3DEditor = true;
    }

    public void renderObject3DEditor(){
        ImGui.setNextWindowPos(650, 20, 0);
        ImGui.setNextWindowSize(500, 500, 0);

        if (!ImGui.begin("3d Object Editor", new ImBoolean(true))) {
            ImGui.end();
            return;
        }

        if (editingObject3DIndex >= 0 && editingObject3DIndex < editor.getObject3ds().size()) {
            Object3D o3d = editor.getObject3ds().get(editingObject3DIndex);
            System.out.println(editor.getObject3ds().get(editingObject3DIndex).getName());
            if (o3d.getName() == null) {
                ImGui.text("Editing: undefined name");
            } else {
                ImGui.text("Editing: " + o3d.getName());
            }
            ImGui.separator();

            ImGui.inputText("Name", newObject3DName);
            ImGui.sameLine();
            if (ImGui.button("Set Name")) {
                o3d.setName(newObject3DName.get());
                newObject3DName.clear();
            }
            ImGui.inputFloat("Pos X", newObjectX);
            ImGui.sameLine();
            if (ImGui.button("Set Pos X")) {
                editor.getRenderer().getRenderer3d().move3DObject(o3d, newObjectX.get(), o3d.getY(), o3d.getZ());
            }
            ImGui.inputFloat("Pos Y", newObjectY);
            ImGui.sameLine();
            if (ImGui.button("Set Pos Y")) {
                editor.getRenderer().getRenderer3d().move3DObject(o3d, o3d.getX(), newObjectY.get(), o3d.getZ());
            }
            ImGui.inputFloat("Pos Z", newObjectZ);
            ImGui.sameLine();
            if (ImGui.button("Set Pos Z")) {
                editor.getRenderer().getRenderer3d().move3DObject(o3d, o3d.getX(), o3d.getY(), newObjectZ.get());
            }
            ImGui.inputFloat("Rot X", newObjectRotationX);
            ImGui.sameLine();
            if (ImGui.button("Set Rot X")) {
                editor.getRenderer().getRenderer3d().rotate3DObject(o3d, newObjectRotationX.get(), o3d.getRotationY(), o3d.getRotationZ());
            }
            ImGui.inputFloat("Rot Y", newObjectRotationY);
            ImGui.sameLine();
            if (ImGui.button("Set Rot Y")) {
                editor.getRenderer().getRenderer3d().rotate3DObject(o3d, o3d.getRotationX(), newObjectRotationY.get(), o3d.getRotationZ());
            }
            ImGui.inputFloat("Rot Z", newObjectRotationZ);
            ImGui.sameLine();
            if (ImGui.button("Set Rot Z")) {
                editor.getRenderer().getRenderer3d().rotate3DObject(o3d, o3d.getRotationX(), o3d.getRotationY(), newObjectRotationZ.get());
            }
            ImGui.inputFloat("Scale X", newObjectScaleX);
            ImGui.sameLine();
            if (ImGui.button("Set Scale X")) {
                editor.getRenderer().getRenderer3d().scale3DObject(o3d, newObjectScaleX.get(), o3d.getScaleY(), o3d.getScaleZ());
            }
            ImGui.inputFloat("Scale Y", newObjectScaleY);
            ImGui.sameLine();
            if (ImGui.button("Set Scale Y")) {
                editor.getRenderer().getRenderer3d().scale3DObject(o3d, o3d.getScaleX(), newObjectScaleY.get(), o3d.getScaleZ());
            }
            ImGui.inputFloat("Scale Z", newObjectScaleZ);
            ImGui.sameLine();
            if (ImGui.button("Set Scale Z")) {
                editor.getRenderer().getRenderer3d().scale3DObject(o3d, o3d.getScaleX(), o3d.getScaleY(), newObjectScaleZ.get());
            }

            if (ImGui.button("Save & Close", 100, 25)) {
                showObject3DEditor = false;
                editingObject3DIndex = -1;
            }
        }

        ImGui.end();
    }

    private void editAnimationStateList(String stateName, List<String> imagePaths, Entity entity, String stateKey) {
        ImGui.text(stateName + ":");
        ImGui.indent();
        if (imagePaths == null) {
            ImGui.text("(No frames, imagePaths is null)");
        } else if (imagePaths.isEmpty()) {
            ImGui.text("(No frames, imagePaths is empty)");
        } else {
            enumerateAnimationStateImagesAndPaths(imagePaths, stateKey);
        }

        // Add new frame
        switch (stateKey) {
            case "idle":
                ImGui.inputText("##addFrame" + stateKey, newFramePathIdle);
                ImGui.sameLine();
                if (ImGui.button("Add Frame##" + stateKey, 100, 20)) {
                    if (!newFramePathIdle.get().isEmpty()) {
                        System.out.println("value of framePath: " + newFramePathIdle.get());
                        imagePaths.add(newFramePathIdle.get());
                        enumerateAnimationStateImagesAndPaths(imagePaths, stateKey);
                    }
                }
                break;
            case "walking_up":
                ImGui.inputText("##addFrame" + stateKey, newFramePathWalkingUp);
                ImGui.sameLine();
                if (ImGui.button("Add Frame##" + stateKey, 100, 20)) {
                    if (!newFramePathWalkingUp.get().isEmpty()) {
                        System.out.println("value of framePath: " + newFramePathWalkingUp.get());
                        imagePaths.add(newFramePathWalkingUp.get());
                        enumerateAnimationStateImagesAndPaths(imagePaths, stateKey);
                    }
                }
                break;
            case "walking_down":
                ImGui.inputText("##addFrame" + stateKey, newFramePathWalkingDown);
                ImGui.sameLine();
                if (ImGui.button("Add Frame##" + stateKey, 100, 20)) {
                    if (!newFramePathWalkingDown.get().isEmpty()) {
                        System.out.println("value of framePath: " + newFramePathWalkingDown.get());
                        imagePaths.add(newFramePathWalkingDown.get());
                        enumerateAnimationStateImagesAndPaths(imagePaths, stateKey);
                    }
                }
                break;
            case "walking_left":
                ImGui.inputText("##addFrame" + stateKey, newFramePathWalkingLeft);
                ImGui.sameLine();
                if (ImGui.button("Add Frame##" + stateKey, 100, 20)) {
                    if (!newFramePathWalkingLeft.get().isEmpty()) {
                        System.out.println("value of framePath: " + newFramePathWalkingLeft.get());
                        imagePaths.add(newFramePathWalkingLeft.get());
                        enumerateAnimationStateImagesAndPaths(imagePaths, stateKey);
                    }
                }
                break;
            case "walking_right":
                ImGui.inputText("##addFrame" + stateKey, newFramePathWalkingRight);
                ImGui.sameLine();
                if (ImGui.button("Add Frame##" + stateKey, 100, 20)) {
                    if (!newFramePathWalkingRight.get().isEmpty()) {
                        System.out.println("value of framePath: " + newFramePathWalkingRight.get());
                        imagePaths.add(newFramePathWalkingRight.get());
                        enumerateAnimationStateImagesAndPaths(imagePaths, stateKey);
                    }
                }
                break;

        }

        ImGui.unindent();
    }

    private void enumerateAnimationStateImagesAndPaths(List<String> imagePaths, String stateKey) {
        for (int i = 0; i < imagePaths.size(); i++) {
            String imagePath = RESOURCE_PATH + "/textures/" + imagePaths.get(i);
            ImGui.text("[" + i + "] " + imagePath);
            if (entityAnimationStateTextures.containsKey(imagePath)) {
                ImGui.image(entityAnimationStateTextures.get(imagePath), 96, 144);
            } else {
                entityAnimationStateTextures.put(imagePath, editor.getRenderer().loadTexture(imagePath));
                ImGui.image(entityAnimationStateTextures.get(imagePath), 96, 144);
            }
            ImGui.sameLine();
            if (ImGui.button("X##anim" + stateKey + i, 25, 20)) {
                imagePaths.remove(i);
                break;
            }
        }
    }

    // Load background image from file path
    void loadBackgroundImage(String imagePath) {
        try {
            String oldImagePath = imagePath;
            imagePath = RESOURCE_PATH + "\\textures\\" + imagePath;
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                System.err.println("Background image file not found: " + imagePath);
                return;
            }

            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                System.err.println("Failed to read image file: " + imagePath);
                return;
            }

            // Crop if necessary
            if (image.getWidth() > 1280 || image.getHeight() > 720) {
                BufferedImage croppedImage = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = croppedImage.createGraphics();
                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();
                image = croppedImage;
                System.out.println("Background image was larger than 1280x720 and has been cropped to fit.");
            }

            // Convert BufferedImage to OpenGL texture
            editor.getRenderer().setBackgroundImageTextureId(createTextureFromBufferedImage(image));
            editor.setPrivateField(editor.getLevels().get(editor.getCurrentLevelIndex()), "backgroundImage",
                    oldImagePath);

        } catch (IOException e) {
            System.err.println("Error loading background image: " + e.getMessage());
        }
    }

    // Convert BufferedImage to OpenGL texture ID
    private int createTextureFromBufferedImage(BufferedImage img) {
        int[] pixels = new int[img.getWidth() * img.getHeight()];
        img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
        ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int pixel = pixels[y * img.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }
        buffer.flip();

        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, img.getWidth(), img.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        return texId;
    }

}