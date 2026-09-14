package com.ddm.server.common.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.util.*;


public class JsonUtil {

    /**
     * JSON字符串转换成对象
     *
     * @param jsonString 需要转换的字符串
     * @param type       需要转换的对象类型
     * @return 对象
     */
    public static <T> T jsonToBean(String jsonString, Class<T> type) {
        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        return (T) JSONObject.toJavaObject(jsonObject, type);
    }

    /**
     * JSON字符串转换成列表对象
     *
     * @param jsonString 需要转换的字符串
     * @param type       需要转换的列表对象类型
     * @return 列表对象
     */
    public static <T> List<T> jsonToBeanList(String jsonString, Class<T> type) {
        return JSONArray.parseArray(jsonString, type);
    }

    /**
     * 将JSONArray对象转换成list集合
     *
     * @param jsonArr
     * @return
     */
    public static List<Object> jsonToList(JSONArray jsonArr) {
        List<Object> list = new ArrayList<Object>();
        for (Object obj : jsonArr) {
            if (obj instanceof JSONArray) {
                list.add(jsonToList((JSONArray) obj));
            } else if (obj instanceof JSONObject) {
                list.add(jsonToMap((JSONObject) obj));
            } else {
                list.add(obj);
            }
        }
        return list;
    }

    /**
     * 将json字符串转换成map对象
     *
     * @param json
     * @return
     */
    public static Map<String, Object> jsonToMap(String json) {
        JSONObject obj = JSONObject.parseObject(json);
        return jsonToMap(obj);
    }

    /**
     * 将JSONObject转换成map对象
     *
     * @param obj
     * @return
     */
    public static Map<String, Object> jsonToMap(JSONObject obj) {
        Set<?> set = obj.keySet();
        Map<String, Object> map = new HashMap<String, Object>(set.size());
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof JSONArray) {
                map.put(key, jsonToList((JSONArray) value));
            } else if (value instanceof JSONObject) {
                map.put(key, jsonToMap((JSONObject) value));
            } else {
                map.put(key, value);
            }

        }
        return map;
    }

    /**
     * 对象转换为json字符串
     *
     * @param obj
     * @return
     */
    public static String toJson(Object obj) {
        return JSONObject.toJSONString(obj, SerializerFeature.WriteMapNullValue);
    }

    /**
     * 对象转换为json字符串格式化字符串为标准的json格式
     *
     * @param obj
     * @return
     */
    public static String toJsonFormat(Object obj) {
        String json = JSONObject.toJSONString(obj, SerializerFeature.WriteMapNullValue);
        json = json.replace("\"{", "{").replace("}\"", "}").replace("\"[", "[").replace("]\"", "]").replace("\\\"", "");
        return json;
    }

    /**
     * 判断是否数组
     *
     * @param json
     * @return
     */
    public static Boolean isArray(String json) {
        if (json != null && json.startsWith("[")) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否对象
     *
     * @param json
     * @return
     */
    public static Boolean isObject(String json) {
        if (json != null && json.startsWith("{")) {
            return true;
        }
        return false;
    }
}
