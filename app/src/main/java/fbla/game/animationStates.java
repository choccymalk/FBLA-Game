package fbla.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class animationStates {
    @SerializedName("idle")
    private List<String> idleImages;
    @SerializedName("walking_up")
    private List<String> walkingUpImages;
    @SerializedName("walking_down")
    private List<String> walkingDownImages;
    @SerializedName("walking_left")
    private List<String> walkingLeftImages;
    @SerializedName("walking_right")
    private List<String> walkingRightImages;

    public List<String> getIdleImagesPaths(){
        return idleImages;
    }
    public List<String> getWalkingUpImagesPaths(){
        return walkingUpImages;
    }
    public List<String> getWalkingDownImagesPaths(){
        return walkingDownImages;
    }
    public List<String> getWalkingLeftImagesPaths(){
        return walkingLeftImages;
    }
    public List<String> getWalkingRightImagesPaths(){
        return walkingRightImages;
    }
}
