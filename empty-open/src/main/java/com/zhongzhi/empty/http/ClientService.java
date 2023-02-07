package com.zhongzhi.empty.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * clientservice注解
 * @author liuh
 * @date 2021年10月28日
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClientService {
    // 注解的属性的设置类似于方法,之所以是String[]是因为这样的话可以不同的key关键字可以对应同一个支付服务
    public String[] interfaces();
}