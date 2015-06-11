package yondoko.util;

import com.google.gson.*;

import javax.vecmath.Matrix3f;
import javax.vecmath.Tuple2f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Tuple4f;
import java.io.File;

public class JsonUtil {
    public static JsonArray toJson(Tuple3f t) {
        JsonArray a = new JsonArray();
        a.add(new JsonPrimitive(t.x));
        a.add(new JsonPrimitive(t.y));
        a.add(new JsonPrimitive(t.z));
        return a;
    }

    public static void fromJson(JsonArray json, Tuple3f t) {
        t.x = json.get(0).getAsFloat();
        t.y = json.get(1).getAsFloat();
        t.z = json.get(2).getAsFloat();
    }

    public static JsonArray toJson(Tuple4f t) {
        JsonArray a = new JsonArray();
        a.add(new JsonPrimitive(t.x));
        a.add(new JsonPrimitive(t.y));
        a.add(new JsonPrimitive(t.z));
        a.add(new JsonPrimitive(t.w));
        return a;
    }

    public static void fromJson(JsonArray json, Tuple4f t) {
        t.x = json.get(0).getAsFloat();
        t.y = json.get(1).getAsFloat();
        t.z = json.get(2).getAsFloat();
        t.w = json.get(3).getAsFloat();
    }

    public static JsonArray toJson(Tuple2f t) {
        JsonArray a = new JsonArray();
        a.add(new JsonPrimitive(t.x));
        a.add(new JsonPrimitive(t.y));
        return a;
    }

    public static void fromJson(JsonArray json, Tuple2f t) {
        t.x = json.get(0).getAsFloat();
        t.y = json.get(1).getAsFloat();
    }

    public static JsonArray toJson(Matrix3f m) {
        JsonArray a = new JsonArray();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                float x = m.getElement(i,j);
                a.add(new JsonPrimitive(x));
            }
        }
        return a;
    }

    public static void fromJson(JsonArray json, Matrix3f m) {
        for (int k = 0; k < 9; k++) {
            float x = json.get(k).getAsFloat();
            m.setElement(k/3, k%3, x);
        }
    }

    public static void save(String fileName, JsonElement json) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String content = gson.toJson(json);
        TextIo.writeTextFile(fileName, content);
    }

    public static JsonElement load(String fileName) {
        String content = TextIo.readTextFile(fileName);
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(content);
        return element;
    }
}
