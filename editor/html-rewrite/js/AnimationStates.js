class AnimationStates {
    idle = [];
    walking_up = [];
    walking_down = [];
    walking_left = [];
    walking_right = [];
    constructor (idle, walking_up, walking_down, walking_left, walking_right){
        this.idle = idle;
        this.walking_up = walking_up;
        this.walking_down = walking_down;
        this.walking_left = walking_left;
        this.walking_right = walking_right;
    }
    get getidle(){
        return this.idle;
    }
    get getwalking_up(){
        return this.walking_up;
    }
    get getwalking_down(){
        return this.walking_down;
    }
    get getwalking_left(){
        return this.walking_left;
    }
    get getwalking_right(){
        return this.walking_right;
    }
}