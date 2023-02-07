package com.zhongzhi.empty.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class InternationalRunTestDomian implements Serializable{
	
	private static final long serialVersionUID = -1961991087905655495L;

	private int totalCount;
	
	private String status; // 1执行中 2执行结束 3执行异常 
	
	private String sendID;
	
	private String code; // 文件标识
	
	public InternationalRunTestDomian() {}
	
	public InternationalRunTestDomian(int totalCount,String status,String code,String sendID) {
		this.totalCount = totalCount;
		this.status = status;
		this.code = code;
		this.sendID = sendID;
	}

}
