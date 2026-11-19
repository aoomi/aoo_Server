package com.ddm.server.common.proxy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import java.lang.invoke.MethodHandles;

import static net.bytebuddy.matcher.ElementMatchers.isFinalizer;
import static net.bytebuddy.matcher.ElementMatchers.isVirtual;
import static net.bytebuddy.matcher.ElementMatchers.not;

public final class ProxyFactory {

    private ProxyFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T getProxyInstance(Object target, ProxyMethodInterceptor interceptor) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(target.getClass(), MethodHandles.lookup());
            return (T) new ByteBuddy()
                    .subclass(target.getClass())
                    .method(isVirtual().and(not(isFinalizer())))
                    .intercept(InvocationHandlerAdapter.of(
                            (proxy, method, args) -> interceptor.intercept(
                                    target, method, args == null ? new Object[0] : args)))
                    .make()
                    .load(target.getClass().getClassLoader(), ClassLoadingStrategy.UsingLookup.of(lookup))
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to create proxy for " + target.getClass().getName(), e);
        }
    }
}
