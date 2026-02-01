package fbla.editor;

import com.google.gson.annotations.SerializedName;

import java.io.Serial;
import java.util.List;
/*
 * "dialogue_tree": {
                    "npc_text": "hello",
                    "responses": [
                        {
                            "response_text": "hi",
                            "next": {
                                "npc_text": "how are you?",
                                "responses": [
                                    {
                                        "response_text": "good",
                                        "next": {
                                            "npc_text": "great to hear!",
                                            "responses": []
                                        }
                                    },
                                    {
                                        "response_text": "bad",
                                        "next": {
                                            "npc_text": "sorry to hear that.",
                                            "responses": []
                                        }
                                    }
                                ]
                            }
                        },
                        {
                            "response_text": "bye",
                            "next": {
                                "npc_text": "goodbye!",
                                "npc_action": "move_to(35,19)",
                                "responses": []
                            }
                        }
                    ]
                }
 */

 public class dialogueTree {
    @SerializedName("npc_text")
    private String npcText;
    @SerializedName("responses")
    private List<Response> responses;
    @SerializedName("npc_action")
    private String npcAction;

    public dialogueTree(boolean doesntmatter){
        List<Response> emptyResponse = List.of();
        this.npcText = "";
        this.npcAction = "";
        this.responses = emptyResponse;
    }

    public dialogueTree() {}

    public String getNpcText() {
        return npcText;
    }

    public List<Response> getResponses() {
        return responses;
    }

    public String getNpcAction() {
        return npcAction;
    }

    public boolean isTreeEmpty(){
        return npcText == null && (responses == null || responses.isEmpty()) && npcAction == null;
    }
}