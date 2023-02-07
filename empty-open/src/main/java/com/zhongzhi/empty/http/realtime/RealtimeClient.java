package com.zhongzhi.empty.http.realtime;


import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.springframework.stereotype.Service;

import com.zhongzhi.empty.config.SpringUtil;
import com.zhongzhi.empty.http.Client;
import com.zhongzhi.empty.http.ClientService;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 创蓝实时接口httpclient
 * @author liuh
 * @date 2021年10月28日
 */
@ClientService(interfaces ="cl_realtime")
@Service
public class RealtimeClient implements Client {

	/***
	 * okhttp client
	 */
	protected OkHttpClient client;
	
	@Override
	public void initClient() {
		RealtimeHttpConfig httpConfig = SpringUtil.getBean(RealtimeHttpConfig.class);
		Builder builder = new Builder();
		builder.connectTimeout(httpConfig.getConnectTimeout(), TimeUnit.MILLISECONDS).writeTimeout(httpConfig.getWriteTimeout(), TimeUnit.MILLISECONDS)
				.readTimeout(httpConfig.getReadTimeout(), TimeUnit.MILLISECONDS).followRedirects(httpConfig.isFollowRedirects())
				.followSslRedirects(httpConfig.isFollowSslRedirects())
				.connectionPool(new ConnectionPool(httpConfig.getMaxIdleConnections(), httpConfig.getKeepAliveDuration(), TimeUnit.MILLISECONDS))
				.pingInterval(httpConfig.getPingInterval(), TimeUnit.MILLISECONDS).retryOnConnectionFailure(httpConfig.isRetryOnConnectionFailure());
		if (httpConfig.isCacheable()) {
			File cacheFile = new File(httpConfig.getBaseCachePath(),httpConfig.getCacheName());
			Cache cache = new Cache(cacheFile, httpConfig.getMaxSize());
			builder.cache(cache);
		}

		this.client = builder.build();
	}

	@Override
	public OkHttpClient getHttpClient() {
		return client;
	}
}
