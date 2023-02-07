package com.zhongzhi.empty.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.zhongzhi.empty.constants.CaffeineConstant;
import com.zhongzhi.empty.dao.AgentMapper;
import com.zhongzhi.empty.entity.Agent;

import lombok.extern.slf4j.Slf4j;

/**
 * 代理商信息实体类
 * @author liuh
 * @date 2021年10月26日
 */
@Slf4j
@Service
public class AgentService {
	
	@Autowired
    private AgentMapper agentMapper;

	@Cacheable(cacheManager = "caffeineCacheManager", value = CaffeineConstant.AGENT_INFO, key = "#id", unless = "#result == null")
	public Agent getAgentById(Long id){
        return agentMapper.getAgentById(id);
    }
	
	@CacheEvict(cacheManager = "caffeineCacheManager", cacheNames = {CaffeineConstant.AGENT_INFO}, key = "#id")
    public void refreshGetAgentById(Long id) {
    	log.info("{},代理商信息缓存刷新成功",id);
    }
}
