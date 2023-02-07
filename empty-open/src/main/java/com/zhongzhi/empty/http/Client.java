package com.zhongzhi.empty.http;

import okhttp3.OkHttpClient;

/**
 * httpClient
 * @author liuh
 * @date 2021年10月28日
 */
public interface Client {

	public void initClient();
	
	public OkHttpClient getHttpClient();
}