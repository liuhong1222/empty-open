package com.zhongzhi.empty.interceptor;

import java.lang.annotation.*;

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Interceptor {
    String name() default "signInterceptorHandleService";
}

