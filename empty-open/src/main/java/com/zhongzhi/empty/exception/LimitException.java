package com.zhongzhi.empty.exception;

import lombok.Data;

/**
 * 自定义限流 异常
 * @author liuh
 * @date 2021年10月29日
 */
@Data
public class LimitException extends RuntimeException {
	private Integer errorCode;
    private String message;
    
    public LimitException(String message) {
        super(message);
    }

    public LimitException(Integer errorCode, String message) {
    	this.errorCode = errorCode;
        this.message = message;
    }
}