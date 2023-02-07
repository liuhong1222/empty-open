package com.zhongzhi.empty.constants;

/**
 * 缓存刷新标识
 * @author liuh
 * @date 2021年11月10日
 */
public class CacheConstant {
	
	/**
	 * redis缓存订阅通道名称
	 */
	public final static String CACHE_REFRESH_CHANNEL = "eo_cache_refresh";

	/**
	 * 代理商信息
	 */
	public final static String AGENT_CACHE = "agent";
	
	/**
	 * api账号信息
	 */
	public final static String API_SETTINGS_CACHE = "api_settings";
	
	/**
	 * 用户信息
	 */
	public final static String CUSTOMER_CACHE = "customer";
	
	/**
	 * 系统配置信息
	 */
	public final static String SYS_CONFIG_CACHE = "sys_config";
}
