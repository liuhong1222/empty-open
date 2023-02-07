package com.zhongzhi.empty.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseController {	
	
	/**
	 * 获取请求的真实ＩＰ地址
	 * @param request
	 * @return
	 */
	public String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for"); 
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {  
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if( ip.indexOf(",")!=-1 ){
                ip = ip.split(",")[0];
            }
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("WL-Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_CLIENT_IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("X-Real-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getRemoteAddr();  
        } 
        return ip;  
    }
	
	public Map<String ,Object> getJsonParam(HttpServletRequest request) throws UnsupportedEncodingException, IOException {
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
	
	public static Map<String, String> getParameter(HttpServletRequest request) {
        Map<String, String> params = new TreeMap<>();
        Enumeration<String> enu = request.getParameterNames();
        while (enu.hasMoreElements()) {
            String key = enu.nextElement();
            String value = request.getParameter(key);
            params.put(key, value);
        }
        return params;
    }
}
