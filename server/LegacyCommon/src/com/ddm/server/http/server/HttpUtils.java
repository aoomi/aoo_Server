package com.ddm.server.http.server;

import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.Lists;
import com.ddm.server.common.utils.secure.MD5;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * http工具类
 *
 * @author HttpUtils
 */
public class HttpUtils {
    static private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //    static private String firstdir = "conf" + "/game.properties";
    static public final String SIGN_KEY = System.getProperty("Server_Key");
    static public String SIGN_KEY_RECHARGE = System.getProperty("Sign_key_Recharge");
    static public final String ZLE_KEY_RECHARGE = System.getProperty("Zle_Charge_Key");
    static public String Server_Charge_Key = System.getProperty("Server_Charge_Key");
    private static final long HTTP_SIGN_MAX_SKEW_MS = Long.getLong("HttpSignMaxSkewMs", 300_000L);
    private static final ConcurrentHashMap<String, Long> HTTP_SIGN_NONCES = new ConcurrentHashMap<>();

    public static Map<String, String> abstractHttpParams(String query) {
        Map<String, String> map = new TreeMap<String, String>();
        if (query == null || query.trim().isEmpty()) {
            return map;
        }
        String[] arrayStr = query.split("&");
        for (String str : arrayStr) {
            if (str == null || str.isEmpty()) {
                continue;
            }
            int sign = str.indexOf('=');
            if (sign >= 0) {
                map.put(str.substring(0, sign), str.substring(sign + 1));
            } else {
                map.put(str, "");
            }
        }
        return map;
    }

    public static JsonObject abstractGMParams(String query) throws Exception {
        return abstractGMParams(query, SIGN_KEY);
    }

    public static JsonObject abstractGMParams(String query, String signkey) throws Exception {
        if (signkey == null || signkey.trim().isEmpty()) {
            CommLogD.error("HTTP签名密钥未配置，拒绝管理请求");
            throw new RequestException(30003, "服务器签名配置错误");
        }
        if (query == null) {
            throw new RequestException(30001, "消息为空");
        }
        query = query.trim();
        if (query.isEmpty()) {
            throw new RequestException(30001, "消息为空");
        }

        JsonObject jsonObject = null;
        try {
            jsonObject = JsonParser.parseString(query).getAsJsonObject();
        } catch (Exception e) {
            throw new RequestException(30002, "发送的post数据无法解析出相关活动参数");
        }

        TreeMap<String, JsonElement> params = new TreeMap<>();
        for (java.util.Map.Entry<String, JsonElement> pair : jsonObject.entrySet()) {
            params.put(pair.getKey(), pair.getValue());
        }

        if (!params.containsKey("sign")) {
            throw new RequestException(30002, "缺少参数[sign]");
        }
        String sign = params.get("sign").getAsString();

        StringBuilder signsrc = new StringBuilder();
        for (java.util.Map.Entry<String, JsonElement> sendparam : params.entrySet()) {
            if ("sign".equalsIgnoreCase(sendparam.getKey())) {
                continue;
            }
            if (sendparam.getValue() == null) {
                throw new RequestException(30002, String.format("key=%s的值为null", sendparam.getKey()));
            }
            String value = null;
            if (sendparam.getValue().isJsonObject() || sendparam.getValue().isJsonArray()) {
                value = sendparam.getValue().toString();
                value = toUnicode(value);
            } else {
                value = sendparam.getValue().getAsString();
            }
            // value = value.replace("\"", "");
            value = value.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
            value = URLDecoder.decode(value, "UTF-8");
            signsrc.append(sendparam.getKey()).append("=").append(value).append("&");
        }

        signsrc = signsrc.append(signkey);
        String s = MD5.md5(signsrc.toString());
        if (!constantTimeEquals(sign, s)) {
            // Never log the signed source: its suffix contains the server secret.
            CommLogD.info("HTTP签名校验失败, 参数数量:{}", params.size());
            throw new RequestException(30002, "签名错误");
        }
        checkReplayProtection(jsonObject);
        return jsonObject;
    }

