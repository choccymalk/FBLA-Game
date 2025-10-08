package fbla.game;

import com.google.gson.annotations.SerializedName;

public class Door {
    @SerializedName("x")
    private int x;

    @SerializedName("y")
    private int y;

    @SerializedName("target_level")
    private int targetLevel;

    @SerializedName("target_x")
    private int targetX;

    @SerializedName("target_y")
    private int targetY;

    // Getters:
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
