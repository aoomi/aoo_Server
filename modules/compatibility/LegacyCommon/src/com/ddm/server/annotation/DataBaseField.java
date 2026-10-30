package com.ddm.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataBaseField {

    public enum IndexType {
        None, // 非Key类型
        Unique, // 唯一索引
        Normal, // 普通索引
    }

    /**
     * @return 数据库字段类型
     */
    public String type();

    /**
     * @return 类型为数组情况下的数组长度 0 表示非数组
     */
    public int size() default 0;

    /**
     * @return 数据库字段名称 - 默认使用java定义的名称
     */
    public String fieldname() default "";

    /**
     * @return 数据库注释
     */
    public String comment();

    /**
     * @return 索引类型 - 默认无索引
     */
    public IndexType indextype() default IndexType.None;

    /**
     * 默认值
     *
     * @return
     */
    public String defaultValue() default "";
}
