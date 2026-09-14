package com.ddm.server.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class ArithUtils {

    /**
     * 向下取整（保留两位小数）
     * @param d
     * @return
     */
    public static double FormatDouble(double d) {
        BigDecimal bg = BigDecimal.valueOf(d).setScale(2, RoundingMode.DOWN);
        return bg.doubleValue();
    }

    /**
     * 向下取整（保留两位小数）
     * @param d
     * @return
     */
    public static double FormatDouble(String d) {
        BigDecimal bg = BigDecimal.valueOf(Double.parseDouble(d)).setScale(2, RoundingMode.DOWN);
        return bg.doubleValue();
    }

    /**
     * 加法运算
     * @param v1
     * @param v2
     * 向下取整（保留两位小数）
     * @return
     */
    public static double addDouble(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.add(p2).setScale(2, RoundingMode.DOWN).doubleValue();
    }

    /**
     * 加法运算
     * @param v1
     * @param v2
     * 向下取整（保留两位小数）
     * @return
     */
    public static double addDoubleNotNull(Double v1, Double v2) {
        double v1D = Objects.nonNull(v1) ? v1.doubleValue():0D;
        double v2D = Objects.nonNull(v2) ? v2.doubleValue():0D;
        Double m1 = Double.valueOf(Double.toString(v1D));
        Double m2 = Double.valueOf(Double.toString(v2D));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.add(p2).setScale(2, RoundingMode.DOWN).doubleValue();
    }

    /**
     * 减法运算
     * @param v1
     * @param v2
     * 向下取整（保留两位小数）
     * @return
     */
    public static double subDouble(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.subtract(p2).setScale(2, RoundingMode.DOWN).doubleValue();
    }



    /**
     * 乘法运算
     * @param v1
     * @param v2
     * 向下取整（保留两位小数）
     * @return
     */
    public static double mul(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.multiply(p2).setScale(2, RoundingMode.DOWN).doubleValue();
    }

    /**
     * 除法运算
     * @param v1
     * @param v2
     * 向下取整（保留两位小数）
     * @return
     */
    public static double div(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.divide(p2, 2, RoundingMode.DOWN).doubleValue();
    }

    /**
     * 大于
     * @param v1
     * @param v2
     * @return
     */
    public static boolean gt(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.compareTo(p2) > 0;
    }

    /**
     * 大于等于
     * @param v1
     * @param v2
     * @return
     */
    public static boolean ge(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.compareTo(p2) >= 0;
    }

    /**
     * 小于
     * @param v1
     * @param v2
     * @return
     */
    public static boolean lt(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.compareTo(p2) < 0;
    }

    /**
     * 小于等于
     * @param v1
     * @param v2
     * @return
     */
    public static boolean le(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.compareTo(p2) <= 0;
    }

    /**
     * 等于
     * @param v1
     * @param v2
     * @return
     */
    public static boolean eq(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.compareTo(p2) == 0;
    }


    /**
     * 不等于
     * @param v1
     * @param v2
     * @return
     */
    public static boolean ne(double v1, double v2) {
        return !eq(v1,v2);
    }


}
