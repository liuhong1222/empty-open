package com.zhongzhi.empty.http.international;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.zhongzhi.empty.http.BaseHttpConfig;

/**
 *      国际接口http配置
 * @author liuh
 * @date 2022年6月9日
 */
@Data
@Component
public class InternationalHttpConfig extends BaseHttpConfig {

	/**
	 * 挡板，测试使用
	 */
	@Value("${http.international.baffle}")
	private boolean baffle = false;
	
	/**
	 * 是否缓存
	 */
	@Value("${http.international.cacheable}")
	private boolean cacheable = false;
	
	/**
	 * 
	 */
	@Value("${http.international.cacheName}")
	private String cacheName;
	
	/**
	 * 缓存大小
	 */
	@Value("${http.international.maxSize}")
	private int maxSize;
	
	/**
	 * 缓存失效
	 */
	@Value("${http.international.maxAge}")
	private int maxAge;

	/**
     * connect Timeout
     */
    @Value("${http.international.connectTimeout:0}")
    private int connectTimeout = 0;

    /**
     * read Timeout
     */
    @Value("${http.international.readTimeout:0}")
    private int readTimeout = 0;

    /**
     * write Timeout
     */
    @Value("${http.international.writeTimeout:0}")
    private int writeTimeout = 0;
}
