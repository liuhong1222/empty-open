package com.zhongzhi.empty.util;

import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

/**
 * http工具类
 * @author liuh
 * @date 2021年10月26日
 */
public class HttpUtils {

    private static final int           REQUEST_TIMEOUT  = 10 * 1000;                                 // 设置请求超时

    private static final int           CONNECT_TIMEOUT  = 5 * 1000;                                 // 连接超时时间

    private static final int           SO_TIMEOUT       = 20 * 1000;                                // 数据传输超时

    private static final Charset       DEFAULT_CHARTSET = ContentType.APPLICATION_JSON.getCharset(); //utf8

    // 务必单例
    private static CloseableHttpClient client;
    
    private static PoolingHttpClientConnectionManager connMgr;

    static {
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(REQUEST_TIMEOUT).setSocketTimeout(SO_TIMEOUT).build();

        SSLContext sslContext;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
			    public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
			        return true;
			    }
			}).build();
			
	        HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
	        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
	        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
	                .register("http", PlainConnectionSocketFactory.getSocketFactory())
	                .register("https", sslSocketFactory)
	                .build();
	        connMgr = new PoolingHttpClientConnectionManager( socketFactoryRegistry);
	        client = HttpClients.custom().setDefaultRequestConfig(requestConfig).setMaxConnTotal(600).setSSLContext(sslContext).setConnectionManager(connMgr)
	                .build();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }


    public static String get(String url, Map<String, String> paramsMap) {
        return send(RequestBuilder.get(url), paramsMap);
    }


    public static String post(String url, Map<String, String> paramsMap) {
        return send(RequestBuilder.post(url), paramsMap);
    }


    public static String postJson(String url, String json) {
        final RequestBuilder postBuilder = RequestBuilder.post(url);
        postBuilder.setCharset(DEFAULT_CHARTSET);
        final StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        postBuilder.setEntity(entity);
        return send0(postBuilder);
    }

    private static String send(RequestBuilder requestBuilder, Map<String, String> paramsMap) {
        requestBuilder.setCharset(DEFAULT_CHARTSET);

        if (paramsMap != null) {
            for (Map.Entry<String, String> param : paramsMap.entrySet()) {
                requestBuilder.addParameter(param.getKey(), param.getValue());
            }
        }
        return send0(requestBuilder);
    }


    private static String send0(final RequestBuilder requestBuilder) {
        String responseText = null;
        CloseableHttpResponse response = null;
        try {
            response = client.execute(requestBuilder.build());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                org.apache.http.HttpEntity entity = response.getEntity();
                if (entity != null) {
                    responseText = EntityUtils.toString(entity, DEFAULT_CHARTSET);
                }
            }
        } catch (Exception e) {
        } finally {
            try {
                response.close();
            } catch (Exception e) {
            }
        }
        return responseText;
    }
}
