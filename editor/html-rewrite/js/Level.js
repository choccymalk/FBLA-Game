class Level {
    collision_grid;
    entities = {};
    background_image;
    doors;
    constructor (collision_grid, entities, background_image, doors){
        this.collision_grid = collision_grid;
        this.entities = entities;
        this.background_image = background_image;
        this.doors = doors;
    }
    get getCollisionGrid(){
        return this.collision_grid;
    }
    get getEntities(){
        return this.entities;
    }
    get getBackgroundImage(){
        return this.background_image;
    }
    get getDoors(){
        return this.doors;
    }
}