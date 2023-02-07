package com.zhongzhi.empty.http.realtime;

import lombok.Data;

@Data
public class MobileRealtimeStatus {

	private String orderNo;
	
	private String handleTime;
	
	private String mobile;
	
	private String area;
	
	private String numberType;
	
	private String status;
	
	private String mnpStatus;
	
	private String remark;
}
