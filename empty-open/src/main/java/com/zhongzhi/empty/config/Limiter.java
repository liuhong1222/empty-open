package com.zhongzhi.empty.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 自定义注解 限流
 * @author liuh
 * @date 2021年10月29日
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Limiter {
    /*
     限制数量
     */
    double limitNum() default 50;      //默认每秒产生10个令牌

    /*
     key
     */
    String key() default "";

    /*
     获取令牌最大等待时间
     */
    long timeout() default 500;

    /*
     单位(例:分钟/秒/毫秒) 默认:毫秒
     */
    TimeUnit timeunit() default TimeUnit.MILLISECONDS;

    /*
     限流类型
     */
    Limiter.LimitType limitType() default LimitType.DEFAULT;

    enum LimitType {
        /*
         默认策略：根据请求方法名
         */
        DEFAULT,
        /*
         自定义key
         */
        CUSTOMER,
        /*
         根据请求者IP
         */
        IP
    }
}
