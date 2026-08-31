package com.ddm.server.http;


import java.util.Map;

public interface IOkHttpClient {

    public String get(String url);

    public String get(String url, Map<String, String> params);

    public void get(String url, Map<String, String> params, StringCallback callback);

    public String post(String url, Map<String, String> params);

    public void post(String url, Map<String, String> params, StringCallback callback);


}
