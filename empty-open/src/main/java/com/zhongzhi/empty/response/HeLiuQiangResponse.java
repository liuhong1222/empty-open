package com.zhongzhi.empty.response;

import lombok.Data;

@Data
public class HeLiuQiangResponse {

	private String status;
	
	private String msg;
	
	private String code;
	
	private StatusResult data;
}
