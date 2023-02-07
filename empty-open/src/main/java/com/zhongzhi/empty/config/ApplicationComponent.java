package com.zhongzhi.empty.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.zhongzhi.empty.constants.CacheConstant;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.task.CustomerConsumeDataTask;
import com.zhongzhi.empty.task.EmptyCheckDataTask;
import com.zhongzhi.empty.task.MobilePoolDataTask;
import com.zhongzhi.empty.task.RealtimeCheckDataTask;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;

/**
 * 线程管理
 * @author liuh
 * @date 2021年10月29日
 */
@Slf4j
@Component
public class ApplicationComponent implements ApplicationRunner {

    public static volatile boolean popStop = false;

    @Autowired
    private EmptyCheckDataTask emptyCheckDataTask;

    @Autowired
    private CustomerConsumeDataTask customerConsumeDataTask;
    
    @Autowired
    private MobilePoolDataTask mobilePoolDataTask;
    
    @Autowired
    private RealtimeCheckDataTask realtimeCheckDataTask;
    
    @Autowired
    private RedisClient redisClient;

    @Override
    public void run(ApplicationArguments args) {
    	log.info("系统定时任务启动中>>>>>>>>>>>>>>>>>>>>>");
    	//redis消息订阅
        redisClient.subscribe(CacheConstant.CACHE_REFRESH_CHANNEL);
    }

    private void stop() {
        emptyCheckDataTask.kill();
        customerConsumeDataTask.kill();
        mobilePoolDataTask.kill();
        realtimeCheckDataTask.kill();
        log.info("系统定时任务停止完成----------------------");
    }

    @PreDestroy
    private void stopPop() {
    	log.info("系统定时任务停止中>>>>>>>>>>>>>>>>>>>>>>>>");
        popStop = true;
        stop();
    }
}
