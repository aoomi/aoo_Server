package com.ddm.server.common.utils;

import org.springframework.context.ApplicationContext;

public class SpringBeanUtils {
    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        SpringBeanUtils.applicationContext = applicationContext;
    }

    public static <T> T Bean(Class<T> requiredType) {
        return SpringBeanUtils.applicationContext.getBean(requiredType);
    }
}
