package fbla.game;

import java.awt.Point;
import java.util.List;

public class EntityAI {
    Entity entity;
    jsonParser parser;
    main main;
    boolean hasAlreadyForcedDialogueWithPlayer = false; // only used if ai is assigned package force_dialogue_with_player_on_level_start
    List<String> packages;
    boolean pathfinding = false;
    boolean inDialogueWithPlayer = false;
    Thread pathfindingThread;

    public EntityAI(Entity entity, jsonParser parser, main main) {
        this.entity = entity;
        this.parser = parser;
        this.main = main;
        packages = entity.getAIAbilities().getAllAbilities();
    }

    // update AI state to be called in main game loop
    public void tick(){
        // random chance to pathfind and move to a random point in the level
        if(Math.random() < 0.001 && packages.contains(new String("move_randomly")) && !pathfinding){
            pathfinding = true;
            // generate random point in level
            int goalX = (int)(Math.random() * 53);
            int goalY = (int)(Math.random() * 30);
            System.out.println("Entity AI: Pathfinding to random point (" + goalX + ", " + goalY + ")");
            // create new thread to handle pathfinding so we don't block the main thread
            pathfindingThread = new Thread(() -> { NPCPathfindToPoint(entity.getX() / 24, entity.getY() / 24, goalX, goalY, entity); });
            pathfindingThread.start();
        }
        if(packages.contains(new String("force_dialogue_with_player_on_level_start")) && !hasAlreadyForcedDialogueWithPlayer){
            hasAlreadyForcedDialogueWithPlayer = true;
            main.dialogueHandler(entity.getDialogueTree(), 0, entity);
            main.setCurrentDialogueTree(entity.getDialogueTree());
            main.setCurrentNPCPlayerIsInteractingWith(entity);
        }
    }

    // these two do the same thing, but are separated for clarity
    private int gridCoordinatesToWindowCoordinatesX(int coordx, int coordy){
        return coordx * 24;
    }
    // same as gridCoordinatesToWindowCoordinatesX but for y, separated for clarity
    private int gridCoordinatesToWindowCoordinatesY(int coordx, int coordy){
        return coordy * 24;
    }

    public int NPCPathfindToPoint(int startX, int startY, int goalX, int goalY, Entity npc){
        System.out.println("NPC Pathfinding from (" + startX + ", " + startY + ") to (" + goalX + ", " + goalY + ")");
        int[][] collisionGrid = main.getCollisionGrid();
        // determine what the index of the npc is in the entities list
        int npcIndex = main.getEntities().indexOf(npc);
        // build path with astar
        List<Point> wayPoints = AStar.findPath(main.getCollisionGrid(), startX, startY, goalX, goalY, 3, 5, main.getEntities().get(0).getX(), main.getEntities().get(0).getY());
        for(int i = 0; i < wayPoints.size(); i++){
            System.out.println("Waypoints: " + wayPoints.get(i).getX() + ", " + wayPoints.get(i).getY());
        }
        for(int i = 0; i < wayPoints.size(); i++){
            try {
                Thread.sleep(200);
                // determine what direction npc will move for this waypoint to update the npc's animation state
                // we can't do this, opengl calls need to be done on the main thread, so we will just set a variable and handle it in the main loop
                if(i > 0){
                    Point previousPoint = wayPoints.get(i - 1);
                    Point currentPoint = wayPoints.get(i);
                    if(currentPoint.getX() > previousPoint.getX()){
                        // reset movement states
                        //main.setEntityMovement(npcIndex, 0, 0); // right
                        //main.setEntityMovement(npcIndex, 1, 0); // up
                        //main.setEntityMovement(npcIndex, 2, 0); // left
                        //main.setEntityMovement(npcIndex, 3, 0); // down
                        // set new movement state
                        main.setEntityMovement(npcIndex, 0, 1); // moving right
                        //entityAnimation(npc, "walkingRight");
                    } else if(currentPoint.getX() < previousPoint.getX()){
                        // reset movement states
                        //main.setEntityMovement(npcIndex, 0, 0); // right
                        //main.setEntityMovement(npcIndex, 1, 0); // up
                        //main.setEntityMovement(npcIndex, 2, 0); // left
                        //main.setEntityMovement(npcIndex, 3, 0); // down
                        // set new movement state
                        main.setEntityMovement(npcIndex, 2, 1); // moving left
                        //entityAnimation(npc, "walkingLeft");
                    } else if(currentPoint.getY() > previousPoint.getY()){
                        // reset movement states
                        //main.setEntityMovement(npcIndex, 0, 0); // right
                        //main.setEntityMovement(npcIndex, 1, 0); // up
                        //main.setEntityMovement(npcIndex, 2, 0); // left
                        //main.setEntityMovement(npcIndex, 3, 0); // down
                        // set new movement state
                        main.setEntityMovement(npcIndex, 3, 1); // moving down
                        //entityAnimation(npc, "walkingDown");
                    } else if(currentPoint.getY() < previousPoint.getY()){
                        // reset movement states
                        //main.setEntityMovement(npcIndex, 0, 0); // right
                        //main.setEntityMovement(npcIndex, 1, 0); // up
                        //main.setEntityMovement(npcIndex, 2, 0); // left
                        //main.setEntityMovement(npcIndex, 3, 0); // down
                        // set new movement state
                        main.setEntityMovement(npcIndex, 1, 1); // moving up
                        //entityAnimation(npc, "walkingUp");
                    }
                }
                for(int a = 0; a <= 3-1; a++){
                    for(int j = 0; j <= 5-1; j++){
                        collisionGrid[(npc.getY() / 24) + j][(npc.getX() / 24) + a] = 0;
                    }
                }
                npc.setPosition(gridCoordinatesToWindowCoordinatesX((int)wayPoints.get(i).getX(), (int)wayPoints.get(i).getY()), gridCoordinatesToWindowCoordinatesY((int)wayPoints.get(i).getX(), (int)wayPoints.get(i).getY()));
                for(int a = 0; a <= 3-1; a++){
                    for(int j = 0; j <= 5-1; j++){
                        collisionGrid[(npc.getY() / 24) + j][(npc.getX() / 24) + a] = 1;
                    }
                }
                System.out.println("Waypoint reached: (" + npc.getX() + ", " + npc.getY() + "), window coordinates: (" + gridCoordinatesToWindowCoordinatesX((int)wayPoints.get(i).getX(), (int)wayPoints.get(i).getY()) + ", " + gridCoordinatesToWindowCoordinatesY((int)wayPoints.get(i).getX(), (int)wayPoints.get(i).getY()) + ")");
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        main.setCollisionGrid(collisionGrid);
        pathfinding = false;
        return 0;
    }

    
}
