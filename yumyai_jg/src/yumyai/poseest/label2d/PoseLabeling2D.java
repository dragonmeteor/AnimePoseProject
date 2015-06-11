package yumyai.poseest.label2d;

import com.google.gson.*;
import yondoko.util.JsonUtil;
import yondoko.util.TextIo;

import javax.vecmath.Point2f;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PoseLabeling2D {
    public String fileName;
    public HashMap<String, Point2f> points = new HashMap<String, Point2f>();
    public String extraInfo = "";

    public PoseLabeling2D() {
        // NOP
    }

    public PoseLabeling2D(String fileName) {
        this.fileName = fileName;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("file_name", fileName);
        JsonObject pointsJson = new JsonObject();
        for (String pointName : points.keySet()) {
            Point2f p = points.get(pointName);
            pointsJson.add(pointName, JsonUtil.toJson(p));
        }
        json.add("points", pointsJson);
        return json;
    }

    public void fromJson(JsonObject json) {
        fileName = json.get("file_name").getAsString();
        JsonObject pointsJson = json.get("points").getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : pointsJson.entrySet()) {
            Point2f p = new Point2f();
            if (entry.getValue() instanceof JsonNull) {
                p = null;
            } else {
                JsonUtil.fromJson(entry.getValue().getAsJsonArray(), p);
            }
            points.put(entry.getKey(), p);
        }
        JsonObject extraInfoObj = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getKey().equals("points") && !entry.getKey().equals("file_name"))
                extraInfoObj.add(entry.getKey(), entry.getValue());
        }
        Gson gson = new GsonBuilder().create();
        extraInfo = gson.toJson(extraInfoObj);
    }

    public static void save(String outputFileName, ArrayList<PoseLabeling2D> labelings) throws IOException {
        JsonArray jsonArray = new JsonArray();
        for (PoseLabeling2D labeling : labelings) {
            JsonObject json = labeling.toJson();
            jsonArray.add(json);
        }
        JsonUtil.save(outputFileName, jsonArray);
    }

    public static ArrayList<PoseLabeling2D> load(String inputFileName) throws IOException {
        JsonElement element = JsonUtil.load(inputFileName);
        if (!(element instanceof JsonArray)) {
            throw new RuntimeException("content of JSON file is not an array!");
        } else {
            JsonArray array = element.getAsJsonArray();
            ArrayList<PoseLabeling2D> result = new ArrayList<PoseLabeling2D>();
            for (JsonElement elem : array) {
                PoseLabeling2D labeling = new PoseLabeling2D();
                labeling.fromJson(elem.getAsJsonObject());
                result.add(labeling);
            }
            return result;
        }
    }
}
