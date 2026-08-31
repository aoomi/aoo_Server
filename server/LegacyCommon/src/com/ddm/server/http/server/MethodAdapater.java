package com.ddm.server.http.server;

import java.util.HashMap;
import java.util.Map;

public class MethodAdapater {
    private Map<String, HttpAdaperter> requests = new HashMap<>();

    public HttpAdaperter getAdaperter(String path) {
        return requests.get(path);
    }

    public boolean containsKey(String path) {
        return requests.containsKey(path);
    }

    public void addAdapter(String path, HttpAdaperter httpAdaperter) {
        requests.put(path, httpAdaperter);
    }
}
