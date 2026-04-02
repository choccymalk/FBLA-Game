class Door {
    x;
    y;
    target_level;
    target_x;
    target_y;
    image;
    constructor (x, y, target_level, target_x, target_y, image){
        this.x = x;
        this.y = y;
        this.target_level = target_level;
        this.target_x = target_x;
        this.target_y = target_y;
        this.image = image;
    }
    get getX(){
        return this.x;
    }
    get getY(){
        return this.y;
    }
    get getTargetLevel(){
        return this.target_level;
    }
    get getTargetX(){
        return this.target_x;
    }
    get getTargetY(){
        return this.target_y;
    }
    get getImage(){
        return this.image;
    }
}