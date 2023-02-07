package com.zhongzhi.empty.http;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 公共http配置
 * @author liuh
 * @date 2021年10月28日
 */
@Data
@Component
public class BaseHttpConfig {
	
	/**
	 * follow Ssl Redirects
	 */
	@Value("${http.base.followSslRedirects}")
	private boolean followSslRedirects = true;

	/**
	 * follow Redirects
	 */
	@Value("${http.base.followRedirects}")
	private boolean followRedirects = true;

	/**
	 * retryOnConnectionFailure
	 */
	@Value("${http.base.retryOnConnectionFailure}")
	private boolean retryOnConnectionFailure = false;

	/**
	 * connect Timeout
	 */
	@Value("${http.base.connectTimeout}")
	private int connectTimeout = 5000;

	/**
	 * read Timeout
	 */
	@Value("${http.base.readTimeout}")
	private int readTimeout = 3000;

	/**
	 * write Timeout
	 */
	@Value("${http.base.writeTimeout}")
	private int writeTimeout = 3000;

	/**
	 * pingInterval
	 */
	@Value("${http.base.pingInterval}")
	private int pingInterval = 10 * 1000;

	/**
	 * pool max Idle Connections
	 */
	@Value("${http.base.maxIdleConnections}")
	private int maxIdleConnections = 50;

	/**
	 * pool keep Alive Duration
	 */
	@Value("${http.base.keepAliveDuration}")
	private int keepAliveDuration = 10 * 60 * 1000;
	
	/**
	 * 缓存的base目录
	 */
	@Value("${http.base.baseCachePath}")
	private String baseCachePath;
}
