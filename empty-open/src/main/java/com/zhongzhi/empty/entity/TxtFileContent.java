package com.zhongzhi.empty.entity;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class TxtFileContent implements Serializable{

	private static final long serialVersionUID = -624925120405918745L;

	private String fileCode;//文件编码格式
	
	private Integer errorCounts;
	
	private Integer mobileCounts;//有效号码列表
	
	private List<String> mobileList;
}
