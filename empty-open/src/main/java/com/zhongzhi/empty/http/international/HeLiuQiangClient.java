package com.zhongzhi.empty.http.international;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

import com.zhongzhi.empty.config.SpringUtil;

public abstract class HeLiuQiangClient {

    /***
     * ok http client
     */
    protected OkHttpClient client;

    /**
     * init OKHttpClient object
     */
    public void initClient() {
    	InternationalHttpConfig httpConfig = SpringUtil.getBean(InternationalHttpConfig.class);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(httpConfig.getConnectTimeout(), TimeUnit.MILLISECONDS).writeTimeout(httpConfig.getWriteTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(httpConfig.getReadTimeout(), TimeUnit.MILLISECONDS).followRedirects(httpConfig.isFollowRedirects())
                .followSslRedirects(httpConfig.isFollowSslRedirects())
                .connectionPool(new ConnectionPool(httpConfig.getMaxIdleConnections(), httpConfig.getKeepAliveDuration(), TimeUnit.MILLISECONDS))
                .pingInterval(httpConfig.getPingInterval(), TimeUnit.MILLISECONDS).retryOnConnectionFailure(httpConfig.isRetryOnConnectionFailure());
        this.client = builder.build();
        this.client.dispatcher().setMaxRequests(1000);
        this.client.dispatcher().setMaxRequestsPerHost(200);
    }


    @Override
    public void finalize() {
        this.client.connectionPool().evictAll();
    }
}
