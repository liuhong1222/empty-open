package com.zhongzhi.empty.service.customer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zhongzhi.empty.constants.CaffeineConstant;
import com.zhongzhi.empty.dao.ApiSettingsMapper;
import com.zhongzhi.empty.entity.ApiSettings;
import com.zhongzhi.empty.redis.RedisClient;

/**
 * api账号实现类
 * @author liuh
 * @date 2021年10月26日
 */
@Slf4j
@Service
public class ApiSettingsService {

    @Autowired
    private ApiSettingsMapper apiSettingsMapper;

    @Lazy
    @Autowired
    private RedisClient redisClient;
    
    @Cacheable(cacheManager = "caffeineCacheManager", value = CaffeineConstant.API_SETTINGS_INFO, key = "#appId", unless = "#result == null")
    @Transactional(readOnly = true)
    public ApiSettings getByAppId(String appId) {
        return apiSettingsMapper.getByAppId(appId);
    }
    
    @CacheEvict(cacheManager = "caffeineCacheManager", cacheNames = {CaffeineConstant.API_SETTINGS_INFO}, key = "#appId")
    public void refreshGetAppId(String appId) {
    	log.info("api账号信息缓存刷新成功，appId:{}",appId);
    }
}
