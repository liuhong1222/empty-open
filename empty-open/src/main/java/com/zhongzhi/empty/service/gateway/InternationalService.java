package com.zhongzhi.empty.service.gateway;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.enums.DirectTypeEnum;
import com.zhongzhi.empty.enums.InternationalMobileReportGroupEnum;
import com.zhongzhi.empty.http.international.InternationalHttpServcie;
import com.zhongzhi.empty.http.international.InternationalIntefaceEnum;
import com.zhongzhi.empty.http.international.InternationalQueryResponse;
import com.zhongzhi.empty.http.international.InternationalUploadResponse;
import com.zhongzhi.empty.http.international.QueryResponse;
import com.zhongzhi.empty.http.international.UploadResponse;
import com.zhongzhi.empty.response.HeLiuQiangResponse;
import com.zhongzhi.empty.response.StatusResult;
import com.zhongzhi.empty.util.DateUtils;
import com.zhongzhi.empty.util.DingDingMessage;
import com.zhongzhi.empty.util.MD5Util;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 国际接口实现类
 * @author liuh
 * @date 2022年6月9日
 */
@Slf4j
@Service
public class InternationalService{
	
	@Value("${http.international.url}")
    private String url;
	
	@Value("${http.international.newUrl}")
    private String newUrl;
	
	@Value("${http.international.appId}")
    private String appId;
	
	@Value("${http.international.appKey}")
    private String appKey;
	
	@Value("${http.international.Appkey}")
    private String Appkey;
	
	@Value("${http.international.Appsecret}")
    private String Appsecret;

	@Autowired
	private InternationalHttpServcie internationalHttpServcie;
	
	@Autowired
	private DingDingMessage dingDingMessage;
	
	private static OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build();
		
	public UploadResponse upload(Long customerId,String fileName,String filePath){
		long st = System.currentTimeMillis();
		try {
			String urlString = url + InternationalIntefaceEnum.COMMON_UPLOAD.getSubUrl() + String.format("?account=%s&pass=%s", appId,appKey);
			InternationalUploadResponse response = internationalHttpServcie.doHttpUpload(InternationalIntefaceEnum.COMMON_UPLOAD.getSubUrl(), urlString, fileName, filePath, InternationalUploadResponse.class);
	        if(response == null) {
	        	log.error("{}, 国际检测上传接口调用失败，下游接口返回结果为空，filePath:{}",customerId,filePath);
	        	return null;
	        }
	        
	        if(!CommonConstant.INTERNATIONAL_SUCCESS_CODE.equals(response.getRES())) {
	        	log.error("{}, 国际检测上传接口调用失败，filePath:{},response:{}",customerId,filePath,JSON.toJSONString(response));
	        	return null;
	        }
	        	        
	        log.info("{}, 国际检测上传接口调用成功，filePath:{},response:{},useTime:{}",customerId,filePath,
	        		JSON.toJSONString(response),(System.currentTimeMillis()-st));
			return response.getDATA();
		} catch (Exception e) {
			log.error("{}, 国际号码检测上传接口调用异常，filePath:{},info:",customerId,filePath,e);
			return null;
		}
	}
	
	public UploadResponse directUpload(Long customerId,String fileName,String filePath,String productType){
		long st = System.currentTimeMillis();
		try {
			String urlString = url + InternationalIntefaceEnum.UPLOAD.getSubUrl() + String.format("?account=%s&pass=%s&type=%s", appId,appKey,DirectTypeEnum.getCodeByName(productType));
			InternationalUploadResponse response = internationalHttpServcie.doHttpUpload(InternationalIntefaceEnum.UPLOAD.getSubUrl(), urlString, fileName, filePath, InternationalUploadResponse.class);
	        if(response == null) {
	        	log.error("{}, 定向国际检测上传接口调用失败，下游接口返回结果为空，filePath:{}",customerId,filePath);
	        	return null;
	        }
	        
	        if(!CommonConstant.INTERNATIONAL_SUCCESS_CODE.equals(response.getRES())) {
	        	log.error("{}, 定向国际检测上传接口调用失败，filePath:{},response:{}",customerId,filePath,JSON.toJSONString(response));
	        	return null;
	        }
	        	        
	        log.info("{}, 定向国际检测上传接口调用成功，filePath:{},response:{},useTime:{}",customerId,filePath,
	        		JSON.toJSONString(response),(System.currentTimeMillis()-st));
			return response.getDATA();
		} catch (Exception e) {
			log.error("{}, 定向国际号码检测上传接口调用异常，filePath:{},info:",customerId,filePath,e);
			return null;
		}
	}
	
