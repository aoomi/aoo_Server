package com.ddm.server.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TableName {
    public enum DbDayEnum {
        /**
         * 没有时间
         */
        NOT_DAY,
        /**
         * 每一天
         */
        EVERY_DAY,
        /**
         * 下一天
         */
        NEXT_DAY,
        /**
         * 前一天
         */
        BEFORE_DAY,
        /**
         * 每一天6点
         */
        EVERY_6DAY,
        /**
         * 下一天6点
         */
        NEXT_6DAY,
        /**
         * 前一天6点
         */
        BEFORE_6DAY,
    }

    String value();


    boolean fieldMappingOverrides() default false;

    /**
     * 每日一表
     *
     * @return
     */
    DbDayEnum dbDay() default DbDayEnum.NOT_DAY;


}