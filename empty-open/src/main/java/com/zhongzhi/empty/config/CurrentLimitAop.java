package com.zhongzhi.empty.config;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.RateLimiter;
import com.zhongzhi.empty.constants.RedisKeyConstant;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.exception.LimitException;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.util.IpUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 自定义限流 AOP
 * @author liuh
 * @date 2021年10月29日
 */
@Slf4j
@Aspect
@Component
public class CurrentLimitAop {

    private RateLimiter rateLimiter;

    @Resource
    private RedisClient redisClient;

    //创建一个ConcurrentHashMap来存放各个方法和它们自己对应的RateLimiter对象
    private static final ConcurrentMap<String, RateLimiter> RATE_LIMITER = new ConcurrentHashMap<>();

    // 限流 切点
    @Pointcut("@annotation(com.zhongzhi.empty.config.Limiter)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法的签名
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        //获取注解信息
        Limiter annotation = method.getAnnotation(Limiter.class);
        //获取注解每秒加入桶中的token
        double limitNum = annotation.limitNum();
        // 注解所在方法名区分不同的限流策略
        Limiter.LimitType limitType = annotation.limitType();
        String key = annotation.key();

        if (limitType.equals(Limiter.LimitType.IP)) {
            // 注解请求IP区分不同的限流策略
            key = IpUtil.getRequestIp();
        } else if (limitType.equals(Limiter.LimitType.CUSTOMER)) {
            // 自定义限流策略：注解所在方法名+IP区分不同的限流策略
            key = methodSignature.getName() + ":" + IpUtil.getRequestIp();
        } else {
            // 注解所在方法名区分不同的限流策略
            key = methodSignature.getName();
        }
        if (!RATE_LIMITER.containsKey(key)) {
            RATE_LIMITER.put(key, RateLimiter.create(limitNum));
        }
        rateLimiter = RATE_LIMITER.get(key);
        // 如果用户在500毫秒内没有获取到令牌,就直接放弃获取进行服务降级处理
        if (rateLimiter.tryAcquire(annotation.timeout(), annotation.timeunit())) {
            // 继续执行
            return joinPoint.proceed();
        } else {
            // 记录IP地址
            if (StringUtils.isBlank(redisClient.get(RedisKeyConstant.CURRENT_LIMIT_KEY+key))) {
                // 缓存到Redis
            	redisClient.set(RedisKeyConstant.CURRENT_LIMIT_KEY+key, "1", 3600 * 5);
            } else {
            	redisClient.incr(RedisKeyConstant.CURRENT_LIMIT_KEY+key);
            }
            // 拒绝了请求（服务降级）
            log.error("请求速率超限，key: {}, method: {}", key, methodSignature.getMethod().getName());
            throw new LimitException(ApiCode.LIMITER_EXCEPTION.getCode(),"请求速率超限");
        }
    }

}
