package com.zhongzhi.empty.http.international;

import com.alibaba.fastjson.JSONObject;
import com.zhongzhi.empty.http.OkHttpService;

import lombok.extern.slf4j.Slf4j;
import okhttp3.CacheControl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 国际接口http
 * @author liuh
 * @date 2022年6月9日
 */
@Slf4j
@Service
public class InternationalHttpServcie extends OkHttpService implements InitializingBean {
    
    /**
     * 地址key
     */
    private static String INTERNATIONAL_URL_KEY = "url";

    @Autowired
    private InternationalHttpConfig config;

    /**
     * cache control
     */
    private CacheControl cacheControl;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.cacheControl = new CacheControl.Builder().maxAge(config.getMaxAge(), TimeUnit.MILLISECONDS).build();
    }


    /**
     * 执行http 请求
     *
     * @param subUrl 子url
     * @param params 请求参数
     * @param clazz  返回对象类型
     * @return T
     */
    public <T> T doHttpSearch(final String subUrl, Map<String, String> params, Class<T> clazz) {
        String jsonStr = null;
        String preUrl = null;
        try {
        	InternationalIntefaceEnum intefaceEnum = InternationalIntefaceEnum.getBySubUrl(subUrl);
            if (null == intefaceEnum) {
                throw new IllegalArgumentException(" international interface cannot be finded ");
            }
            switch (intefaceEnum) {
            	case UPLOAD:
                	preUrl = params.get(INTERNATIONAL_URL_KEY);
                    params.remove(INTERNATIONAL_URL_KEY);
                    break;
                    
            	case QUERY:
                	preUrl = params.get(INTERNATIONAL_URL_KEY);
                    params.remove(INTERNATIONAL_URL_KEY);
                    break;
                    
            	case DOWNLOAD:
                	preUrl = params.get(INTERNATIONAL_URL_KEY);
                    params.remove(INTERNATIONAL_URL_KEY);
                    break;
                
                default:
                    throw new IllegalArgumentException(" international interface cannot be identified ");
            }


            jsonStr = this.doPost(preUrl + subUrl, params, this.cacheControl,intefaceEnum.getUrlProperty());
            return JSONObject.parseObject(jsonStr, clazz);
        } catch (Exception e) {
            log.error(" do international search failed !  request url is :{}, data is : {},return json is:{}", subUrl, params.toString(), jsonStr, e);
        }

        return null;
    }
    
    public <T> T doHttpUpload(final String subUrl,String url,String fileName,String filePath, Class<T> clazz) {
    	String jsonStr = "";
    	try {
    		InternationalIntefaceEnum intefaceEnum = InternationalIntefaceEnum.getBySubUrl(subUrl);
    		jsonStr = this.doPostFile(url, fileName, new File(filePath), cacheControl, intefaceEnum.getUrlProperty());
            return JSONObject.parseObject(jsonStr, clazz);
        } catch (Exception e) {
            log.error(" do international upload failed !  request url is :{}, filePath is : {},return json is:{}, info:", url, filePath, jsonStr, e);
        }

        return null;
    }
    
    public Boolean doHttpDownload(String url,String filePath) {
    	Boolean jsonStr = null;
    	try {
            return this.downloadFile(url, filePath);
        } catch (Exception e) {
            log.error(" do international download failed !  request url is :{}, filePath is : {},return json is:{}, info:", url, filePath, jsonStr, e);
        }

        return null;
    }
}
