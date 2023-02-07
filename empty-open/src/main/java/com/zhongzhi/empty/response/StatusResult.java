package com.zhongzhi.empty.response;

import java.util.List;

import lombok.Data;

@Data
public class StatusResult {

	private Integer jhs;
	
	private List<String> jhhms;
	
	private Integer wjhs;
	
	private List<String> wjhhms;
	
	private Integer wzs;
	
	private List<String> wzhms;
	
	private String pch;
}
