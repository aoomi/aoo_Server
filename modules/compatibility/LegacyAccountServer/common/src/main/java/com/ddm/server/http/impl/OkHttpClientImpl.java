package com.ddm.server.http.impl;

import com.ddm.server.common.utils.CommLogD;
import com.ddm.server.http.IOkHttpClient;
import com.ddm.server.http.StringCallback;
import okhttp3.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Map;

public class OkHttpClientImpl implements IOkHttpClient {
    /**
     * 客户端操作
     */
    private OkHttpClient okHttpClient;

    @Override
    public String get(String url) {
        return get(url, null);
    }

    @Override
    public String get(String url, Map<String, String> params) {
        Request.Builder reqBuilder = new Request.Builder();
        reqBuilder.url(concatUrl(url, params));

        return execute(reqBuilder.build());
    }

    @Override
    public void get(String url, Map<String, String> params, StringCallback callback) {
        Request.Builder reqBuilder = new Request.Builder();
        reqBuilder.url(concatUrl(url, params));

        execute(reqBuilder.build(), callback);
    }

    @Override
    public String post(String url, Map<String, String> params) {
        Request.Builder reqBuilder = new Request.Builder();
        reqBuilder.url(url);

        FormBody.Builder formBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            formBuilder.add(entry.getKey(), entry.getValue());
        }
        reqBuilder.post(formBuilder.build());

        return execute(reqBuilder.build());
    }


    @Override
    public void post(String url, Map<String, String> params, StringCallback callback) {
        Request.Builder reqBuilder = new Request.Builder();
        reqBuilder.url(url);

        FormBody.Builder formBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            formBuilder.add(entry.getKey(), entry.getValue());
        }
        reqBuilder.post(formBuilder.build());

        execute(reqBuilder.build(), callback);
    }

    public String concatUrl(String url, Map<String, String> data) {
        if (data == null) {
            return url;
        }

        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                sb.append("&" + entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "utf-8"));
            }

            if (sb.length() < 1) {
                return url;
            }

            String prefix = sb.substring(1).toString();
            if (url.indexOf("?") < 1) {
                url += "?";
            }

            return url + prefix;
        } catch (Exception ex) {
            CommLogD.warn("OkHttpClientImpl concatUrl:{}", ex);
        }
        return url;
    }


    private InputStream executeAndGetStream(Request request) {
        final Call call = okHttpClient.newCall(request);

        Response response = null;
        try {
            response = call.execute();
            ResponseBody body = response.body();
            return body.byteStream();
        } catch (IOException e) {
            CommLogD.error("OkHttpClientImpl executeAndGetStream:{}", e);
        }
        return null;
    }

    private void execute(Request request, StringCallback callback) {
        final Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onResponse(Call paramCall, Response paramResponse) throws IOException {
                callback.completed(paramResponse.body().string());
            }

            @Override
            public void onFailure(Call paramCall, IOException ex) {
                callback.failed(ex);
            }
        });
    }


    private String execute(Request request) {
        final Call call = okHttpClient.newCall(request);
        Response response = null;
        try {
            response = call.execute();
            ResponseBody body = response.body();
            return body.string();
        } catch (IOException e) {
            CommLogD.error("OkHttpClientImpl execute:{}", e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return "";
    }


    public void setOkHttpClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }
}

