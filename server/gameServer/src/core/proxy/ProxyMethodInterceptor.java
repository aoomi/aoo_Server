package core.proxy;

import com.ddm.server.annotation.Service;

import java.lang.reflect.Method;

@Service(source = "fff")
public interface ProxyMethodInterceptor {

    public Object intercept(Object obj, Method method, Object[] args) throws Throwable;

}
