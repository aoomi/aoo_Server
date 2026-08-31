package com.ddm.server.http.server;

public enum HttpMethod {
    GET, // 从指定的url上获取内容
    POST, // 提交body中的内容给服务器中指定的url中，属于非幂等的(non-idempotent)请求
    PUT, // 将body上传至服务器指定url处
    DELETE, // 在指定url处删除资源
    ;

    public static HttpMethod nameOf(String name) {
        for (HttpMethod method : HttpMethod.values()) {
            if (method.name().equals(name)) {
                return method;
            }
        }
        return HttpMethod.GET;
    }

}
