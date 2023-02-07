package com.zhongzhi.empty.http.realtime;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.zhongzhi.empty.http.BaseHttpConfig;

/**
 * 创蓝实时接口http配置
 * @author liuh
 * @date 2021年10月28日
 */
@Data
@Component
public class RealtimeHttpConfig extends BaseHttpConfig {

	/**
	 * 挡板，测试使用
	 */
	@Value("${http.realtime.baffle}")
	private boolean baffle = false;
	
	/**
	 * 是否缓存
	 */
	@Value("${http.realtime.cacheable}")
	private boolean cacheable = false;
	
	/**
	 * 
	 */
	@Value("${http.realtime.cacheName}")
	private String cacheName;
	
	/**
	 * 缓存大小
	 */
	@Value("${http.realtime.maxSize}")
	private int maxSize;
	
	/**
	 * 缓存失效
	 */
	@Value("${http.realtime.maxAge}")
	private int maxAge;

	/**
     * connect Timeout
     */
    @Value("${http.realtime.connectTimeout:0}")
    private int connectTimeout = 0;

    /**
     * read Timeout
     */
    @Value("${http.realtime.readTimeout:0}")
    private int readTimeout = 0;

    /**
     * write Timeout
     */
    @Value("${http.realtime.writeTimeout:0}")
    private int writeTimeout = 0;
}
