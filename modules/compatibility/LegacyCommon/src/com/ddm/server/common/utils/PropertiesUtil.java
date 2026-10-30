package com.ddm.server.common.utils;

import BaseCommon.CommLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author xsj
 * @date 2020/8/24 14:44
 * @description 属性字段修改
 */
public class PropertiesUtil {
    public static Method getGetMethod(Class objectClass, String fieldName) {
        StringBuffer sb = new StringBuffer();
        sb.append("get");
        sb.append(fieldName.substring(0, 1).toUpperCase());
        sb.append(fieldName.substring(1));
        try {
            return objectClass.getMethod(sb.toString());
        } catch (Exception e) { /* exception-policy: missing bean accessor is an expected lookup miss */ }
        return null;
    }

    public static Method getGetMethodBoolean(Class objectClass, String fieldName) {
        // 获取实体类的所有属性，返回Field数组
        Field[] fields = objectClass.getDeclaredFields();
        String begin = "get";
        for (Field field : fields) {
            if (field.getName().equals(fieldName) && (field.getGenericType().toString().equals("class java.lang.Boolean") || field.getGenericType().toString().equals("boolean"))) {
                begin = "is";
                break;
            }
        }
        StringBuffer sb = new StringBuffer();
        sb.append(begin);
        sb.append(fieldName.substring(0, 1).toUpperCase());
        sb.append(fieldName.substring(1));
        try {
            return objectClass.getMethod(sb.toString());
        } catch (Exception e) { /* exception-policy: missing bean accessor is an expected lookup miss */ }
        return null;
    }

    public static Object invokeGet(Object o, String fieldName) {
        Method method = getGetMethod(o.getClass(), fieldName);
        if (method == null) {
            method = getGetMethodBoolean(o.getClass(), fieldName);
        }
        try {
            return method.invoke(o, new Object[0]);
        } catch (Exception e) {
            CommLog.error(e.getMessage(), e);
        }
        return null;
    }

    public static Field getField(Object o, String fieldName) {
        //得到class
        Class cls = o.getClass();
        //得到所有属性
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if (fieldName.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    /**
     * 执行set方法
     *
     * @param o         执行对象
     * @param fieldName 属性
     * @param value     值
     */
    public static void invokeSet(Object o, String fieldName, Object value) {
        Method method = getSetMethod(o.getClass(), fieldName);
        try {
            method.invoke(o, new Object[]{value});
        } catch (Exception e) {
            CommLog.error(e.getMessage(), e);
        }
    }

    /**
     * 执行set方法
     *
     * @param o         执行对象
     * @param fieldName 属性
     * @param value     值
     */
    public static void invokeSetSwitch(Object o, String fieldName, Object value) {
        Method method = getSetMethodSwitch(o.getClass(), fieldName);
        String name = "";
        try {
            Class[] parameterTypes = new Class[1];
            Field field = o.getClass().getDeclaredField(fieldName);
            parameterTypes[0] = field.getType();
            name = parameterTypes[0].getName();
            if (parameterTypes[0].getName().equals("double")) {
                value = Double.parseDouble((String) value);
            } else if (parameterTypes[0].getName().equals("int")) {
                value = Integer.parseInt((String) value);
            } else if (parameterTypes[0].getName().equals("long")) {
                value = Long.parseLong((String) value);
            } else if (parameterTypes[0].getName().equals("java.util.List")) {
                value = null;
            }
            method.invoke(o, new Object[]{value});
        } catch (Exception e) {
            CommLog.error("Unable to assign property " + name, e);
        }
    }

    public static Method getSetMethodSwitch(Class objectClass, String fieldName) {
        try {
            Class[] parameterTypes = new Class[1];
            Field field = objectClass.getDeclaredField(fieldName);
            parameterTypes[0] = field.getType();
            StringBuffer sb = new StringBuffer();
            sb.append("set");
            sb.append(fieldName.substring(0, 1).toUpperCase());
            sb.append(fieldName.substring(1));
            Method method = objectClass.getMethod(sb.toString(), parameterTypes);
            return method;
        } catch (Exception e) {
            CommLog.error(e.getMessage(), e);
        }
        return null;
    }

    public static Method getSetMethod(Class objectClass, String fieldName) {
        try {
            Class[] parameterTypes = new Class[1];
            Field field = objectClass.getDeclaredField(fieldName);
            parameterTypes[0] = field.getType();
            StringBuffer sb = new StringBuffer();
            sb.append("set");
            sb.append(fieldName.substring(0, 1).toUpperCase());
            sb.append(fieldName.substring(1));
            Method method = objectClass.getMethod(sb.toString(), parameterTypes);
            return method;
        } catch (Exception e) {
            CommLog.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 对象转数组数据
     *
     * @param o
     * @return
     */
    public static String getObjectToString(Object o) {
        StringBuilder stringBuilder = new StringBuilder();
        //得到class
        Class cls = o.getClass();
        //得到所有属性
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if (!field.getGenericType().toString().equals("class com.ddm.server.common.lock.AtomicBooleanLock")) {
                Object value = invokeGet(o, field.getName());
                stringBuilder.append(value).append(";");
            }
        }
        return stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1);
    }

    /**
     * 获取对象名称所在的位子
     *
     * @param objectClass
     * @return
     */
    public static Map<String, Integer> getObjectToMapIndex(Class objectClass) {
        Map<String, Integer> map = Maps.newConcurrentMap();
        //得到所有属性
        Field[] fields = objectClass.getDeclaredFields();
        int i = 0;
        for (Field field : fields) {
            if (!field.getGenericType().toString().equals("class com.ddm.server.common.lock.AtomicBooleanLock")) {
                map.put(field.getName(), i);
                i++;
            }
        }
        return map;
    }
}
