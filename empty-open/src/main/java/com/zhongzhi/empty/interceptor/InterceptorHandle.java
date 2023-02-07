package com.zhongzhi.empty.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 自定义拦截器
 * @author liuh
 * @date 2021年10月26日
 */
public interface InterceptorHandle {
    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception;
}
