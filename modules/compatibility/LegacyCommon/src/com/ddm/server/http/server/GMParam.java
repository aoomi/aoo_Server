package com.ddm.server.http.server;

import com.ddm.server.common.utils.secure.MD5;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.util.TreeMap;

public class GMParam extends TreeMap<String, Object> {

    private static final long serialVersionUID = 4770492078712306761L;

    private String key = null;

    public GMParam() {
        this.key = HttpUtils.SIGN_KEY;
    }

    public GMParam(String key) {
        this.key = key;
    }

    public String toUrlParam() throws Exception {
        StringBuilder params = new StringBuilder("?");
        StringBuilder signsrc = new StringBuilder();
        for (java.util.Map.Entry<String, Object> pair : this.entrySet()) {
            String value = pair.getValue().toString();
            value = URLEncoder.encode(value, "utf-8");
            signsrc.append(pair.getKey()).append("=").append(value).append("&");
            params.append(pair.getKey()).append("=").append(value).append("&");
        }
        signsrc = signsrc.append(key);
        params.append("sign").append("=").append(MD5.md5(signsrc.toString()));
        return params.toString();
    }
}
