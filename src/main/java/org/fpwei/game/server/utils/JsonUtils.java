package org.fpwei.game.server.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fpwei.game.server.annotation.Message;
import org.fpwei.game.server.common.CommonRuntimeException;

import java.util.Set;

public class JsonUtils {

    private static final Gson gson = new Gson();
    private static final JsonParser parser = new JsonParser();

    public static String toJson(Object object) {

        return toJson(object, true);

    }

    public static String toJson(Object object, boolean isIncludeClassName) {
        String root;

        if (isIncludeClassName) {
            if (object.getClass().isAnnotationPresent(Message.class)) {
                root = object.getClass().getAnnotation(Message.class).value();
            } else {
                root = object.getClass().getSimpleName().toLowerCase();
            }

            JsonElement je = gson.toJsonTree(object);
            JsonObject jo = new JsonObject();
            jo.add(root, je);

            return gson.toJson(jo);
        } else {
            return gson.toJson(object);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static String getRootKey(String json) {
        JsonObject obj = parser.parse(json).getAsJsonObject();

        Set<String> keySet = obj.keySet();
        if (keySet.size() != 1) {
            throw new CommonRuntimeException("Expect only one property in json string.");
        }

        return keySet.iterator().next();
    }

    public static String getJson(String key, String json) {
        if (json.contains(key)) {
            int begin = json.indexOf("{", json.indexOf(key));
            int count = 1;

            for (int i = begin + 1; i < json.length(); i++) {
                if (json.charAt(i) == '{') {
                    count++;
                } else if (json.charAt(i) == '}') {
                    count--;
                }

                if (count == 0) {
                    return json.substring(begin, i + 1);
                }
            }

            throw new CommonRuntimeException("Illegal format");
        } else {
            return null;
        }
    }

}
