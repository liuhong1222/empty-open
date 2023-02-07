package com.zhongzhi.empty.interceptor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * IP白名单拦截
 * @author liuh
 * @date 2021年10月26日
 */
@Slf4j
public class IPInterceptor implements HandlerInterceptor {
	
	private final static String APP_ID                   = "account";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        try {
//            //获取实际IP地址
//        	AdFluxApiConfig config = ThreadLocalContainer.getAdFluxApiConfig();
//            String ipAddress = NetUtil.getRealIP(request);
//            log.info("客户{}真实IP为：{}", config.getFluxAccount(), ipAddress);
//            if (StringUtils.isEmpty(ipAddress)) {
//                log.info("{}--获取真实IP失败,fluxAccount:{}", request.getRequestURI(), config.getFluxAccount());
//                throw new BusinessException(ErrorCodeEnum.SYSTEM_IP_ILLEGAL_CODE, "获取真实IP失败");
//            }
//
//            //本地IP直接放行
//            if(ipAddress != null && (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1"))){
//                return true;
//            }
//
//            //没设置ip地址则放行
//            if(StringUtils.isBlank(config.getIps())) {
//            	return true;
//            }
//
//            String threeNodeIp = ipAddress.substring(0, ipAddress.lastIndexOf("."));
//            int lastNodeIp = Integer.parseInt(ipAddress.substring(ipAddress.lastIndexOf(".") + 1, ipAddress.length()));
//            int oneIp, anotherIp;
//
//            String[] arrIps = config.getIps().split(",");
//
//            for (String ip : arrIps) {
//                try {
//                    if (ip.contains(ipAddress)) {
//                        return true;
//                    } else if (ip.contains(threeNodeIp)) {
//                        if (ip.contains("*")) {
//                            return true;
//                        } else if (ip.contains("~")) {
//                            oneIp = Integer.parseInt(ip.substring(ipAddress.lastIndexOf(".") + 1, ip.lastIndexOf("~")));
//                            anotherIp = Integer.parseInt(ip.substring(ip.lastIndexOf("~") + 1, ip.length()));
//                            if ((oneIp <= lastNodeIp && lastNodeIp <= anotherIp) || (anotherIp <= lastNodeIp && lastNodeIp <= oneIp)) {
//                                return true;
//                            }
//                        }else if(ip.contains("[")){
//                            String ips = ip.substring(ip.lastIndexOf("[")+1,ip.lastIndexOf("]"));
//                            String[] lastIps = ips.split("\\.");
//                            for (String lastIp : lastIps) {
//                                if(lastIp.equals(lastNodeIp+"")){
//                                    return true;
//                                }
//                            }
//                        }
//                    }
//                } catch (Exception ex) {
//                    log.error("IP检验出错", ex);
//                }
//            }
//            
//            log.error("{}--请求IP不合法,fluxAccount:{}", request.getRequestURI(), config.getFluxAccount());
//            // IP未能匹配
//            throw new BusinessException(ErrorCodeEnum.SYSTEM_IP_ILLEGAL_CODE);
        } catch (Exception ex) {
            log.error("IP白名单校验失败,account:{}", request.getParameter(APP_ID), ex);
            throw ex;
        }
        
        return true;
    }
}
