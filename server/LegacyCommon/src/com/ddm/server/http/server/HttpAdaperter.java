package com.ddm.server.http.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class HttpAdaperter {

    private Object instance = null;
    private Method method = null;

    public HttpAdaperter(Object instance, Method method) {
        super();
        this.instance = instance;
        this.method = method;
    }

    public void invoke(HttpRequest request, HttpResponse response) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        method.invoke(instance, request, response);
    }
}