    private static boolean constantTimeEquals(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(actual.toLowerCase().getBytes(StandardCharsets.US_ASCII),
                expected.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    /** Timestamp and nonce are mandatory; the legacy replayable request shape is rejected. */
    private static void checkReplayProtection(JsonObject jsonObject) throws RequestException {
        JsonElement timestampElement = jsonObject.get("timestamp");
        JsonElement nonceElement = jsonObject.get("nonce");
        if (timestampElement == null || nonceElement == null) {
            throw new RequestException(30002, "缺少防重放参数[timestamp/nonce]");
        }
        long timestamp;
        String nonce;
        try {
            timestamp = timestampElement.getAsLong();
            nonce = nonceElement.getAsString();
        } catch (Exception e) {
            throw new RequestException(30002, "防重放参数格式错误");
        }
        if (timestamp < 10_000_000_000L) {
            timestamp *= 1000L;
        }
        long now = System.currentTimeMillis();
        if (nonce.length() < 16 || nonce.length() > 128 || Math.abs(now - timestamp) > HTTP_SIGN_MAX_SKEW_MS) {
            throw new RequestException(30002, "请求已过期或nonce无效");
        }
        HTTP_SIGN_NONCES.entrySet().removeIf(entry -> entry.getValue() < now);
        if (HTTP_SIGN_NONCES.putIfAbsent(nonce, timestamp + HTTP_SIGN_MAX_SKEW_MS) != null) {
            throw new RequestException(30002, "请求已被使用");
        }
    }

    private static String toUnicode(String asciicode) {
        char[] utfBytes = asciicode.toCharArray();
        StringBuilder unicodeBytes = new StringBuilder();
        for (int byteIndex = 0; byteIndex < utfBytes.length; byteIndex++) {
            String hexB = Integer.toHexString(utfBytes[byteIndex]);
            if (hexB.length() <= 2) {
                unicodeBytes.append(utfBytes[byteIndex]);
            } else {
                unicodeBytes.append("\\u" + hexB);
            }
        }
        return unicodeBytes.toString();
    }

    public static int getTime(JsonObject json, String name) throws RequestException {
        try {
            synchronized (sdf) {
                return (int) (sdf.parse(json.get(name).getAsString()).getTime() / 1000);
            }
        } catch (Exception e) {
            throw new RequestException(30002, "缺少int格式参数[%s]", name);
        }
    }

    public static int getTime(JsonObject json, String name, int def) throws RequestException {
        JsonElement value = json.get(name);
        if (value == null) {
            return def;
        }
        try {
            synchronized (sdf) {
                return (int) (sdf.parse(value.getAsString()).getTime() / 1000);
            }
        } catch (Exception e) {
            throw new RequestException(30002, "参数[%s]无法解析出int值", name);
        }
    }

    public static <T extends Enum<T>> T getEnum(JsonObject json, String name, Class<T> class1) throws RequestException {
        try {
            return Enum.valueOf(class1, json.get(name).getAsString());
        } catch (Exception e) {
            throw new RequestException(30002, "缺少[%s]格式参数[%s]", class1.getSimpleName(), name);
        }
    }

    public static boolean getBool(JsonObject json, String name) throws RequestException {
        try {
            return json.get(name).getAsBoolean();
        } catch (Exception e) {
            throw new RequestException(30002, "缺少bool格式参数[%s]", name);
        }
    }

    public static boolean getBool(JsonObject json, String name, boolean def) {
        try {
            return json.get(name).getAsBoolean();
        } catch (Exception e) {
            return def;
        }
    }

    public static JsonObject getJsonObject(JsonObject json, String name) throws RequestException {
        try {
            return json.get(name).getAsJsonObject();
        } catch (Exception e) {
            throw new RequestException(30002, "缺少JsonObject格式参数[%s]", name);
        }
    }

    public static JsonArray getJsonArray(JsonObject json, String name) throws RequestException {
        try {
            return json.get(name).getAsJsonArray();
        } catch (Exception e) {
            throw new RequestException(30002, "缺少JsonArray格式参数[%s]", name);
        }
    }

    public static int getInt(JsonObject json, String name) throws RequestException {
        try {
            return json.get(name).getAsInt();
        } catch (Exception e) {
            throw new RequestException(30002, "缺少Int格式参数[%s]", name);
        }
    }

    public static double getDouble(JsonObject json, String name) throws RequestException {
        try {
            return json.get(name).getAsDouble();
        } catch (Exception e) {
            throw new RequestException(30002, "缺少Double格式参数[%s]", name);
        }
    }

    public static long getLong(JsonObject json, String name) throws RequestException {
        try {
            return json.get(name).getAsLong();
        } catch (Exception e) {
            throw new RequestException(30002, "缺少Long格式参数[%s]", name);
        }
    }

    public static String getString(JsonObject json, String name) throws RequestException {
        try {
            return json.get(name).getAsString();
        } catch (Exception e) {
            throw new RequestException(30002, "缺少string格式参数[%s]", name);
        }
    }

    public static String getString(JsonObject json, String name, String def) throws RequestException {
        JsonElement value = json.get(name);
        if (value == null) {
            return def;
        }
        try {
            return value.getAsString();
        } catch (Exception e) {
            throw new RequestException(30002, "参数[%s]无法解析出string值", name);
        }
    }

    public static List<Integer> getIntList(JsonObject json, String name) throws RequestException {
        List<Integer> list = Lists.newArrayList();
        try {
            JsonArray jsonArray = json.get(name).getAsJsonArray();
            if (jsonArray.size() > 0) {
                jsonArray.forEach(x -> {
                    list.add(x.getAsInt());
                });
            }
        } catch (Exception e) {
            throw new RequestException(30002, "缺少Integer格式参数[%s]", name);
        }
        return list;
    }

    public static List<Long> getLongList(JsonObject json, String name) throws RequestException {
        try {
            return new Gson().fromJson(json.get(name).getAsString(), new TypeToken<List<Long>>() {
            }.getType());
        } catch (Exception e) {
            throw new RequestException(30002, "缺少Long数组格式参数[%s],[%s]", name, json);
        }
    }

    public static boolean isHas(JsonObject json, String name) {
        return json.has(name);
    }


//    /**
//     * 通知Php后台,传递游戏内数据
//     *
//     * @param url
//     * @param param
//     */
//    public static void NotifyGM(String url, GMParam param) {
//        HttpUtils.RequestGM(url, param, new IResponseHandler() {
//            @Override
//            public void compeleted(String response) {
//                 //CommLogD.error("GM通知[{}] 回包成功：{}", url, response);
//            }
//
//            @Override
//            public void failed(Exception exception) {
//                CommLogD.error("GM通知[{}] 回包失败", url, exception);
//            }
//        });
//    }
//
//    /**
//     * 请求Php后台,传递游戏内数据
//     *
//     * @param url
//     * @param param
//     * @param response
//     */
//    public static void RequestGM(String url, GMParam params, IResponseHandler response) {
//        try {
//            params.put("op", "GetGameStatus");
//            params.put("server_id", Config.ServerID());
//            HttpAsyncClient.startHttpGet(url + params.toUrlParam(), response);
//        } catch (Exception e) {
//            CommLogD.error("GM请求[{}] 发送失败", url, e);
//        }
//    }

//    /**
//     * 指游数据收集请求系统
//     *
//     * @param url
//     * @param param
//     */
//    public static void ZyDataCollect(String url, GMParam param) {
//        JsonObject json = new JsonObject();
//        param.forEach((key, value) -> {
//            json.addProperty(key, value.toString());
//        });
//        StringEntity stringEntity = null;
//        try {
//            stringEntity = new StringEntity(json.toString(), "UTF-8");
//            stringEntity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
//        } catch (Exception e) {
//            CommLogD.error("请求[{}]封装post参数失败:{}", url, e.toString());
//        }
//        if (stringEntity == null) {
//            return;
//        }
//        HttpPost httpPost = new HttpPost(url);
//        httpPost.addHeader("Content-Type", "application/json;charset=utf-8");
//        httpPost.setEntity(stringEntity);
//        // 通知系统
//        HttpAsyncClient.startHttpPost(httpPost, new IResponseHandler() {
//
//            @Override
//            public void compeleted(String response) {
//                CommLogD.warn("追踪地址[{}]通知回包成功：{}", url, response);
//            }
//
//            @Override
//            public void failed(Exception exception) {
//                CommLogD.warn("追踪地址[{}]通知回包失败：{}", url, exception.toString());
//            }
//        });
//    }
}
