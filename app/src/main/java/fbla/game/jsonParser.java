package fbla.game;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;

public class jsonParser {
    File levelsJson;
    Gson gson = new Gson();
    List<Level> levels;
    List<Door> doors;

    public jsonParser(File levelsJson) {
        this.levelsJson = levelsJson;
    }
}