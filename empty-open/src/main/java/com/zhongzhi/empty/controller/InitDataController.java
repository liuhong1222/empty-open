package com.zhongzhi.empty.controller;

import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.service.InitDataService;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/**
 * 数据初始化
 * @author liuh
 * @date 2021年11月10日
 */
@Slf4j
@RestController
@RequestMapping("/data/init")
@Api("初始化数据接口 ")
public class InitDataController extends BaseController{
	
	@Autowired
	private InitDataService initDataService;
    
    @PostMapping("/balance")
    public ApiResult deleteTempFileByEmpty(HttpServletRequest request,Long customerId) {
    	return initDataService.customerBalanceHandle(customerId);
    }
}