	public QueryResponse query(Long customerId,String sendID){
		long st = System.currentTimeMillis();
		try {
			Map<String ,String> params = new HashMap<String ,String>();
			params.put("account", appId);
	        params.put("pass", appKey);
	        params.put("sendID", sendID);
	        params.put("url", url);
	        InternationalQueryResponse response = internationalHttpServcie.doHttpSearch(InternationalIntefaceEnum.QUERY.getSubUrl(), params, InternationalQueryResponse.class);
	        if(response == null) {
	        	log.error("{}, 国际号码检测查询接口调用失败，下游接口返回结果为空,sendID:{}，",customerId,sendID);
	        	return null;
	        }
	        
	        if(!CommonConstant.INTERNATIONAL_SUCCESS_CODE.equals(response.getRES())) {
	        	log.error("{}, 国际号码检测查询接口调用失败，sendID:{}，response:{}",customerId,sendID,JSON.toJSONString(response));
	        	return null;
	        }
	        	        
	        log.info("{}, 国际号码检测查询接口调用成功,sendID:{},response:{},useTime:{}",customerId,sendID,JSON.toJSONString(response),(System.currentTimeMillis()-st));
			return response.getDATA();
		} catch (Exception e) {
			log.error("{}, 国际号码检测查询接口调用异常，sendID:{},info:",customerId,sendID,e);
			return null;
		}
	}
	
	public Boolean download(Long customerId,String sendID,String type,String targetFilePath){
		long st = System.currentTimeMillis();
		try {
			String urlString = url + InternationalIntefaceEnum.DOWNLOAD.getSubUrl() + String.format("?account=%s&pass=%s&sendID=%s&type=%s", appId,appKey,sendID,type);
	        Boolean response = internationalHttpServcie.doHttpDownload(urlString, targetFilePath);
	        if(response == null) {
	        	log.error("{}, 国际号码检测下载接口调用失败，下游接口返回结果为空,sendID:{}，type:{},targetFilePath:{}",customerId,sendID,type,targetFilePath);
	        	return response;
	        }
      
	        log.info("{}, 国际号码检测下载接口调用成功,sendID:{},type:{},targetFilePath:{},response:{},useTime:{}",customerId,sendID,type,targetFilePath,JSON.toJSONString(response),(System.currentTimeMillis()-st));
			return response;
		} catch (Exception e) {
			log.error("{}, 国际号码检测下载接口调用异常，sendID:{},type:{},targetFilePath:{},info:",customerId,sendID,type,targetFilePath,e);
			return false;
		}
	}
	
	public ListMultimap<InternationalMobileReportGroupEnum,String> internationalFileDetection(List<String> mobileList, Long customerId,String countryCode) throws Exception {
        if (CollectionUtils.isEmpty(mobileList)) {
            return null;
        }
        
        ListMultimap<InternationalMobileReportGroupEnum, String> data = ArrayListMultimap.create();
        Set<String> mobileSet = new HashSet<>(mobileList);
        //请求api批量检查号码
        Map<String, String> mobilesParam = getMobileParam(mobileSet, countryCode);
        HeLiuQiangResponse response = invoke(customerId, String.join(",", mobilesParam.keySet()), countryCode);
        Set<String> hasResultSet = new HashSet<>();
        if (response != null && CommonConstant.HLQ_SUCCESS_STATUS.equals(response.getStatus())) {
        	StatusResult statusResult = response.getData();
            if (null != statusResult) {
            	// 激活号码
            	if(statusResult.getJhs() != null && statusResult.getJhs() > 0 && !CollectionUtils.isEmpty(statusResult.getJhhms())) {
            		for(String mobile : statusResult.getJhhms()) {
            			data.put(InternationalMobileReportGroupEnum.FileDetection.ACTIVATE, mobilesParam.get(mobile));
            			hasResultSet.add(mobile);
            		}
            	}
            	
            	if(statusResult.getWjhs() != null && statusResult.getWjhs() > 0 && !CollectionUtils.isEmpty(statusResult.getWjhhms())) {
            		for(String mobile : statusResult.getWjhhms()) {
            			data.put(InternationalMobileReportGroupEnum.FileDetection.NO_ACTIVE, mobilesParam.get(mobile));
            			hasResultSet.add(mobile);
            		}
            	}
            	
            	if(statusResult.getWzs() != null && statusResult.getWzs() > 0 && !CollectionUtils.isEmpty(statusResult.getWzhms())) {
            		for(String mobile : statusResult.getWzhms()) {
            			data.put(InternationalMobileReportGroupEnum.FileDetection.UNKNOWN, mobilesParam.get(mobile));
            			hasResultSet.add(mobile);
            		}
            	}
            }            
        }else {
        	log.error("{}, 何柳强国际号码状态检测接口调用失败，countryCode:{}, mobilesParam:{},response:{}",customerId,countryCode,mobilesParam,JSON.toJSONString(response));
        }

        List<String> tempList = new ArrayList<String>(mobilesParam.keySet());
        if (!CollectionUtils.isEmpty(hasResultSet)) {
        	tempList.removeAll(hasResultSet);
        }

        if (!CollectionUtils.isEmpty(tempList)) {
            for (String mm : tempList) {
                data.put(InternationalMobileReportGroupEnum.FileDetection.NO_RESULT, mobilesParam.get(mm));
            }
        }

        return data;
    }
	
