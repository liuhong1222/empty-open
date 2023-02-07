package com.zhongzhi.empty.response;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel("redis扣费返回结果")
public class LuaByDeductFeeResponse implements Serializable{

	private static final long serialVersionUID = -5635112669633687534L;
	private Long balance = 0L;//redis余额
	private Long deductFeeTime = 0L;//redis扣费时间
	
	public LuaByDeductFeeResponse() {}
	
	public LuaByDeductFeeResponse(Long balance,Long deductFeeTime) {
		this.balance = balance;
		this.deductFeeTime = deductFeeTime;
	}
}

