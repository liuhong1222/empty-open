package com.zhongzhi.empty.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.alibaba.fastjson.JSONObject;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.exception.BusinessException;
import com.zhongzhi.empty.param.OpenApiParam;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.service.ApiSignService;
import com.zhongzhi.empty.util.ThreadLocalContainer;
import com.zhongzhi.empty.vo.CustomerInfoVo;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * 鉴权拦截器
 * @author liuh
 * @date 2021年10月26日
 */
@Slf4j
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final static String APP_ID                   = "appId";

    private final static String APP_KEY                  = "appKey";

    private Map<String, InterceptorHandle> interceptorMap = new HashMap<String, InterceptorHandle>();
    
    @Autowired
    private ApiSignService apiSignService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        if (handlerMethod.getBeanType().isAnnotationPresent(Interceptor.class) || handlerMethod.getMethod()
                                                                                               .isAnnotationPresent(
                                                                                                       Interceptor.class)) {
            Interceptor annotation = handlerMethod.getBeanType().getAnnotation(Interceptor.class);
            annotation = annotation == null ? handlerMethod.getMethod().getAnnotation(Interceptor.class) : annotation;
            InterceptorHandle handle = interceptorMap.get(annotation.name());
            if (handle != null) {
                return handle.preHandle(request, response, handler);
            }
        }
        return defaultInterceptorHandle(request, response, handler);
    }


    private boolean defaultInterceptorHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws UnsupportedEncodingException, IOException {
    	String appId = request.getParameter(APP_ID);
        String appKey = request.getParameter(APP_KEY); 
        Map<String ,Object> paramMap = new HashMap<String, Object>();
        OpenApiParam openApiParam = new OpenApiParam();
        String mobiles = null;
        if(StringUtils.isEmpty(appId)){
        	paramMap = getJsonParam(request);
            appId = paramMap.get(APP_ID).toString();
            mobiles = paramMap.get("mobiles").toString();
        }
        if(StringUtils.isEmpty(appKey)){
            appKey = paramMap.get(APP_KEY).toString();
        }
        if (StringUtils.isEmpty(appId)) {
            log.info("{}--请求{}不能为空", request.getRequestURI(),APP_ID);
            throw new BusinessException(ApiCode.PARAMETER_EXCEPTION, "请求非法，"+APP_ID+"不能为空");
        }
        if (StringUtils.isEmpty(appKey)) {
        	log.info("{}--请求{}不能为空", request.getRequestURI(),APP_KEY);
            throw new BusinessException(ApiCode.PARAMETER_EXCEPTION, "请求非法，"+APP_KEY+"不能为空");
        }

        ApiResult<CustomerInfoVo> apiResult = apiSignService.getCustomerInfoVo(appId, appKey);
        if (apiResult == null || !apiResult.isSuccess()) {
        	log.info("{}--请求非法，验证不通过,account:{}", request.getRequestURI(), appId);
            throw new BusinessException(ApiCode.getApiCode(apiResult.getCode()), apiResult.getMsg());
        }
        
        openApiParam.setAppId(appId);
    	openApiParam.setAppKey(appKey);
    	openApiParam.setMobiles(mobiles);
    	CustomerInfoVo customerInfoVo= apiResult.getData();
    	customerInfoVo.setOpenApiParam(openApiParam);
        ThreadLocalContainer.setCustomerInfoVo(customerInfoVo);
        return true;
    }
    
    private Map<String ,Object> getJsonParam(HttpServletRequest request) throws UnsupportedEncodingException, IOException {
    	BufferedReader streamReader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
    	StringBuilder stringBuilder = new StringBuilder();
    	String inputStr = "";
    	while ((inputStr = streamReader.readLine()) != null) {
			stringBuilder.append(inputStr);
		}
    	
    	if(stringBuilder.length()<=0) {
    		return null;
    	}
    	
    	return JSONObject.parseObject(stringBuilder.toString(),Map.class);
    }
}
