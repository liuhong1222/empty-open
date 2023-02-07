package com.zhongzhi.empty.interceptor;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import com.zhongzhi.empty.util.ThreadLocalContainer;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 清除 local线程变量
 * @author liuh
 * @date 2021年10月26日
 */
@Slf4j
public class ThreadBoundClearInterceptor implements HandlerInterceptor {	
	
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) throws Exception {
		ThreadLocalContainer.clearAll();
		log.debug("cleared ThreadLocal Container's objects.");
	}

}
