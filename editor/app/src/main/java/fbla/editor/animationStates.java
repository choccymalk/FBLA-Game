package fbla.editor;

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

    // does not matter what setallempty is, it has to be there so gson doesnt try to use it
    public animationStates(boolean setAllEmpty){
        List<String> emptyList = List.of();
        this.idleImages = emptyList;
        this.walkingDownImages = emptyList;
        this.walkingUpImages = emptyList;
        this.walkingLeftImages = emptyList;
        this.walkingRightImages = emptyList;
    }

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
