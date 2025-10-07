package fbla.game;

public class Door {
    int level;
    int x;
    int y;
    int targetLevel;
    int targetX;
    int targetY;

    public Door(int level, int x, int y, int targetLevel, int targetX, int targetY) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.targetLevel = targetLevel;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    public int getLevel() {
        return level;
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public int getTargetLevel() {
        return targetLevel;
    }
    public int getTargetX() {
        return targetX;
    }
    public int getTargetY() {
        return targetY;
    }
}
