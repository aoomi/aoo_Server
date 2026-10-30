package com.ddm.server.common.utils.secure;

import com.ddm.server.common.CommLogD;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;


public class CSRSAImpl {
    /**
     * RSA公钥加密
     *
     * @param _sRSAModulus
     * @param _sRSAPublic
     * @param _sPlain
     * @return 使用Base64编码后的字符串
     */
    public static String encryptPublic(String _sRSAModulus, String _sRSAPublic, String _sPlain) {
        BigInteger modulus = new BigInteger(_sRSAModulus, 10);
        BigInteger pubExp = new BigInteger(_sRSAPublic, 10);

        KeyFactory keyFactory;

        try {
            keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus, pubExp);
            RSAPublicKey key = (RSAPublicKey) keyFactory.generatePublic(pubKeySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] cipherData = cipher.doFinal(_sPlain.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            return Base64.encodeBase64String(cipherData);
        } catch (Exception e) {
            CommLogD.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * RSA私钥解密
     *
     * @param _sRSAModulus
     * @param _sRSAPrivate
     * @param _sCipher
     * @return
     */
    public static String decryptPrivate(String _sRSAModulus, String _sRSAPrivate, String _sCipher) {
        BigInteger modulus = new BigInteger(_sRSAModulus, 10);
        BigInteger privExp = new BigInteger(_sRSAPrivate, 10);

        KeyFactory keyFactory;

        try {
            keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKeySpec privKeySpec = new RSAPrivateKeySpec(modulus, privExp);
            RSAPrivateKey key = (RSAPrivateKey) keyFactory.generatePrivate(privKeySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] plainData = cipher.doFinal(Base64.decodeBase64(_sCipher));

            return new String(plainData, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            CommLogD.error(e.getMessage(), e);
        }

        return null;
    }
}
