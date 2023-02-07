package com.zhongzhi.empty.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhongzhi.empty.config.Limiter;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.param.OpenApiParam;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.response.BatchCheckResult;
import com.zhongzhi.empty.response.FileCheckResult;
import com.zhongzhi.empty.response.UnnMobileNewStatus;
import com.zhongzhi.empty.service.empty.UnnApiService;
import com.zhongzhi.empty.util.CommonUtils;
import com.zhongzhi.empty.util.ThreadLocalContainer;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 空号检测对外api接口
 * @author liuh
 * @date 2021年10月27日
 */
@Slf4j
@RestController
@RequestMapping("/open/api")
@Api("对外接口 API")
public class UnnApiController extends BaseController{
	
	@Autowired
	private UnnApiService unnApiService;

	@PostMapping("/batchCheck")
    public ApiResult<BatchCheckResult> batchCheck(HttpServletRequest request) throws UnsupportedEncodingException, IOException {
		OpenApiParam openApiParam = ThreadLocalContainer.getCustomerInfoVo().getOpenApiParam();
		if (StringUtils.isBlank(openApiParam.getMobiles())) {
            log.warn("手机号码为空，appId: {}", openApiParam.getAppId());
            return ApiResult.result(ApiCode.MOBILE_COUNT_EXCEPTION, "手机号码不能为空", null);
        }
		
		Set<String> list = new HashSet<String>();
        for(String mobile : openApiParam.getMobiles().split(",")) {
        	if(CommonUtils.isMobile(mobile)) {
        		list.add(mobile);
        	}
        }
        
        String[] mobiles = list.toArray(new String[list.size()]);
        if (mobiles.length == 0) {
            log.warn("手机号码格式错误，appId: {}", openApiParam.getAppId());
            return ApiResult.result(ApiCode.MOBILE_COUNT_EXCEPTION, "无有效手机号码", null);
        }
        
        if (mobiles.length > 1000) {
            log.warn("手机号码数量超过1000个，appId: {}", openApiParam.getAppId());
            return ApiResult.result(ApiCode.MOBILE_COUNT_EXCEPTION, "手机号码个数不能超过1000个", null);
        }
        
        return unnApiService.batchCheck(mobiles,super.getIpAddr(request));
    }
    
    @PostMapping("/batchCheckNew")
    public ApiResult<List<UnnMobileNewStatus>> batchCheckNew(HttpServletRequest request,@Valid OpenApiParam openApiParam) {
        Set<String> list = new HashSet<String>();
        for(String mobile : openApiParam.getMobiles().split(",")) {
        	if(CommonUtils.isMobile(mobile)) {
        		list.add(mobile);
        	}
        }
        
        String[] mobiles = list.toArray(new String[list.size()]);
        if (mobiles.length == 0) {
            log.warn("手机号码格式错误，appId: {}", openApiParam.getAppId());
            return ApiResult.result(ApiCode.MOBILE_COUNT_EXCEPTION, "无有效手机号码", null);
        }
        
        if (mobiles.length > 1000) {
            log.warn("手机号码数量超过1000个，appId: {}", openApiParam.getAppId());
            return ApiResult.result(ApiCode.MOBILE_COUNT_EXCEPTION, "手机号码个数不能超过1000个", null);
        }
        
        return unnApiService.batchCheckNew(mobiles,super.getIpAddr(request));
    }
    
    @Limiter(limitNum = 30, limitType = Limiter.LimitType.CUSTOMER)
    @PostMapping("/uploadCheck")
    public ApiResult<FileCheckResult> uploadCheck(HttpServletRequest request,@RequestParam("file") MultipartFile multipartFile,
										    		@RequestParam("appId") String appId,
										            @RequestParam("appKey") String appKey) {
    	if(multipartFile == null || multipartFile.isEmpty()) {
    		log.error("文件不存在, appId: {}", appId);
            return ApiResult.result(ApiCode.FAIL, "文件不存在", null);
    	}
    	
    	log.info("{}，ContentType = {}, OriginalFilename = {}, Name = {}, Size = {}",appId, multipartFile.getContentType(),
                multipartFile.getOriginalFilename(), multipartFile.getName(), multipartFile.getSize());
    	return unnApiService.uploadCheck(multipartFile,super.getIpAddr(request));
    }
    
    @Limiter(limitNum = 10, limitType = Limiter.LimitType.CUSTOMER)
    @PostMapping("/uploadQuery")
    public ApiResult<FileCheckResult> uploadQuery(HttpServletRequest request,@RequestParam("sendId") Long sendId,
										    		@RequestParam("appId") String appId,
										            @RequestParam("appKey") String appKey) {
    	if(sendId == null) {
    		return ApiResult.result(ApiCode.FAIL, "sendId不能为空", null);
    	}
    	
    	return unnApiService.uploadQuery(sendId,super.getIpAddr(request));
    }
    
    private OpenApiParam getOpenApiParam(HttpServletRequest request) {
    	try {
			Map<String, String> param = super.getParameter(request);
			return JSONObject.parseObject(JSON.toJSONString(param), OpenApiParam.class);
		} catch (Exception e) {
			log.error("获取接口参数异常：",e);
			return null;
		}
    }
}

