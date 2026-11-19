package com.ddm.server.common.utils;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Objects;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author:zjm
 * @version:1.0 类辅助器
 */
public class ClassHelper {

    /**
     * 空格个数
     */
    private static int blankSpaceNum = 0;

    public static void parsingJson(Object o, Class<?> c, StringBuilder stringBuilder) {
        Objects.requireNonNull(o, "object should not be null.");
        stringBuilder.append("{\n");
        parsingStructure(o, c, stringBuilder, false);
        stringBuilder.append("}");
    }

    public static void sysOutParsingJson(Object o, Class<?> c) {
        Objects.requireNonNull(o, "object should not be null.");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\n");
        parsingStructure(o, c, stringBuilder, false);
        stringBuilder.append("}");
        System.getLogger(ClassHelper.class.getName()).log(System.Logger.Level.DEBUG, stringBuilder.toString());
    }

    /**
     * 获取结构体解析
     *
     * @param o
     * @param c
     * @param stringBuilder
     * @param isSuperClass
     */
    @SuppressWarnings("rawtypes")
    private static void parsingStructure(Object o, Class<?> c, StringBuilder stringBuilder, boolean isSuperClass) {
        //不是超类就加类名输出
        if (!isSuperClass) {
            addToBuilderAndBlank(stringBuilder, resolveQuotesToStringAndEnum(c.getSimpleName()) + ":{\n");
        }
        // 获取类中的所有定义字段
        Field[] fields = c.getDeclaredFields();

        blankSpaceNum++;
        // 循环遍历字段，获取字段对应的属性值
        for (Field field : fields) {
            // 如果不为空，设置可见性，然后返回
            field.setAccessible(true);
            resolveField(o, field, stringBuilder);
        }
        blankSpaceNum--;

        // 获取父类，判断是否为实体类
        Class superClass = c.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            parsingStructure(o, superClass, stringBuilder, true);
        } else {
            if (fields.length > 0) {
                stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), "\n");
            }
            addToBuilderAndBlank(stringBuilder, "}\n");
        }
    }

    /**
     * 处理field
     *
     * @param o
     * @param field
     * @param stringBuilder
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void resolveField(Object o, Field field, StringBuilder stringBuilder) {
        try {
            if (field.get(o) == null) {
                return;
            }
            //特殊类型List处理
            if (field.get(o) instanceof List || field.get(o).getClass().isArray()) {
                List list;
                if (field.get(o).getClass().isArray()) {
                    list = Arrays.asList(((Object[]) field.get(o))).stream().filter(m -> m != null).collect(Collectors.toList());
                    if (list.size() <= 0) {
                        addToBuilderAndBlank(stringBuilder, resolveQuotesToStringAndEnum(field.getName()) + ":\"\",\n");
                        return;
                    }
                } else {
                    list = ((List) field.get(o));
                }
                if (list != null && list.size() > 0) {
                    if (!isPrimitive(list.get(0).getClass())) {
                        addToBuilderAndBlank(stringBuilder, resolveQuotesToStringAndEnum(field.getName()) + ":[{\n");
                        blankSpaceNum++;
                        list.stream().forEach(listItem -> {
                            parsingStructure(listItem, listItem.getClass(), stringBuilder, false);
                            stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length(), ",\n");
                        });
                        stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), "\n");
                        blankSpaceNum--;
                        addToBuilderAndBlank(stringBuilder, "}],\n");
                        return;
                    }
                }
            } else if (field.get(o) instanceof Map) {
                Map map = ((Map) field.get(o));
                if (map != null && map.size() > 0) {
                    boolean isNoPrimitive = map.keySet().stream().anyMatch(n -> !isPrimitive(n.getClass()));//是否是基础类型
                    if (!isNoPrimitive) {
                        isNoPrimitive = map.values().stream().anyMatch(n -> !isPrimitive(n.getClass()));//是否是基础类型
                        if (isNoPrimitive) {
                            addToBuilderAndBlank(stringBuilder, resolveQuotesToStringAndEnum(field.getName()) + ":{\n");
                            blankSpaceNum++;
                            map.entrySet().stream().forEach(mapItem -> {
                                addToBuilderAndBlank(stringBuilder, resolveQuotesToStringAndEnum(((Map.Entry) mapItem).getKey()) + ":{\n");
                                blankSpaceNum++;
                                parsingStructure(((Map.Entry) mapItem).getValue(), ((Map.Entry) mapItem).getValue().getClass(), stringBuilder, false);
                                blankSpaceNum--;
                                addToBuilderAndBlank(stringBuilder, "},\n");
                            });
                            stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), "\n");
                            blankSpaceNum--;
                            addToBuilderAndBlank(stringBuilder, "},\n");
                            return;
                        }
                    }
                }
            } else if (field.get(o) != null && isNoFilterType(field.get(o)) && !isPrimitive(field.get(o).getClass())) {
                addToBuilderAndBlank(stringBuilder, resolveQuotesToStringAndEnum(field.getName()) + ":{\n");
                blankSpaceNum++;
                parsingStructure(field.get(o), field.get(o).getClass(), stringBuilder, false);
                blankSpaceNum--;
                addToBuilderAndBlank(stringBuilder, "},\n");
                return;
            }
            addToBuilderAndBlank(stringBuilder, resolveQuotesToStringAndEnum(field.getName()) + ":" + resolveQuotesToStringAndEnum(field.get(o)) + ",\n");
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
    }

    /**
     * 是不是基础类型
     *
     * @param type
     * @return
     */
    private static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Character.class
                || type == Boolean.class
                || type == Byte.class
                || type == Short.class
                || type == Integer.class
                || type == Long.class
                || type == Float.class
                || type == Double.class
                || type == Object.class;
    }

    /**
     * 添加空格
     *
     * @param stringBuilder
     */
    private static void blankSpace(StringBuilder stringBuilder) {
        StringBuilder blank = new StringBuilder();
        for (int i = 0; i < blankSpaceNum; i++) {
            blank.append("\t");
        }
        stringBuilder.append(blank);
    }

    /**
     * 添加字符串前加空格
     *
     * @param stringBuilder
     * @param appendString
     */
    private static void addToBuilderAndBlank(StringBuilder stringBuilder, String appendString) {
        blankSpace(stringBuilder);
        stringBuilder.append(appendString);
    }

    /**
     * 字符串和Emun或者空字符串或者boolean加引号
     *
     * @param object
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String resolveQuotesToStringAndEnum(Object object) {
        StringBuilder stringBuilder = new StringBuilder();
        if (object instanceof String || object instanceof Enum || object == null || object instanceof Boolean) {
            stringBuilder.append("\"");
            stringBuilder.append(object == null || String.valueOf(object).length() <= 0 ? "" : object);
            stringBuilder.append("\"");
        } else if (object instanceof List) {
            stringBuilder.append("[");
            ((List) object).stream().forEach(m -> {
                stringBuilder.append("\"" + m + "\"" + ",");
            });
            if (((List) object).size() > 0) {
                stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length(), "");
            }
            stringBuilder.append("]");
        } else if (object instanceof Map) {
            stringBuilder.append("{");
            (((Map) object).entrySet()).stream().forEach(m -> {
                stringBuilder.append("\"" + ((Map.Entry) m).getKey() + "\"" + ":" + resolveQuotesToStringAndEnum(((Map.Entry) m).getValue()) + ",");
            });
            if (((Map) object).size() > 0) {
                stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length(), "");
            }
            stringBuilder.append("}");
        } else if (isPrimitive(object.getClass())) {
            stringBuilder.append(object);
        } else {
            stringBuilder.append("\"");
            stringBuilder.append(object.toString());
            stringBuilder.append("\"");
        }
        return stringBuilder.toString();
    }

    /**
     * 过滤掉会互相引用的类型
     *
     * @param object
     * @return
     */
    private static boolean isNoFilterType(Object object) {
        if (object instanceof ReferenceQueue || object instanceof Reference
                || object instanceof java.security.Permission || object instanceof Enum) {
            return false;
        }
        return true;
    }

    public static void checkHasAllTestMethod(Class<?> sourceClass, Class<?> targetClass) {
        try {
            Pattern p = Pattern.compile("[\\d]");
            Method[] sourceMethods = sourceClass.getDeclaredMethods();
            Method[] targetMethod = targetClass.getDeclaredMethods();
            List<String> targeMethodNames = Arrays.stream(targetMethod).map(source -> {
                Matcher matcher = p.matcher(source.getName());
                return matcher.replaceAll("");
            }).collect(Collectors.toList());
            Arrays.stream(sourceMethods).forEach(method -> {
                if (!method.getName().startsWith("lambda")) {
                    StringBuilder sb = new StringBuilder(method.getName());
                    sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
                    if (!targeMethodNames.contains("test" + sb.toString())) {
                        System.getLogger(ClassHelper.class.getName()).log(System.Logger.Level.DEBUG, "没有测试方法: test" + sb);
                    } else {
                        targeMethodNames.remove("test" + sb.toString());
                    }
                }
            });
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
    }
}
