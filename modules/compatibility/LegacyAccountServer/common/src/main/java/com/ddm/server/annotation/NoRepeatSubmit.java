package com.ddm.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防止重复提交标记注解活动
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoRepeatSubmit {
    /**
     * 重复统计时长
     *
     * @return
     */
    int time() default 3;

    /**
     * 存在单次活动
     * @return
     */
    boolean existSingleActivity() default false;

}
