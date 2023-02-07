package com.zhongzhi.empty.controller;

import com.zhongzhi.empty.config.Limiter;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.service.balance.BalanceQueryService;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/**
 * 余额查询接口
 * @author liuh
 * @date 2021年11月10日
 */
@Slf4j
@RestController
@RequestMapping("/open/api")
@Api("对外余额查询接口 API")
public class BalanceQueryController extends BaseController{
	
	@Autowired
	private BalanceQueryService balanceQueryService;
    
    @Limiter(limitNum = 1, limitType = Limiter.LimitType.CUSTOMER)
    @PostMapping("/accountQuery")
    public ApiResult accountQuery(HttpServletRequest request) {
    	return balanceQueryService.balanceQuery(super.getIpAddr(request));
    }
}

