package com.ddm.server.annotation;

import com.ddm.server.annotation.base.AutowiredManager;
import com.ddm.server.annotation.base.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@AutowiredManager
public @interface Dao {
    String dataSource() default "clark_game";
}
