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

    /**
     * Finds a path for a rectangular entity of size objWidth×objHeight.
     * startX/Y and goalX/Y refer to the top-left corner of that rectangle.
     */
    public static List<Point> findPath(
        int[][] grid,
        int startX, int startY,
        int goalX,  int goalY,
        int objWidth, int objHeight
    ) {
        int rows = grid.length, cols = grid[0].length;

        // Quick reject if start or goal footprint collides immediately
        if (!isAreaFree(grid, startX, startY, objWidth, objHeight)) return Collections.emptyList();
        if (!isAreaFree(grid, goalX,  goalY,  objWidth, objHeight))  return Collections.emptyList();

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
                if (!isAreaFree(grid, nx, ny, objWidth, objHeight)
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
    private static boolean isAreaFree(int[][] grid, int x, int y, int w, int h) {
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                if (grid[yy][xx] == 1) return false;
            }
        }
        return true;
    }

    private static List<Point> buildPath(Node end) {
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
