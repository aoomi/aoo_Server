package com.ddm.server.common.utils.secure;

import com.ddm.server.common.CommLogD;

import java.io.*;


public class SecureUtils {

    /**
     * byte数组转换成16进制字符串
     *
     * @param byteArray
     * @return
     */
    public static final String byteArray2StringHex(byte[] byteArray) {
        StringBuilder buf = new StringBuilder(byteArray.length * 2);
        int i;

        for (i = 0; i < byteArray.length; i++) {
            if (((int) byteArray[i] & 0xff) < 0x10) {
                // 如果bytes[i]补码的低8位小于 16 buf添加0
                buf.append("0");
            }
            // bytes[i] 的低8位，换算成16进制数，添加到buf
            buf.append(Long.toString((int) byteArray[i] & 0xff, 16));
        }
        return buf.toString();
    }

    /**
     * 16进制的字符串转换成byte数组
     *
     * @param str
     * @return
     */
    public static final byte[] stringHex2ByteArray(String str) {
        if (str == null) {
            return null;
        }
        int iCount = str.length() / 2;
        byte[] byarrRet = new byte[iCount];
        for (int i = 0; i < iCount; i++) {
            byarrRet[i] = (byte) (hex2Int(str.charAt(i * 2)) * 16 + hex2Int(str.charAt(i * 2 + 1)));
        }

        return byarrRet;
    }

    /**
     * 字符串转换成byte数组
     *
     * @param str
     * @return
     */
    public static final byte[] string2ByteArray(String str) {
        if (str == null) {
            return null;
        }

        return str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 十六进制描述的字符转换其对应的十进制数字
     *
     * @param ch
     * @return
     */
    public static final int hex2Int(char ch) {
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        } else if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }

        return -1;
    }

    /**
     * 传入加密的配置文件路径，返回解密后的临时文件所在路径
     *
     * @param strConfigFile
     * @return
     */
    @SuppressWarnings("all")
    public static final String decryptConfigFile(String strConfigFile) {
        String strConfigFileTmp = null;

        InputStream isEncrypt = null;
        try {
            isEncrypt = new FileInputStream(strConfigFile);
        } catch (FileNotFoundException e1) {
            CommLogD.error(null, e1);
        }

        if (isEncrypt == null) {
            return strConfigFileTmp;
        }

        // 按照 UTF-8 编码方式将字节流转化为字符流
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(isEncrypt, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            CommLogD.error(null, e1);
        }

        if (isr == null) {
            return strConfigFileTmp;
        }

        String strLine;
        try (BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sbContent = new StringBuilder();
            strLine = br.readLine();
            while (strLine != null) {
                sbContent.append(strLine);
                strLine = br.readLine();
            }

            sbContent.deleteCharAt(0);

            CSTea tea = new CSTea();
            String strPlainText = tea.decrypt(stringHex2ByteArray(sbContent.toString()), CSTea.CONFIG_FILE_KEY);

            strConfigFileTmp = strConfigFile + ".tmp";

            // 如果文件存在，先删除
            File file = new File(strConfigFileTmp);
            if (file.isFile() && file.exists()) {
                file.delete();
            }

            try (FileOutputStream fosTemp = new FileOutputStream(strConfigFileTmp)) {
                fosTemp.write(strPlainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            CommLogD.error(null, e);
        }

        return strConfigFileTmp;
    }
}
