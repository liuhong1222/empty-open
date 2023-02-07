package com.zhongzhi.empty.http;

import com.alibaba.fastjson.JSON;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.Response.Builder;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * mock拦截器
 * @author liuh
 * @date 2021年10月28日
 */
public abstract class AbstractHttpInterceptor implements Interceptor {

	/**
	 * supplier map
	 */
	protected final Map<String, Supplier<Object>> supplierMap = new HashMap<>();

	/* (non-Javadoc)
	 * @see okhttp3.Interceptor#intercept(okhttp3.Interceptor.Chain)
	 */	
	@Override
	public Response intercept(Chain chain) throws IOException {
		Object ret = getMockDataByRequestUrl(chain.request().url().url());
		String bodyContent = null;
		if(ret instanceof String) {
			bodyContent = (String)ret;
		}else {
			bodyContent = JSON.toJSONString(ret);
		}
		Builder builder = new Builder().addHeader("Content-Type", "application/json;charset=UTF-8");
		builder.body(ResponseBody.create(null,bodyContent )).code(200).request(chain.request()).protocol(Protocol.HTTP_1_1).message("ok");
		return builder.build();
	}
	
	/**
	 * get mock data by request url
	 * @param url java.net.URL
	 * @return Object mocked object
	 */
	private Object getMockDataByRequestUrl(URL url) {
		Object ret = null;
		String requestUrl = url.getPath();	
		for(String key : supplierMap.keySet()) {
			if(requestUrl.equals(key)) {
				ret = supplierMap.get(key).get();
				break;
			}
		}
		return ret;
	}

}
