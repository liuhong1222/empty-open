package com.zhongzhi.empty.service.gateway;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.http.unn.UnnData;
import com.zhongzhi.empty.http.unn.UnnHttpServcie;
import com.zhongzhi.empty.http.unn.UnnIntefaceEnum;
import com.zhongzhi.empty.http.unn.UnnResponse;
import com.zhongzhi.empty.response.UnnMobileStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * 创蓝空号接口实现类
 * @author liuh
 * @date 2021年10月28日
 */
@Slf4j
@Service
public class UnnService {
	
	@Value("${http.unn.url}")
    private String url;
	
	@Value("${http.unn.appId}")
    private String appId;
	
	@Value("${http.unn.userId}")
    private String userId;

	@Autowired
	private UnnHttpServcie unnHttpServcie;
	
	public List<UnnMobileStatus> emptyCheck(Long customerId,String[] mobiles){
		try {
			Map<String ,String> params = new HashMap<String ,String>();
			params.put("appID", appId);
	        params.put("userId", userId);
	        params.put("mobiles", String.join(",", mobiles));
	        params.put("url",url);
	        UnnResponse unnResponse = unnHttpServcie.doHttpSearch(UnnIntefaceEnum.UNN_CHECK.getSubUrl(), params, UnnResponse.class);
	        if(unnResponse == null || !CommonConstant.UNN_SUCCESS_CODE.equals(unnResponse.getResultCode())) {
	        	log.error("{}, 创蓝空号检测接口调用失败，号码个数：{}，response:{}",customerId,mobiles.length,unnResponse==null?null:JSON.toJSONString(unnResponse));
	        	return null;
	        }
	        
	        List<UnnMobileStatus> list = new ArrayList<UnnMobileStatus>();
	        for(UnnData unnData : unnResponse.getResultObj()) {
	        	UnnMobileStatus unnMobileStatus = new UnnMobileStatus();
	        	unnMobileStatus.setMobile(unnData.getMobile());
	        	unnMobileStatus.setChargesStatus(unnData.getChargesStatus());
	        	
	        	String status = unnData.getStatus();
	        	if (StringUtils.equals("0", status) || StringUtils.equals("2", status)) {
	                // 空号
	        		unnMobileStatus.setStatus("0");
	            } else if (StringUtils.equals("1", status)) {
	                // 实号
	            	unnMobileStatus.setStatus("1");
	            } else if (StringUtils.equals("4", status)) {
	                // 沉默号
	            	unnMobileStatus.setStatus("4");
	            } else if (StringUtils.equals("5", status) || StringUtils.equals("3", status)) {
	                // 风险号
	            	unnMobileStatus.setStatus("5");
	            }
	        	
	        	list.add(unnMobileStatus);
	        }
	        
	        log.info("{}, 创蓝空号检测接口调用成功，号码个数：{}",customerId,list.size());
			return list;
		} catch (Exception e) {
			log.error("{}, 创蓝空号检测接口调用异常，info:",customerId,e);
			return null;
		}
	}
	
	public List<UnnData> emptyCheckNew(String[] mobiles){
		try {
			Map<String ,String> params = new HashMap<String ,String>();
			params.put("appID", appId);
	        params.put("userId", userId);
	        params.put("mobiles", String.join(",", mobiles));
	        params.put("url",url);
	        UnnResponse unnResponse = unnHttpServcie.doHttpSearch(UnnIntefaceEnum.UNN_CHECK.getSubUrl(), params, UnnResponse.class);
	        if(unnResponse == null || !CommonConstant.UNN_SUCCESS_CODE.equals(unnResponse.getResultCode())) {
	        	return null;
	        }
	        	        
			return unnResponse.getResultObj();
		} catch (Exception e) {
			log.error("info:",e);
			return null;
		}
	}
}
