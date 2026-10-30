package com.ddm.server.common.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RefField {
    boolean iskey() default false;

    boolean isrequird() default true;

    boolean isfield() default true;
}
