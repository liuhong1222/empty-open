package com.zhongzhi.empty.service.gateway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.entity.MobileStatusCache;
import com.zhongzhi.empty.http.realtime.MobileRealtimeStatus;
import com.zhongzhi.empty.http.realtime.RealtimeHttpServcie;
import com.zhongzhi.empty.http.realtime.RealtimeIntefaceEnum;
import com.zhongzhi.empty.http.realtime.RealtimeResponse;
import com.zhongzhi.empty.task.MobilePoolLocalCache;

import lombok.extern.slf4j.Slf4j;

/**
 * 创蓝实时接口实现类
 * @author liuh
 * @date 2021年11月2日
 */
@Slf4j
@Service
public class RealtimeService {
	
	@Value("${http.realtime.url}")
    private String url;
	
	@Value("${http.realtime.appId}")
    private String appId;
	
	@Value("${http.realtime.appKey}")
    private String appKey;

	@Autowired
	private RealtimeHttpServcie realtimeHttpServcie;
	
	private static MobilePoolLocalCache mobilePoolLocalCache = MobilePoolLocalCache.getInStance();
	
	public MobileRealtimeStatus mobileStatusStaticQuery(Long customerId,String mobile){
		long st = System.currentTimeMillis();
		try {
			Map<String ,String> params = new HashMap<String ,String>();
			params.put("appId", appId);
	        params.put("appKey", appKey);
	        params.put("mobile", mobile);
	        params.put("orderNo", String.valueOf(st));
	        params.put("url",url);
	        RealtimeResponse realtimeResponse = realtimeHttpServcie.doHttpSearch(RealtimeIntefaceEnum.MOB_STATUS_STATIC.getSubUrl(), params, RealtimeResponse.class);
	        if(realtimeResponse == null) {
	        	log.error("{}, 创蓝号码实时查询基础版接口调用失败，下游接口返回结果为空，orderNo:{},mobile:{}",customerId,st,mobile);
	        	return null;
	        }
	        
	        if(!CommonConstant.REALTIME_SUCCESS_CODE.equals(realtimeResponse.getCode())) {
	        	log.error("{}, 创蓝号码实时查询基础版接口调用失败，orderNo:{},mobile:{}，response:{}",customerId,st,mobile,JSON.toJSONString(realtimeResponse));
	        	return realtimeResponse.getData();
	        }
	        	        
	        log.info("{}, 创蓝号码实时查询基础版接口调用成功，orderNo:{},response:{},useTime:{}",customerId,st,
	        		JSON.toJSONString(realtimeResponse),(System.currentTimeMillis()-st));
	        handleCacheData(realtimeResponse.getData(), true);
			return realtimeResponse.getData();
		} catch (Exception e) {
			log.error("{}, 创蓝号码实时查询基础版接口调用异常，orderNo:{},mobile:{}，info:",customerId,st,mobile,e);
			return null;
		}
	}
	
	public MobileRealtimeStatus mobileStatusQuery(Long customerId,String mobile){
		long st = System.currentTimeMillis();
		try {
			Map<String ,String> params = new HashMap<String ,String>();
			params.put("appId", appId);
	        params.put("appKey", appKey);
	        params.put("mobile", mobile);
	        params.put("url",url);
	        RealtimeResponse realtimeResponse = realtimeHttpServcie.doHttpSearch(RealtimeIntefaceEnum.MOB_STATUS.getSubUrl(), params, RealtimeResponse.class);
	        if(realtimeResponse == null) {
	        	log.error("{}, 创蓝号码实时查询接口调用失败，下游接口返回结果为空,mobile:{}，",customerId,mobile);
	        	return null;
	        }
	        
	        if(!CommonConstant.REALTIME_SUCCESS_CODE.equals(realtimeResponse.getCode())) {
	        	log.error("{}, 创蓝号码实时查询接口调用失败，mobile:{}，response:{}",customerId,mobile,JSON.toJSONString(realtimeResponse));
	        	return realtimeResponse.getData();
	        }
	        	        
	        log.info("{}, 创蓝号码实时查询接口调用成功,response:{},useTime:{}",customerId,JSON.toJSONString(realtimeResponse),(System.currentTimeMillis()-st));
	        handleCacheData(realtimeResponse.getData(), false);
			return realtimeResponse.getData();
		} catch (Exception e) {
			log.error("{}, 创蓝号码实时查询接口调用异常，mobile:{},info:",customerId,mobile,e);
			return null;
		}
	}
	
	private  void handleCacheData(MobileRealtimeStatus mobileRealtimeStatus,boolean isStatic) {
		String mobileStatus = getEmptyCheckStatusByRealtime(mobileRealtimeStatus, isStatic);
		if(StringUtils.isBlank(mobileStatus)) {
			return ;
		}
		
		List<MobileStatusCache> list = new ArrayList<MobileStatusCache>();
		MobileStatusCache mobileStatusCache = new MobileStatusCache();
		mobileStatusCache.setMobile(mobileRealtimeStatus.getMobile());
		mobileStatusCache.setStatus(mobileStatus);
		list.add(mobileStatusCache);
		mobilePoolLocalCache.setLocalCache(list);
	}
	
	private String getEmptyCheckStatusByRealtime(MobileRealtimeStatus mobileRealtimeStatus,boolean isStatic) {
		if(isStatic) {
			String[] realStatus = {"1","3"};
			String[] emptyStatus = {"2","4","12"};
			return new ArrayList<String>(Arrays.asList(realStatus)).contains(mobileRealtimeStatus.getStatus())?"1":(
					new ArrayList<String>(Arrays.asList(emptyStatus)).contains(mobileRealtimeStatus.getStatus())?"0":null);
		}else {
			String[] realStatus = {"11","23","31"};
			String[] emptyStatus = {"13","14","15","18","21","25","32","33"};
			return new ArrayList<String>(Arrays.asList(realStatus)).contains(mobileRealtimeStatus.getNumberType()+mobileRealtimeStatus.getStatus())?"1":(
					new ArrayList<String>(Arrays.asList(emptyStatus)).contains(mobileRealtimeStatus.getNumberType()+mobileRealtimeStatus.getStatus())?"0":null);
		}
	}
}
