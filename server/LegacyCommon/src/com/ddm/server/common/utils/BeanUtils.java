package com.ddm.server.common.utils;

import BaseCommon.CommLog;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xsj
 * @date 2020/8/24 13:48
 * @description 对象属性拷贝工具
 */
public class BeanUtils {
    private static final ConcurrentHashMap<String, List<PropertyBinding>> BINDINGS = new ConcurrentHashMap<>();

    private record PropertyBinding(Method getter, Method setter) {
        void copy(Object source, Object target) throws ReflectiveOperationException {
            setter.invoke(target, getter.invoke(source));
        }
    }

    /**
     * 单纯属性拷贝
     *
     * @param dest
     * @param orig
     */
    public static void copyProperties(Object dest, Object orig) {
        if (orig != null) {
            try {
                for (PropertyBinding binding : bindings(orig.getClass(), dest.getClass())) {
                    binding.copy(orig, dest);
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("属性复制失败", e);
            }
        }
    }

    /**
     * 对象复制
     *
     * @param obj1   被复制对象，为空会抛出异常
     * @param classz 复制类型
     * @param <T>
     * @return
     */
    public static <T> T copyObject(Object obj1, Class<T> classz) {
        if (obj1 == null || classz == null) {
            throw new IllegalArgumentException("复制对象或者被复制类型为空!");
        }
        T obj2;
        try {
            obj2 = classz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            CommLog.error(e.getMessage(), e);
            throw new IllegalStateException("无法创建复制目标: " + classz.getName(), e);
        }
        copyProperties(obj2, obj1);
        return obj2;
    }

    /**
     * 复制队列
     *
     * @param list   被复制队列
     * @param classz 复制类型
     * @param <T>
     * @return
     */
    public static <T> List<T> copyList(List<?> list, Class<T> classz) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("被复制的队列为空!");
        }
        List<Object> resultList = new LinkedList<>();
        for (Object obj1 : list) {
            resultList.add(copyObject(obj1, classz));
        }
        return (List<T>) resultList;
    }

    private static List<PropertyBinding> bindings(Class<?> source, Class<?> target) {
        String key = source.getName() + "->" + target.getName();
        return BINDINGS.computeIfAbsent(key, ignored -> createBindings(source, target));
    }

    private static List<PropertyBinding> createBindings(Class<?> source, Class<?> target) {
        try {
            var sourceProperties = Introspector.getBeanInfo(source).getPropertyDescriptors();
            var targetProperties = Introspector.getBeanInfo(target).getPropertyDescriptors();
            List<PropertyBinding> result = new LinkedList<>();
            for (PropertyDescriptor sourceProperty : sourceProperties) {
                Method getter = sourceProperty.getReadMethod();
                if (getter == null) continue;
                for (PropertyDescriptor targetProperty : targetProperties) {
                    Method setter = targetProperty.getWriteMethod();
                    if (sourceProperty.getName().equals(targetProperty.getName()) && setter != null
                            && setter.getParameterTypes()[0].isAssignableFrom(getter.getReturnType())) {
                        result.add(new PropertyBinding(getter, setter));
                        break;
                    }
                }
            }
            return List.copyOf(result);
        } catch (IntrospectionException e) {
            throw new IllegalStateException("无法分析属性映射", e);
        }
    }
}
