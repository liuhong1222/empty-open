package com.zhongzhi.empty.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhongzhi.empty.constants.CacheConstant;
import com.zhongzhi.empty.service.AgentService;
import com.zhongzhi.empty.service.SysConfigService;
import com.zhongzhi.empty.service.customer.ApiSettingsService;
import com.zhongzhi.empty.service.customer.CustomerService;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPubSub;

/**
 * 缓存刷新监听
 * @author liuh
 * @date 2021年10月27日
 */
@Slf4j
@Component
public class CacheRefreshListener extends JedisPubSub{
	
	@Autowired
	private AgentService agentService;
	
	@Autowired
	private ApiSettingsService apiSettingsService;
	
	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private SysConfigService sysConfigService;
	
	@Override
	public void onSubscribe(String channel, int subscribedChannels) {
		log.info("channel:" + channel + "is been subscribed:" +subscribedChannels);
	}
 
	@Override
	public void onUnsubscribe(String channel, int subscribedChannels) {
		log.info("channel:" + channel + "is been unsubscribed:" +subscribedChannels);
	}
	
	@Override
	public void onMessage(String channel,String message) {
		try {
			JSONObject jsonObject = JSON.parseObject(message);
			String option = jsonObject.getString("option");
			if(CacheConstant.AGENT_CACHE.equals(option)) {
				String id = jsonObject.getString("id");
				agentService.refreshGetAgentById(Long.valueOf(id));
				log.info("代理商信息缓存刷新成功，agentId:{}",id);
			}else if(CacheConstant.API_SETTINGS_CACHE.equals(option)) {
				String appId = jsonObject.getString("appId");
				apiSettingsService.refreshGetAppId(appId);
				log.info("api账号信息缓存刷新成功，appId:{}",appId);
			}else if (CacheConstant.CUSTOMER_CACHE.equals(option)) {
				String customerId = jsonObject.getString("customerId");
				customerService.refreshGetCustomerById(Long.valueOf(customerId));
				log.info("用户信息缓存刷新成功，customerId:{}",customerId);
			}else if(CacheConstant.SYS_CONFIG_CACHE.equals(option)) {
				String keys = jsonObject.getString("keys");
				sysConfigService.refreshFindOneByKey(keys);
				log.info("系统通道配置信息缓存刷新成功，keys:{}",keys);
			}else {
				log.error("不存在的缓存信息，message:{}",message);
			}
		} catch (Exception e) {
			log.error("synchronous cache error:{}", message, e);
		}
		log.info("synchronous cache:{}", message);
	}
}