	public HeLiuQiangResponse invoke(Long customerId,String mobiles,String countryCode) throws Exception {
		HeLiuQiangResponse result = null;

		Map<String, String> paramMap = convertToParamMap(mobiles, countryCode);
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(paramMap));
        Request.Builder rBuilder = new Request.Builder();
        Request requestPost = rBuilder.url(newUrl).post(body).build();
        long startTime = 0L;
        long startTime2 = System.currentTimeMillis();
        try (Response res = client.newCall(requestPost).execute()) {
            if (res.isSuccessful()) {
                startTime = System.currentTimeMillis();
                String jsonStr = res.body().string();
                long endTime = System.currentTimeMillis();
                result = JSONObject.parseObject(jsonStr, HeLiuQiangResponse.class);
                //调用接口耗时超过4s报警
                if (startTime - startTime2 >= 4000L) {
                	log.error("{}, 国际号码状态检测调用何柳强接口耗时超过4s,info:{},useTime:{}",customerId,JSON.toJSONString(paramMap),startTime - startTime2);
                	dingDingMessage.sendMessage("警告：" + customerId + ", 时间：" + DateUtils.getNowTime() + ", 调用何柳强国际号码状态检测接口耗时超过4s ");
                }else {
                	log.info("{},国际号码状态检测调用何柳强接口耗时： {}",customerId,(startTime - startTime2));
                }
                
                if (endTime - startTime > 2000L) {
                	dingDingMessage.sendMessage("警告：" + customerId + ", 时间：" + DateUtils.getNowTime() + ", 解析何柳强国际号码状态检测接口返回结果耗时超过2s ");
                	log.error("{}, 解析何柳强国际号码状态检测接口返回结果耗时超过2s,info:{},useTime:{}" ,customerId,JSON.toJSONString(paramMap), String.valueOf(endTime - startTime));
                }
            } else {
            	log.error("{}, 国际号码状态检测调用何柳强接口失败,param:{},response:{}",customerId,JSON.toJSONString(paramMap),res.body().string());
                throw new RuntimeException("调用检测服务失败");
            }
        } catch (Exception e) {
        	log.error("{}, 国际号码状态检测调用何柳强接口异常,info:{},e:",customerId,JSON.toJSONString(paramMap),e);
        }
        
        // release memory
        paramMap = null;
        body = null;
        rBuilder = null;
        requestPost = null;
        
        return result;
    }
	
	private Map<String, String> convertToParamMap(String mobiles,String countryCode){
		String timestamp = DateUtils.getDateTimeS();
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("apikey", Appkey);
		paramMap.put("timestamp", timestamp);
		paramMap.put("sign", MD5Util.MD5(Appkey+timestamp+Appsecret).toLowerCase());
		paramMap.put("mobile", mobiles);
		paramMap.put("cycode", countryCode);
		return paramMap;
	}
	
	public Map<String, String> getMobileParam(Set<String> mobileSet,String country) {
		Map<String, String> resultMap = new HashMap<String, String>();
		for(String mobile : mobileSet) {
			if(StringUtils.isBlank(mobile)) {
				continue;
			}
			
			if(CommonConstant.YINDU_COUNTRY_CODE.equals(country)) {
				if(mobile.length() == 12) {
					resultMap.put(mobile, mobile);
				}else {
					resultMap.put(country+mobile, mobile);
				}
			}else {
				if(mobile.startsWith(country)){
					resultMap.put(mobile, mobile);
				}else {
					resultMap.put(country+mobile, mobile);
				}
			}
		}
		
		return resultMap;
	}
}
