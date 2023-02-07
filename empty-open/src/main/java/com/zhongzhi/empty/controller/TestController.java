package com.zhongzhi.empty.controller;

import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.response.UnnMobileNewStatus;
import com.zhongzhi.empty.service.empty.TestService;
import com.zhongzhi.empty.util.CommonUtils;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/open/test")
@Api("对外接口 API")
public class TestController extends BaseController{
	
	@Autowired
	private TestService testService;
    
    @PostMapping("/unn")
    public ApiResult<List<UnnMobileNewStatus>> unn(HttpServletRequest request,String mobiles) {
        Set<String> list = new HashSet<String>();
        for(String mobile : mobiles.split(",")) {
        	if(CommonUtils.isMobile(mobile)) {
        		list.add(mobile);
        	}
        }
        
        String[] mobileList = list.toArray(new String[list.size()]);
        if (mobileList.length == 0) {
            return ApiResult.result(ApiCode.MOBILE_COUNT_EXCEPTION, "无有效手机号码", null);
        }
        
        if (mobileList.length > 2000) {
            return ApiResult.result(ApiCode.MOBILE_COUNT_EXCEPTION, "手机号码个数不能超过2000个", null);
        }
        
        return testService.batchCheckNew(mobileList,super.getIpAddr(request));
    }
}

