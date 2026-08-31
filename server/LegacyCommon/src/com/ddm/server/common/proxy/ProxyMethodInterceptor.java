package com.ddm.server.common.proxy;

import java.lang.reflect.Method;

public interface ProxyMethodInterceptor {

    public Object intercept(Object obj, Method method, Object[] args) throws Throwable;

}
