package com.ddm.server.common.utils;

import java.io.*;

/**
 * 类序列化转换(主要是对对象转换成byte[]，和根据byte[]数组反序列化成java对象)
 *
 * @author yxm
 */
public class ObjectUtil {
    /**
     * 构造方法私有不可创建对象
     */
    private ObjectUtil() {

    }

    /**
     * 对象序列化(将对象转换成byte[])
     *
     * @param obj
     * @return
     * @throws IOException
     */
    public static byte[] objectToBytes(Object obj) throws Exception {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        byte[] bytes = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            bytes = bos.toByteArray();
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        } finally {
            if (bos != null) {
                bos.close();
            }
            if (oos != null) {
                oos.close();
            }
        }
        return bytes;
    }

    /**
     * 对象凡序列化(byte[] 转对象)
     *
     * @param bytes
     * @return
     * @throws Exception
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T bytesToObject(byte[] bytes) throws Exception {
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        T obj = null;
        try {
            //读取字节到内存
            bis = new ByteArrayInputStream(bytes);
            //转换流
            ois = new ObjectInputStream(bis);
            //变成对象
            obj = (T) ois.readObject();
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return obj;
    }
}