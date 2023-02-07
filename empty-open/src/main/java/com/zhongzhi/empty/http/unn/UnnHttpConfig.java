package com.zhongzhi.empty.http.unn;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.zhongzhi.empty.http.BaseHttpConfig;

/**
 * 创蓝万数接口http配置
 * @author liuh
 * @date 2021年10月28日
 */
@Data
@Component
public class UnnHttpConfig extends BaseHttpConfig {

	/**
	 * 挡板，测试使用
	 */
	@Value("${http.unn.baffle}")
	private boolean baffle = false;
	
	/**
	 * 是否缓存
	 */
	@Value("${http.unn.cacheable}")
	private boolean cacheable = false;
	
	/**
	 * 
	 */
	@Value("${http.unn.cacheName}")
	private String cacheName;
	
	/**
	 * 缓存大小
	 */
	@Value("${http.unn.maxSize}")
	private int maxSize;
	
	/**
	 * 缓存失效
	 */
	@Value("${http.unn.maxAge}")
	private int maxAge;

	/**
     * connect Timeout
     */
    @Value("${http.unn.connectTimeout:0}")
    private int connectTimeout = 0;

    /**
     * read Timeout
     */
    @Value("${http.unn.readTimeout:0}")
    private int readTimeout = 0;

    /**
     * write Timeout
     */
    @Value("${http.unn.writeTimeout:0}")
    private int writeTimeout = 0;
}
