class DialogueTree {
    npc_text;
    responses = [];
    npc_action;
    constructor (npc_text, responses, npc_action){
        this.npc_text = npc_text;
        this.responses = responses;
        this.npc_action = npc_action;
    }
    get getNpcText(){
        return this.npc_text
    }
    get getResponses(){
        return this.responses;
    }
    get getNpcAction(){
        return this.npc_action
    }
}