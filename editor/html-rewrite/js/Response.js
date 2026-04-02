class Response {
    response_text;
    next;
    constructor (response_text, next){
        this.response_text = response_text;
        if(next == null){
            this.next = "";
        } else {
            this.next = next;
        }
    }
    get getResponseText(){
        return this.response_text;
    }
    get getNextTree(){
        return this.next;
    }
}