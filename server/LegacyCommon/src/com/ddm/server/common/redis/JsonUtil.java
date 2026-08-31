package com.ddm.server.common.redis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;

public class JsonUtil {

    private static final Gson GSON = new GsonBuilder().create();

    public static String bean2Json(Object bean) throws Exception {
        if (bean == null) {
            return "";
        }
        return GSON.toJson(bean);
    }

    public static Object json2Bean(String json, Class<?> cls) throws Exception {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return GSON.fromJson(json, cls);
    }
}
