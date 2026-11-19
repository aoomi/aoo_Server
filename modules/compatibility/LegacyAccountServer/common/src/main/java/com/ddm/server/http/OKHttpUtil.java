package com.ddm.server.http;

import java.util.Map;

public class OKHttpUtil {

    private static IOkHttpClient iOkHttpClient;

    public static void setiOkHttpClient(IOkHttpClient iOkHttpClient) {
        OKHttpUtil.iOkHttpClient = iOkHttpClient;
    }

    public static String get(String url) {
        return iOkHttpClient.get(url);
    }

    ;

    public static String get(String url, Map<String, String> params) {
        return iOkHttpClient.get(url, params);
    }

    public static void get(String url, Map<String, String> params, StringCallback callback) {
        iOkHttpClient.get(url, params, callback);
    }

    public static String post(String url, Map<String, String> params) {
        return iOkHttpClient.post(url, params);
    }

    public static void post(String url, Map<String, String> params, StringCallback callback) {
        iOkHttpClient.post(url, params, callback);
    }

}
