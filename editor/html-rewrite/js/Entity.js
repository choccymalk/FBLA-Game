class Entity {
    type;
    x;
    y;
    image;
    name;
    entityId;
    animation_states;
    constructor (type, x, y, imagePath, name, entityId, animationStates){
        this.type = type;
        this.x = x;
        this.y = y;
        this.image = imagePath;
        this.name = name;
        this.entityId = entityId;
        this.animation_states = animationStates;
    }
    get getType(){
        return this.type;
    }
    get getX(){
        return this.x;
    }
    get getY(){
        return this.y;
    }
    get getImagePath(){
        return this.image;
    }
    get getEntityId(){
        return this.entityId;
    }
    get getAnimationStates(){
        return this.animation_states;
    }
}