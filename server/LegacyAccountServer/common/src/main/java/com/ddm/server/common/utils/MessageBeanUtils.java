package com.ddm.server.common.utils;


public class MessageBeanUtils {
    /**
     * 消息转换成对应的数据类型
     *
     * @param message
     * @param clazz
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> T messageToBean(Object message, Class<T> clazz){
        if (message instanceof String) {
            return GsonUtils.stringToBean((String) message, clazz);
        } else {
            return (T) message;
        }
    }

    /**
     * 消息转换成对应的数据类型
     *
     * @param message
     * @return
     * @throws Exception
     */
    public static String messageToString(Object message){
        if (message instanceof String) {
            return (String) message;
        } else {
            return GsonUtils.toJsonString(message);
        }
    }
}
