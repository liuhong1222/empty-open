package com.zhongzhi.empty.constants;

/**
 * 本地缓存常量类
 * @author liuh
 * @date 2021年10月26日
 */
public class CaffeineConstant {

	/**
	 * 缓存前缀
	 */
	public final static String CACHE_PREFIX = "ec_";
	
	/**
	 * 缓存版本号
	 */
	public final static String CACHE_VERSION = "v1_";
	
	/**
	 * api账号信息缓存
	 */
	public final static String API_SETTINGS_INFO = CACHE_PREFIX + CACHE_VERSION + "api_settings";
	
	/**
	 * 用户信息缓存
	 */
	public final static String CUSTOMER_INFO = CACHE_PREFIX + CACHE_VERSION + "customer";
	
	/**
	 * 代理商信息缓存
	 */
	public final static String AGENT_INFO = CACHE_PREFIX + CACHE_VERSION + "agent";
	
	/**
	 * 空号检测通道信息缓存
	 */
	public final static String EMPTY_GATEWAY_INFO = CACHE_PREFIX + CACHE_VERSION + "empty_gateway";
}
