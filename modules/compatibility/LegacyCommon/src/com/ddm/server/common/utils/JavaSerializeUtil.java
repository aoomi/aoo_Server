package com.ddm.server.common.utils;

import BaseCommon.CommLog;

import java.io.*;

/**
 * @author xsj
 * @date 2020/8/17 9:27
 * @description java序列化和反序列化
 */
public class JavaSerializeUtil {
    //序列化
    public static byte[] serialize(Object obj) {
        ObjectOutputStream obi = null;
        ByteArrayOutputStream bai = null;
        try {
            bai = new ByteArrayOutputStream();
            obi = new ObjectOutputStream(bai);
            obi.writeObject(obj);
            byte[] byt = bai.toByteArray();
            return byt;
        } catch (IOException e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            CommLog.error(e.getMessage(), e);
        }
        return null;
    }

    //反序列化
    public static Object unSerialize(byte[] byt) {
        ObjectInputStream oii = null;
        ByteArrayInputStream bis = null;
        bis = new ByteArrayInputStream(byt);
        try {
            oii = new ObjectInputStream(bis);
            Object obj = oii.readObject();
            return obj;
        } catch (Exception e) {
            CommLog.error(e.getMessage(), e);
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
        return null;
    }
}
