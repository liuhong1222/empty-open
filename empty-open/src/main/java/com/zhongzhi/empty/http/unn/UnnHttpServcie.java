package com.zhongzhi.empty.http.unn;

import com.alibaba.fastjson.JSONObject;
import com.zhongzhi.empty.http.OkHttpService;

import lombok.extern.slf4j.Slf4j;
import okhttp3.CacheControl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 创蓝空号接口http
 * @author liuh
 * @date 2021年10月28日
 */
@Slf4j
@Service
public class UnnHttpServcie extends OkHttpService implements InitializingBean {
    
    /**
     * 万数地址key
     */
    private static String UNN_URL_KEY = "url";

    @Autowired
    private UnnHttpConfig config;

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
        	UnnIntefaceEnum intefaceEnum = UnnIntefaceEnum.getBySubUrl(subUrl);
            if (null == intefaceEnum) {
                throw new IllegalArgumentException(" chuanglan unn interface cannot be finded ");
            }
            switch (intefaceEnum) {
            	case UNN_CHECK:
                	preUrl = params.get(UNN_URL_KEY);
                    params.remove(UNN_URL_KEY);
                    break;
                
                default:
                    throw new IllegalArgumentException(" chuanglan unn interface cannot be identified ");
            }


            jsonStr = this.doPost(preUrl + subUrl, params, this.cacheControl,intefaceEnum.getUrlProperty());
            return JSONObject.parseObject(jsonStr, clazz);
        } catch (Exception e) {
            log.error(" do unn search failed !  request url is :{}, data is : {},return json is:{}", subUrl, params.toString(), jsonStr, e);
        }

        return null;
    }
}
