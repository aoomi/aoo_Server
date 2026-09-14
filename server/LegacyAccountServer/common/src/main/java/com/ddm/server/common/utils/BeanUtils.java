package com.ddm.server.common.utils;

import com.ddm.server.redis.jedis.RedisMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

public class BeanUtils {

    /**
     * 将Map对象通过反射机制转换成HashMap对象
     *
     * @param map   存放数据的map对象
     * @throws Exception 异常
     *                   异常暂时保留
     */
    public static Map<String , String> mapToHashMap(RedisMap map){
        if (Objects.nonNull(map) && map.size() > 0) {
            Map<String,String> stringMap = Maps.newHashMap();
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                stringMap.put(entry.getKey().toString(),entry.getValue().toString());
            }
            return stringMap;
        }
        return Collections.emptyMap();
    }


    /**
     * 将Map对象通过反射机制转换成HashMap对象
     *
     * @param map   存放数据的map对象
     * @throws Exception 异常
     *                   异常暂时保留
     */
    public static Map<String , Double> mapToHashDoubleMap(RedisMap map){
        if (Objects.nonNull(map) && map.size() > 0) {
            Map<String,Double> stringMap = Maps.newHashMap();
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                String value = entry.getValue().toString();
                stringMap.put(entry.getKey().toString(),Double.parseDouble(StringUtil.isNumber(value) ? value:"0"));
            }
            return stringMap;
        }
        return Collections.emptyMap();
    }

    /**
     * 获取指定字段名称查找在class中的对应的Field对象(包括查找父类)
     *
     * @param clazz     指定的class
     * @param fieldName 字段名称
     * @return Field对象
     */
    public static Field getClassField(Class<?> clazz, String fieldName) {
        if (Object.class.getName().equals(clazz.getName())) {
            return null;
        }
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        Class<?> superClass = clazz.getSuperclass();
        //简单的递归一下
        if (superClass != null) {
            return getClassField(superClass, fieldName);
        }
        return null;
    }

    /**
     * 将Object类型的值，转换成bean对象属性里对应的类型值
     *
     * @param value          Object对象值
     * @param fieldTypeClass 属性的类型
     * @return 转换后的值
     */
    public static Object convertValType(Object value, Class<?> fieldTypeClass, Type genericType) {
        Object retVal = null;
        if (Long.class.getName().equals(fieldTypeClass.getName())
                || long.class.getName().equals(fieldTypeClass.getName())) {
            retVal = Objects.nonNull(value) && StringUtils.isNotEmpty(value.toString()) ? Long.parseLong(value.toString()) : 0L;
        } else if (Integer.class.getName().equals(fieldTypeClass.getName())
                || int.class.getName().equals(fieldTypeClass.getName())) {
            retVal = Objects.nonNull(value) && StringUtils.isNotEmpty(value.toString()) ? Integer.parseInt(value.toString()) : 0;
        } else if (Float.class.getName().equals(fieldTypeClass.getName())
                || float.class.getName().equals(fieldTypeClass.getName())) {
            retVal = Objects.nonNull(value) && StringUtils.isNotEmpty(value.toString()) ? Float.parseFloat(value.toString()) : 0F;
        } else if (Double.class.getName().equals(fieldTypeClass.getName())
                || double.class.getName().equals(fieldTypeClass.getName())) {
            retVal = Objects.nonNull(value) && StringUtils.isNotEmpty(value.toString()) ? Double.parseDouble(value.toString()) : 0D;
        } else if (String.class.getName().equals(fieldTypeClass.getName())) {
            retVal = objectToString(value,genericType);
        } else {
            retVal = Objects.nonNull(value) && StringUtils.isNotEmpty(value.toString()) ? GsonUtils.stringToBean(value.toString().trim(),genericType):"";
        }
        return retVal;
    }

    private static String objectToString (Object value, Type genericType) {
        try {
            return Objects.nonNull(value) && StringUtils.isNotEmpty(value.toString()) ? GsonUtils.stringToBean(value.toString(),genericType) : "";
        } catch (JsonSyntaxException jsonSyntaxException) {
            return value.toString();
        }
    }


    public static Object getObjDefault(Object param) {
        if (Objects.isNull(param)) {
            return null;
        }
        if (param instanceof Integer) {
            return param;
        } else if (param instanceof String) {
            return param;
        } else if (param instanceof Double) {
            return param;
        } else if (param instanceof Float) {
            return param;
        } else if (param instanceof Long) {
            return param;
        } else if (param instanceof Boolean) {
            return param;
        } else if (param instanceof Date) {
            return param;
        } else {
            return GsonUtils.toJsonString(param);
        }
    }

    /**
     * 转 String
     * @param key
     * @return
     */
    public static String getKeyStr (Object key) {
        if(key instanceof  String) {
            return key.toString();
        } else {
            return String.valueOf(key);
        }
    }

}
