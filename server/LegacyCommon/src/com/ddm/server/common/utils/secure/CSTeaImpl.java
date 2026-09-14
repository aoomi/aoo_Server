package com.ddm.server.common.utils.secure;

/**
 * Tea对称加密算法内部实现
 *
 * @author aurain
 */
public class CSTeaImpl {
    /**
     * TEA算法标准值
     */
    private int DELTA = 0x9e3779b9;

    /**
     * 加密
     *
     * @param content
     * @param offset
     * @param key
     * @param times
     * @return
     */
    public byte[] encrypt(byte[] content, int offset, int[] key, int times) {
        // times为加密轮数
        int[] tempInt = byteToInt(content, offset);
        int y = tempInt[0], z = tempInt[1], sum = 0, i;
        int a = key[0], b = key[1], c = key[2], d = key[3];

        for (i = 0; i < times; i++) {
            sum += DELTA;
            y += ((z << 4) + a) ^ (z + sum) ^ ((z >> 5) + b);
            z += ((y << 4) + c) ^ (y + sum) ^ ((y >> 5) + d);
        }
        tempInt[0] = y;
        tempInt[1] = z;
        return intToByte(tempInt, 0);
    }

    /**
     * 解密
     *
     * @param encryptContent
     * @param offset
     * @param key
     * @param times
     * @return
     */
    public byte[] decrypt(byte[] encryptContent, int offset, int[] key, int times) {
        int[] tempInt = byteToInt(encryptContent, offset);
        int y = tempInt[0], z = tempInt[1], sum = 0, i;
        int a = key[0], b = key[1], c = key[2], d = key[3];
        if (times == 32) {
            sum = 0xC6EF3720; /* delta << 5 */
        } else if (times == 16) {
            sum = 0xE3779B90; /* delta << 4 */
        } else {
            sum = DELTA * times;
        }

        for (i = 0; i < times; i++) {
            z -= ((y << 4) + c) ^ (y + sum) ^ ((y >> 5) + d);
            y -= ((z << 4) + a) ^ (z + sum) ^ ((z >> 5) + b);
            sum -= DELTA;
        }
        tempInt[0] = y;
        tempInt[1] = z;

        return intToByte(tempInt, 0);
    }

    /**
     * byte[]型数据转成int[]型数据
     *
     * @param content
     * @param offset
     * @return
     */
    private int[] byteToInt(byte[] content, int offset) {

        int[] result = new int[content.length >> 2];// 除以2的n次方 == 右移n位 即
        // content.length / 4 ==
        // content.length >> 2
        for (int i = 0, j = offset; j < content.length; i++, j += 4) {
            result[i] = transform(content[j + 3]) | transform(content[j + 2]) << 8 | transform(content[j + 1]) << 16 | (int) content[j] << 24;
        }

        return result;
    }

    /**
     * int[]型数据转成byte[]型数据
     *
     * @param content
     * @param offset
     * @return
     */
    private byte[] intToByte(int[] content, int offset) {
        byte[] result = new byte[content.length << 2];// 乘以2的n次方 == 左移n位 即
        // content.length * 4 ==
        // content.length << 2
        for (int i = 0, j = offset; j < result.length; i++, j += 4) {
            result[j + 3] = (byte) (content[i] & 0xff);
            result[j + 2] = (byte) ((content[i] >> 8) & 0xff);
            result[j + 1] = (byte) ((content[i] >> 16) & 0xff);
            result[j] = (byte) ((content[i] >> 24) & 0xff);
        }

        return result;
    }

    /**
     * 若某字节为负数则需将其转成无符号正数
     *
     * @param temp
     * @return
     */
    private static int transform(byte temp) {
        int tempInt = (int) temp;
        if (tempInt < 0) {
            tempInt += 256;
        }
        return tempInt;
    }
}
