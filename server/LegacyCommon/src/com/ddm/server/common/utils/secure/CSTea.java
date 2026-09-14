package com.ddm.server.common.utils.secure;

/**
 * Tea对称加密算法外部接口 Tea tea = new Tea(); String strInfo =
 * "@www.cityofsteam.com/1234567890@"; String strCipherText =
 * tea.encrypt(strInfo, "9aefe6eed716dc2f290cb371f52c5d36"); String strPlainText
 * = tea.decrypt(SecureCommon.stringHex2ByteArray(strCipherText),
 * "9aefe6eed716dc2f290cb371f52c5d36");
 *
 * @author aurain
 */
public class CSTea {
    public static final String CONFIG_FILE_KEY = "789f5645f68bd5a481963ffa458fac58";

    /**
     * 加密轮数
     */
    private int TEA_TIMES = 32;

    /**
     * 通过TEA算法加密
     *
     * @param strPlainText ，明文字符串
     * @return 密文字符串
     */
    public String encrypt(String strPlainText, String strKey) {
        CSTeaImpl teaImpl = new CSTeaImpl();
        byte[] byarrPlainText = strPlainText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int iPaddingCount = 8 - byarrPlainText.length % 8;// 若temp的位数不足8的倍数,需要填充的位数
        byte[] byarrEncrypt = new byte[byarrPlainText.length + iPaddingCount];
        byarrEncrypt[0] = (byte) iPaddingCount;
        System.arraycopy(byarrPlainText, 0, byarrEncrypt, iPaddingCount, byarrPlainText.length);
        byte[] byarrCipherText = new byte[byarrEncrypt.length];
        for (int offset = 0; offset < byarrCipherText.length; offset += 8) {
            byte[] tempEncrpt = teaImpl.encrypt(byarrEncrypt, offset, _generateKey(strKey), TEA_TIMES);
            System.arraycopy(tempEncrpt, 0, byarrCipherText, offset, 8);
        }
        return SecureUtils.byteArray2StringHex(byarrCipherText);
    }

    /**
     * 通过TEA算法解密
     *
     * @param byarrCipher
     * @param strKey
     * @return
     */
    public String decrypt(byte[] byarrCipher, String strKey) {
        CSTeaImpl teaImpl = new CSTeaImpl();
        byte[] byarrDecrypt = null;
        byte[] byarrDecryptTmp = new byte[byarrCipher.length];
        for (int offset = 0; offset < byarrCipher.length; offset += 8) {
            byarrDecrypt = teaImpl.decrypt(byarrCipher, offset, _generateKey(strKey), TEA_TIMES);
            System.arraycopy(byarrDecrypt, 0, byarrDecryptTmp, offset, 8);
        }

        int iPadding = byarrDecryptTmp[0];
        return new String(byarrDecryptTmp, iPadding, byarrDecrypt.length - iPadding, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 将32位的字符串密钥转换成一个int数组，数组大小为4
     *
     * @param strKey
     * @return
     */
    private int[] _generateKey(String strKey) {
        int[] iarrRet = new int[4];
        String strKey1;
        String strKey2;
        String strKey3;
        String strKey4;

        strKey1 = strKey.substring(0, 8);
        strKey2 = strKey.substring(8, 16);
        strKey3 = strKey.substring(16, 24);
        strKey4 = strKey.substring(24, 32);

        iarrRet[0] = Long.valueOf(strKey1, 16).intValue();
        iarrRet[1] = Long.valueOf(strKey2, 16).intValue();
        iarrRet[2] = Long.valueOf(strKey3, 16).intValue();
        iarrRet[3] = Long.valueOf(strKey4, 16).intValue();

        return iarrRet;
    }
}
