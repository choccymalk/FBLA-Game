package fbla.game;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.awt.Point;
import java.util.*;

public class AStar {
    // a star pathfinding for the entity's move_to command in dialogue, to make it look like the entity is moving on its own
    // overkill but it's fun

    private static final int[][] DIRS = {
        { 0, -1 }, // up
        { 1,  0 }, // right
        { 0,  1 }, // down
        {-1,  0 }  // left
    };

    private static int startX;
    private static int startY;

    /**
     * Finds a path for a rectangular entity of size objWidth×objHeight.
     * startX/Y and goalX/Y refer to the top-left corner of that rectangle.
     */
    public static List<Point> findPath(
        int[][] grid,
        int startXPos, int startYPos,
        int goalX,  int goalY,
        int objWidth, int objHeight,
        int playerPositionX, int playerPositionY
    ) {
        int rows = grid.length, cols = grid[0].length;

        startX = startXPos;
        startY = startYPos;

        // Quick reject if start or goal footprint collides immediately
        if (!isAreaFree(grid, startX, startY, objWidth, objHeight, playerPositionX, playerPositionY)) return Collections.emptyList();
        if (!isAreaFree(grid, goalX,  goalY,  objWidth, objHeight, playerPositionX, playerPositionY))  return Collections.emptyList();

        boolean[][] closed = new boolean[rows][cols];
        Node[][] nodes   = new Node[rows][cols];

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        Node start = new Node(startX, startY, null, 0, heuristic(startX, startY, goalX, goalY));
        nodes[startY][startX] = start;
        open.add(start);

        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (cur.x == goalX && cur.y == goalY) {
                return buildPath(cur);
            }

            closed[cur.y][cur.x] = true;

            for (int[] d : DIRS) {
                int nx = cur.x + d[0], ny = cur.y + d[1];

                // Bounds check for the rectangle footprint
                if (nx < 0 || ny < 0
                 || nx + objWidth  > cols
                 || ny + objHeight > rows) continue;

                // Collision check for full footprint
                if (!isAreaFree(grid, nx, ny, objWidth, objHeight, playerPositionX, playerPositionY)
                  || closed[ny][nx]) continue;

                int gNew = cur.g + 1;
                Node neighbor = nodes[ny][nx];

                if (neighbor == null) {
                    neighbor = new Node(nx, ny, cur, gNew,
                                        heuristic(nx, ny, goalX, goalY));
                    nodes[ny][nx] = neighbor;
                    open.add(neighbor);

                } else if (gNew < neighbor.g) {
                    // Better path found—reorder in open
                    open.remove(neighbor);
                    neighbor.g      = gNew;
                    neighbor.f      = gNew + neighbor.h;
                    neighbor.parent = cur;
                    open.add(neighbor);
                }
            }
        }
        return Collections.emptyList();
    }

    // Manhattan heuristic for 4-way movement
    private static int heuristic(int x, int y, int gx, int gy) {
        return Math.abs(x - gx) + Math.abs(y - gy);
    }

    // Scans a rectangle of size w×h at (x,y) for any '1' obstacles
    private static boolean isAreaFree(int[][] grid, int x, int y, int w, int h, int playerPositionX, int playerPositionY) {
        System.out.println("Checking area free at (" + x + ", " + y + ") size (" + w + ", " + h + ")");
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                try{
                    if (grid[yy][xx] == 1) {
                        // check the location i am at to make sure i am not colliding with myself
                        if(yy == startY && xx == startX){
                            return true;
                        } else {
                            System.out.println("Collision at (" + xx + ", " + yy + ")");
                            return false;
                        }
                    }
                } catch(Exception e){
                    System.out.println(e);
                }
                // also check to make sure i will not collide with the player
                if(xx == playerPositionX && yy == playerPositionY){
                    System.out.println("Collision with player at (" + xx + ", " + yy + ")");
                    return false;
                }
            }
        }
        return true;
    }

    private static List<Point> buildPath(Node end) {
        System.out.println("Building path...");
        LinkedList<Point> path = new LinkedList<>();
        for (Node cur = end; cur != null; cur = cur.parent) {
            path.addFirst(new Point(cur.x, cur.y));
        }
        return path;
    }

    private static class Node {
        int x, y, g, h, f;
        Node parent;

        Node(int x, int y, Node parent, int g, int h) {
            this.x = x; this.y = y;
            this.parent = parent;
            this.g = g; this.h = h;
            this.f = g + h;
        }
    }
}
