package com.zhongzhi.empty.controller;

import com.zhongzhi.empty.config.Limiter;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.param.OpenApiParam;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.response.MobileStatusStaticResult;
import com.zhongzhi.empty.response.RealtimeResult;
import com.zhongzhi.empty.service.realtime.RealtimeApiService;
import com.zhongzhi.empty.util.CommonUtils;
import com.zhongzhi.empty.util.ThreadLocalContainer;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 实时检测对外api接口
 * @author liuh
 * @date 2021年11月2日
 */
@Slf4j
@RestController
@RequestMapping("/open/api")
@Api("实时检测对外接口 API")
public class RealtimeApiController extends BaseController{
	
	@Autowired
	private RealtimeApiService realtimeApiService;

	@Limiter(limitNum = 20, limitType = Limiter.LimitType.CUSTOMER)
	@PostMapping("/mobileStatusQuery")
    public ApiResult<MobileStatusStaticResult> mobileStatusQuery(HttpServletRequest request) {
		OpenApiParam openApiParam = ThreadLocalContainer.getCustomerInfoVo().getOpenApiParam();
		if(StringUtils.isBlank(openApiParam.getMobiles())) {
			return ApiResult.result(ApiCode.PARAMETER_EXCEPTION, "手机号码不能为空", null);
		}
		
		Set<String> list = new HashSet<String>();
        for(String mobile : openApiParam.getMobiles().split(",")) {
        	if(CommonUtils.isMobile(mobile)) {
        		list.add(mobile);
        	}
        }
		
		if(list.size() <= 0) {
			log.warn("手机号码格式错误，appId: {}", openApiParam.getAppId());
            return ApiResult.result(ApiCode.PARAMETER_EXCEPTION, "手机号码格式不对", null);
    	}
		
		if(list.size() > 100) {
			log.warn("号码数量超限，appId: {}", openApiParam.getAppId());
            return ApiResult.result(ApiCode.PARAMETER_EXCEPTION, "手机号码数量超限", null);
    	}
                
        return realtimeApiService.mobileStatusStaticQuery(list,super.getIpAddr(request));
    }
	
	@Limiter(limitNum = 100, limitType = Limiter.LimitType.CUSTOMER)
    @PostMapping("/mobileStatusStatic")
    public ApiResult<RealtimeResult> mobileStatusStatic(HttpServletRequest request,@Valid OpenApiParam openApiParam) {
		if(StringUtils.isBlank(openApiParam.getMobiles())) {
			return ApiResult.result(ApiCode.PARAMETER_EXCEPTION, "手机号码不能为空", null);
		}
		
		if(!CommonUtils.isMobile(openApiParam.getMobiles())) {
			log.warn("手机号码格式错误，appId: {}", openApiParam.getAppId());
            return ApiResult.result(ApiCode.PARAMETER_EXCEPTION, "手机号码格式不对", null);
    	}
                
        return realtimeApiService.mobileStatusStaticQueryNew(openApiParam.getMobiles(),super.getIpAddr(request));
    }
	
	@Limiter(limitNum = 100, limitType = Limiter.LimitType.CUSTOMER)
    @PostMapping("/mobileStatusStardard")
    public ApiResult<RealtimeResult> mobileStatusStardard(HttpServletRequest request,@Valid OpenApiParam openApiParam) {
		if(StringUtils.isBlank(openApiParam.getMobiles())) {
			return ApiResult.result(ApiCode.PARAMETER_EXCEPTION, "手机号码不能为空", null);
		}
		
		if(!CommonUtils.isMobile(openApiParam.getMobiles())) {
			log.warn("手机号码格式错误，appId: {}", openApiParam.getAppId());
            return ApiResult.result(ApiCode.PARAMETER_EXCEPTION, "手机号码格式不对", null);
    	}
        
        return realtimeApiService.mobileStatusStardard(openApiParam.getMobiles(),super.getIpAddr(request));
    }
}

