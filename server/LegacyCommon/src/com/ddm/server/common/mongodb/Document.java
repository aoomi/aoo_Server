package com.ddm.server.common.mongodb;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Document {
    String collection() default "";
}
