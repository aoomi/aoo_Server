package com.ddm.server.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Consumer {
    /**
     * topic名称
     */
    String topic() default "";

    /**
     * 表达式
     */
    String subExpression() default "*";

    /**
     * 游戏id区分不同游戏
     *
     * @return
     */
    int id() default -1;
}
