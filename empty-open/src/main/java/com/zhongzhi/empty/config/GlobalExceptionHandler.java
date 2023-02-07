package com.zhongzhi.empty.config;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.exception.BusinessException;
import com.zhongzhi.empty.exception.LimitException;
import com.zhongzhi.empty.response.ApiResult;

import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理handler
 * @author liuh
 * @date 2021年10月29日
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
	
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ApiResult error(Exception e){
        log.error("[Exception异常] - \n" + ExceptionUtils.getStackTrace(e));
        return ApiResult.fail(ApiCode.SYSTEM_EXCEPTION);
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ApiResult error(BusinessException businessException) {
        log.error("[全局异常] - \n" + ExceptionUtils.getStackTrace(businessException));
        return ApiResult.result(businessException.getErrorCodeEnum(), businessException.getMessage(),null);
    }
    
    @ExceptionHandler(LimitException.class)
    @ResponseBody
    public ApiResult error(LimitException limitException) {
        log.error("[全局异常] - \n" + ExceptionUtils.getStackTrace(limitException));
        return ApiResult.result(ApiCode.getApiCode(limitException.getErrorCode()), limitException.getMessage(),null);
    }
}