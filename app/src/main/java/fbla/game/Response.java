package fbla.game;

import com.google.gson.annotations.SerializedName;

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
public class Response {
    @SerializedName("response_text")
    private String responseText;
    @SerializedName("next")
    private dialogueTree next;

    public String getResponseText() {
        return responseText;
    }

    public dialogueTree getNextNode() {
        return next;
    }

    public boolean isThereANextNode(){
        if(next == null){
            return false;
        } else {
            return true;
        }
    }
}
