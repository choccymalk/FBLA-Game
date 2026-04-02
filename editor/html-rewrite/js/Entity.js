class Entity {
    type;
    x;
    y;
    image;
    name;
    entityId;
    animation_states;
    dialogue_tree;
    constructor (type, x, y, imagePath, name, entityId, animationStates, dialogue_tree){
        this.type = type;
        this.x = x;
        this.y = y;
        this.image = imagePath;
        this.name = name;
        this.entityId = entityId;
        this.animation_states = animationStates;
        this.dialogue_tree = dialogue_tree;
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
    get dialogue_tree(){
        return this.dialogue_tree;
    }
}