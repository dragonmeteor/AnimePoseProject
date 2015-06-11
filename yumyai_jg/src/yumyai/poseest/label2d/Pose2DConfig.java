package yumyai.poseest.label2d;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import yondoko.util.JsonUtil;

import javax.vecmath.Color3f;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Pose2DConfig {
    public ArrayList<String> boneNames = new ArrayList<String>();
    public HashMap<String, Color3f> boneColors = new HashMap<String, Color3f>();
    public ArrayList<Pair<String, String>> edges = new ArrayList<Pair<String, String>>();

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        {
            JsonArray array = new JsonArray();
            for (String boneName : boneNames) {
                array.add(new JsonPrimitive(boneName));
            }
            json.add("bone_names", array);
        }
        {
            JsonObject boneColorsJson = new JsonObject();
            for (Map.Entry<String, Color3f> entry : boneColors.entrySet()) {
                boneColorsJson.add(entry.getKey(), JsonUtil.toJson(entry.getValue()));
            }
            json.add("bone_colors", boneColorsJson);
        }
        {
            JsonArray array = new JsonArray();
            for (Pair<String, String> edge : edges) {
                JsonArray edgeJson = new JsonArray();
                edgeJson.add(new JsonPrimitive(edge.getLeft()));
                edgeJson.add(new JsonPrimitive(edge.getRight()));
            }
            json.add("edges", array);
        }
        return json;
    }

    public void fromJson(JsonObject json) {
        {
            boneNames.clear();
            JsonArray array = json.get("bone_names").getAsJsonArray();
            for (JsonElement elem : array) {
                boneNames.add(elem.getAsString());
            }
        }
        {
            boneColors.clear();
            JsonObject boneColorsJson = json.get("bone_colors").getAsJsonObject();
            for (String boneName : boneNames) {
                Color3f color = new Color3f();
                JsonUtil.fromJson(boneColorsJson.get(boneName).getAsJsonArray(), color);
                boneColors.put(boneName, color);
            }
        }
        {
            edges.clear();
            JsonArray edgeArray = json.get("edges").getAsJsonArray();
            for (JsonElement elem : edgeArray) {
                JsonArray edgeA = elem.getAsJsonArray();
                ImmutablePair<String, String> edge = new ImmutablePair<String, String>(
                        edgeA.get(0).getAsString(),
                        edgeA.get(1).getAsString()
                );
                edges.add(edge);
            }
        }
    }

    public void save(String outputFileName) throws IOException {
        JsonUtil.save(outputFileName, toJson());
    }

    public static Pose2DConfig load(String inputFileName) throws IOException {
        Pose2DConfig result = new Pose2DConfig();
        JsonObject json = JsonUtil.load(inputFileName).getAsJsonObject();
        result.fromJson(json);
        return result;
    }
}
