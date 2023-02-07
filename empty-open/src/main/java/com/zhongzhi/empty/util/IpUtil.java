package com.zhongzhi.empty.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 获取IP地址工具类
 * @author liuh
 * @date 2021年10月29日
 */
public final class IpUtil {
    private IpUtil(){
        throw new AssertionError();
    }

    /**
     * 获取请求用户的IP地址
     * @return
     */
    public static String getRequestIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        return getRequestIp(request);
    }

    /**
     * 获取请求用户的IP地址
     * @param request
     * @return
     */
    public static String getRequestIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if ("0:0:0:0:0:0:0:1".equals(ip) || StringUtils.equals(ip, "127.0.0.1")) {
            ip = getLocalhostIp();
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ip != null && ip.length() > 15 && StringUtils.indexOf(ip, ',') > 0) {
            // "***.***.***.***".length() = 15
            ip = StringUtils.substring(ip, 0, StringUtils.indexOf(ip, ','));
        }
        return ip;
    }

    public static String getLocalhostIp(){
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
        }
        return null;
    }

}
