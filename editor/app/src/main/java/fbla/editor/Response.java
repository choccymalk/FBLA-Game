package fbla.editor;

import com.google.gson.annotations.SerializedName;

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
