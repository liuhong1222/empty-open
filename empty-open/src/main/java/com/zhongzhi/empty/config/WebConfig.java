package com.zhongzhi.empty.config;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongzhi.empty.interceptor.AuthenticationInterceptor;
import com.zhongzhi.empty.interceptor.ThreadBoundClearInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();

        //json中多余的参数不报错，不想要可以改掉
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //设置全局的时间转化
        SimpleDateFormat smt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        objectMapper.setDateFormat(smt);
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));//解决时区差8小时问题

        //设置中文编码格式
        List<MediaType> list = new ArrayList<MediaType>();
        list.add(MediaType.APPLICATION_JSON_UTF8);
        jackson2HttpMessageConverter.setSupportedMediaTypes(list);

        jackson2HttpMessageConverter.setObjectMapper(objectMapper);
        converters.add(0, jackson2HttpMessageConverter);
    }

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(getAuthenticationInterceptor()).addPathPatterns("/open/**").excludePathPatterns("/open/test/**");
//		registry.addInterceptor(getIPInterceptor()).addPathPatterns("/open/**").excludePathPatterns("/open/callback/**");
		registry.addInterceptor(new ThreadBoundClearInterceptor()).addPathPatterns("/**");
	}
	
	@Bean 
	public AuthenticationInterceptor getAuthenticationInterceptor() {
        return new AuthenticationInterceptor();
    }
	
//	@Bean 
//	public IPInterceptor getIPInterceptor() {
//        return new IPInterceptor();
//    }
}
