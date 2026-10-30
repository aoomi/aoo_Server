package com.ddm.server.common.task;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodInvoke {

    private Object obj;
    private Method method;

    public void init(Object obj, Method method) {
        this.obj = obj;
        this.method = method;
    }

    public void invoke() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        method.invoke(obj);
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

}
