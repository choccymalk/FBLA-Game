package fbla.editor;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class AIAbilities {
    @SerializedName("ai_packages")
    private List<String> aiPackages;

    // required no-arg constructor for Gson
    public AIAbilities() {}

    public AIAbilities(List<String> packages){
        this.setAiPackages(packages);
    }

    public void setAiPackages(List<String> aiPackages){
        this.aiPackages = aiPackages;
    }

    public boolean canMoveRandomly() {
        return aiPackages != null && aiPackages.contains("move_randomly");
    }

    public List<String> getAllAbilities(){
        return aiPackages;
    }
}
