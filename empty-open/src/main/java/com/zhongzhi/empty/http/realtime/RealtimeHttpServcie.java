package com.zhongzhi.empty.http.realtime;

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
 * 创蓝实时接口http
 * @author liuh
 * @date 2021年11月2日
 */
@Slf4j
@Service
public class RealtimeHttpServcie extends OkHttpService implements InitializingBean {
    
    /**
     * 万数地址key
     */
    private static String REALTIME_URL_KEY = "url";

    @Autowired
    private RealtimeHttpConfig config;

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
        	RealtimeIntefaceEnum intefaceEnum = RealtimeIntefaceEnum.getBySubUrl(subUrl);
            if (null == intefaceEnum) {
                throw new IllegalArgumentException(" chuanglan realtime interface cannot be finded ");
            }
            switch (intefaceEnum) {
            	case MOB_STATUS_STATIC:
                	preUrl = params.get(REALTIME_URL_KEY);
                    params.remove(REALTIME_URL_KEY);
                    break;
                    
            	case MOB_STATUS:
                	preUrl = params.get(REALTIME_URL_KEY);
                    params.remove(REALTIME_URL_KEY);
                    break;
                
                default:
                    throw new IllegalArgumentException(" chuanglan realtime interface cannot be identified ");
            }


            jsonStr = this.doPost(preUrl + subUrl, params, this.cacheControl,intefaceEnum.getUrlProperty());
            return JSONObject.parseObject(jsonStr, clazz);
        } catch (Exception e) {
            log.error(" do realtime search failed !  request url is :{}, data is : {},return json is:{}", subUrl, params.toString(), jsonStr, e);
        }

        return null;
    }
}
