package fbla.game;

import com.google.gson.annotations.SerializedName;

import java.io.Serial;
import java.util.List;
/*
 * {"type": "npc", "x": 384, "y": 168, "dialogue_tree": {
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
                                            "npc_action": "end_conversation",
                                            "responses": []
                                        }
                                    },
                                    {
                                        "response_text": "bad",
                                        "next": {
                                            "npc_text": "sorry to hear that.",
                                            "npc_action": "end_conversation",
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
                                "npc_action": "end_conversation",
                                "responses": []
                            }
                        }
                    ]
                }
            }
 */

 public class dialogueTree {
    @SerializedName("npc_text")
    private String npcText;
    @SerializedName("responses")
    private List<Response> responses;
    @SerializedName("npc_action")
    private String npcAction;

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