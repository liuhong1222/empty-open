package com.zhongzhi.empty.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.zhongzhi.empty.constants.CaffeineConstant;
import com.zhongzhi.empty.dao.SysConfigMapper;
import com.zhongzhi.empty.entity.SysConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * oem参数管理实现表
 * @author liuh
 * @date 2021年10月28日
 */
@Slf4j
@Service
public class SysConfigService {

	@Autowired
	private SysConfigMapper sysConfigMapper;
	
	@Cacheable(cacheManager = "caffeineCacheManager", value = CaffeineConstant.EMPTY_GATEWAY_INFO, key = "#keys", unless = "#result == null")
	public SysConfig findOneByKey(String keys) {
		return sysConfigMapper.findOneByKey(keys);
	}
	
	@CacheEvict(cacheManager = "caffeineCacheManager", cacheNames = {CaffeineConstant.EMPTY_GATEWAY_INFO}, key = "#keys")
	public void refreshFindOneByKey(String keys) {
		log.info("空号检测通道缓存刷新成功");
	}
}
