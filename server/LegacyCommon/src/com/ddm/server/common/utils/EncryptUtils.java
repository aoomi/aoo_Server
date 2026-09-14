package com.ddm.server.common.utils;

import org.apache.commons.lang3.StringUtils;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptUtils {

    //前端对称加密算法
    private static final String DES_ALGORITHM = "DES";
    private static final String DEFAULT_DES_KEY = "ds34524djsk";

    /**
     * DES加密操作
     *
     * @param source 要加密的源
     * @return
     */
    public static String encryptDES(String source) {
        return encryptDES(source, DEFAULT_DES_KEY);
    }

    /**
     * DES加密操作
     *
     * @param source 要加密的源
     * @param key    约定的密钥
     * @return
     */
    public static String encryptDES(String source, String key) {
        if (StringUtils.isEmpty(source)) {
            return null;
        }
        //强加密随机数生成器
        SecureRandom random = new SecureRandom();
        try {
            //创建密钥规则
            DESKeySpec keySpec = new DESKeySpec(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            //创建密钥工厂
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            //按照密钥规则生成密钥
            SecretKey secretKey = keyFactory.generateSecret(keySpec);
            //加密对象
            Cipher cipher = Cipher.getInstance(DES_ALGORITHM);
            //初始化加密对象需要的属性
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, random);
            //开始加密
            byte[] result = cipher.doFinal(source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            //Base64加密
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
        return null;
    }


    /**
     * 解密
     *
     * @param cryptograph 密文
     * @return
     */
    public static String decryptDES(String cryptograph) {
        return decryptDES(cryptograph, DEFAULT_DES_KEY);
    }


    /**
     * 解密
     *
     * @param cryptograph 密文
     * @param key         约定的密钥
     * @return
     */
    public static String decryptDES(String cryptograph, String key) {
        if (StringUtils.isEmpty(cryptograph)) {
            return null;
        }
        //强加密随机生成器
        SecureRandom random = new SecureRandom();
        try {
            //定义私钥规则
            DESKeySpec keySpec = new DESKeySpec(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            //定义密钥工厂
            SecretKeyFactory factory = SecretKeyFactory.getInstance("DES");
            //按照密钥规则生成密钥
            SecretKey secretkey = factory.generateSecret(keySpec);
            //创建加密对象
            Cipher cipher = Cipher.getInstance(DES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretkey, random);
            //Base64对
            byte[] result = Base64.getMimeDecoder().decode(cryptograph);
            return new String(cipher.doFinal(result), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        //前端对称加密解码
        String pwd = "123456";
        String cryptograph = encryptDES(pwd, DEFAULT_DES_KEY);
        System.out.println(cryptograph);
        System.out.println(decryptDES(cryptograph, DEFAULT_DES_KEY));
    }

}
