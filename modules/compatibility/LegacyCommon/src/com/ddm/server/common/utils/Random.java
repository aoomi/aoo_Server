/**
 * License THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS CREATIVE COMMONS PUBLIC LICENSE ("CCPL" OR "LICENSE"). THE WORK IS
 * PROTECTED BY COPYRIGHT AND/OR OTHER APPLICABLE LAW. ANY USE OF THE WORK OTHER THAN AS AUTHORIZED UNDER THIS LICENSE OR COPYRIGHT LAW IS PROHIBITED.
 * <p>
 * BY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND AGREE TO BE BOUND BY THE TERMS OF THIS LICENSE. TO THE EXTENT THIS LICENSE MAY
 * BE CONSIDERED TO BE A CONTRACT, THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.
 */
package com.ddm.server.common.utils;

import org.apache.commons.lang3.RandomUtils;

import java.util.UUID;

public class Random {

    public static int ZERO = 0;

    /**
     * 取得指定范围内整數
     *
     * @param n
     *            范围值, 取值[0,n)之间的随机数, n可取负数
     * @return
     */
    public static int nextInt(int n) {
        if (n == 0) {
            return 0;
        }
        int hash = UUID.randomUUID().hashCode();
        int res = n > 0 ? Math.floorMod(hash, n) : (int) -Math.floorMod(hash, -(long) n);
        return res;
    }

    // 取得整數+偏移

    /**
     * 取得指定范围内整數, 并加上 offset
     *
     * @param n
     * @param offset
     * @return
     */
    public static int nextInt(int n, int offset) {
        if (n == 0) {
            return offset;
        }
        int hash = UUID.randomUUID().hashCode();
        int res = n > 0 ? Math.floorMod(hash, n) : (int) -Math.floorMod(hash, -(long) n);
        return res + offset;
    }

    /**
     * 真假随机
     *
     * @return
     */
    public static boolean nextBoolean() {
        return (nextInt(2) == 1);
    }

    /**
     * 256范围内随机取值
     *
     * @return
     */
    public static byte nextByte() {
        return (byte) nextInt(256);
    }

    /**
     * 64位长整数随机
     *
     * @param n
     * @return
     */
    public static long nextLong(long n) {
        if (n == 0) {
            return 0;
        }
        long head = nextInt(Integer.MAX_VALUE);
        long l = nextInt(Integer.MAX_VALUE);

        long dividend = ((head << 32) + l);

        long remain = dividend - (dividend / n) * n;

        if (n < 0) {
            return 0 - remain;
        } else {
            return remain;
        }
    }

    public static boolean isTrue(float ratio) {
        float fl = (float) Math.random();
        if (fl <= ratio) {
            return true;
        }
        return false;
    }


    public static boolean isTrueDouble(double ratio) {
        double value = RandomUtils.nextDouble(0.1D, 100D);
        if (value <= ratio) {
            return true;
        }
        return false;
    }
}
