package fbla.game;

import com.google.gson.annotations.SerializedName;

import fbla.game.Renderer.Object3D;

import java.util.List;

public class Level {
    @SerializedName("collision_grid")
    private int[][] collisionGrid;

    @SerializedName("entities")
    private List<Entity> entities;

    @SerializedName("background_image")
    private String backgroundImage;

    @SerializedName("doors")
    private List<Door> doors;

    @SerializedName("3d_objects")
    private List<Object3D> objects3d;

    // Gson requires a no-arg constructor or it will use reflection; leaving none is fine.

    public int[][] getCollisionGrid() {
        return collisionGrid;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public List<Door> getDoors() {
        return doors;
    }

    public void setEntities(List<Entity> entities){
        this.entities = entities;
    }

    public void set3DObjectsList(List<Object3D> objects){
        this.objects3d = objects;
    }

    public void appendItemTo3DObjectsList(Object3D object){
        this.objects3d.add(object);
    }

    public List<Object3D> getObject3DList(){
        return this.objects3d;
    }

    public void removePlayerEntityFromLevel(){
        for (Entity entity : entities) {
            if(entity.getType().equals("player")){
                entities.remove(entity);
            }
        }
    }

    public Entity getPlayerEntityFromLevel(){
        for (Entity entity : entities) {
            if(entity.getType().equals("player")){
                return entity;
            }
        }
        return null;
    }

    public void setDoors(List<Door> doors){
        this.doors = doors;
    }

}

