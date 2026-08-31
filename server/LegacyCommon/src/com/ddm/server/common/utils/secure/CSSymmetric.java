package com.ddm.server.common.utils.secure;

public class CSSymmetric {
    /**
     * 自定义的对称加密算法
     *
     * @param strPlain 需要加密的明文
     * @param strKey   密钥
     * @return 转换成16进制的加密后的数据
     */
    public static String encrypt(String strPlain, String strKey) {
        if (strPlain == null) {
            return null;
        }

        if (strKey == null) {
            return strPlain;
        }

        byte[] byarrPlain = strPlain.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] byarrKey = SecureUtils.stringHex2ByteArray(strKey);
        int iPlainCount = byarrPlain.length;
        int iKeyCount = byarrKey.length;
        byte[] byarrCipher = new byte[iPlainCount];
        for (int i = 0; i < iPlainCount; i++) {
            byarrCipher[i] = (byte) (byarrPlain[i] ^ byarrKey[i % iKeyCount]);
        }

        return SecureUtils.byteArray2StringHex(byarrCipher);
    }

    /**
     * 自定义的对称解密算法
     *
     * @param strCipher 需要解密的密文
     * @param strKey    密钥
     * @return 解密后的明文
     */
    public static String decrypt(String strCipher, String strKey) {
        if (strCipher == null) {
            return null;
        }

        if (strKey == null) {
            return strCipher;
        }

        byte[] byarrCipher = SecureUtils.stringHex2ByteArray(strCipher);
        byte[] byarrKey = SecureUtils.stringHex2ByteArray(strKey);
        int iCipherCount = byarrCipher.length;
        int iKeyCount = byarrKey.length;
        byte[] byarrPlain = new byte[iCipherCount];
        for (int i = 0; i < iCipherCount; i++) {
            byarrPlain[i] = (byte) (byarrCipher[i] ^ byarrKey[i % iKeyCount]);
        }

        return new String(byarrPlain, java.nio.charset.StandardCharsets.UTF_8);
    }
}
