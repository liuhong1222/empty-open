package com.zhongzhi.empty.exception;

import com.zhongzhi.empty.enums.ApiCode;

public class BusinessException extends RuntimeException {

    private ApiCode apiCode;

    public BusinessException(ApiCode apiCode) {
        super(apiCode.toString());
        this.apiCode=apiCode;
    }

    public BusinessException(ApiCode apiCode,String message) {
        super(message);
        this.apiCode=apiCode;
    }

    public BusinessException(ApiCode apiCode,String message, Throwable cause) {
        super(message,cause);
        this.apiCode=apiCode;
    }

    public ApiCode getErrorCodeEnum() {
        return apiCode;
    }
}
