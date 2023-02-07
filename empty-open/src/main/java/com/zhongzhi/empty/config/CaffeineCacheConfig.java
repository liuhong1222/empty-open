package com.zhongzhi.empty.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * caffeine本地缓存
 * @author liuh
 * @date 2021年10月26日
 */
@Configuration
@EnableCaching
public class CaffeineCacheConfig {
    private static final int DEFAULT_MAXSIZE = 1000;
    private static final int DEFAULT_TTL = 3600;

    @Bean("caffeineCacheManager")
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        List<CaffeineCache> caches = new ArrayList<>();
        for (Caches c : Caches.values()) {
            caches.add(new CaffeineCache(c.name(), Caffeine.newBuilder().expireAfterAccess(c.getTtl(), TimeUnit.SECONDS).maximumSize(c.maxSize).build()));
        }
        manager.setCaches(caches);
        return manager;
    }

    /**
     * 定义cache名称、超时时长秒、最大个数
     * 每个cache缺省3600秒过期，最大个数1000
     */
    public enum Caches {

    	ec_v1_api_settings(3*24*60*60,50000),
    	ec_v1_customer(3*24*60*60,50000),
    	ec_v1_agent(3*24*60*60,50000),
    	ec_v1_empty_gateway(3*24*60*60,50000)
    	;

        private int maxSize = DEFAULT_MAXSIZE;    //最大數量
        private int ttl = DEFAULT_TTL;        //过期时间（秒）

        Caches() {
        }

        Caches(int ttl) {
            this.ttl = ttl;
        }

        Caches(int ttl, int maxSize) {
            this.ttl = ttl;
            this.maxSize = maxSize;
        }

        public String getName(){
            return this.name();
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getTtl() {
            return ttl;
        }

        public void setTtl(int ttl) {
            this.ttl = ttl;
        }
    }

}
