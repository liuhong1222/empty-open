package com.zhongzhi.empty.entity;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class InternationalTxtFileContent implements Serializable{

	private static final long serialVersionUID = -324077318767155225L;

	private String fileCode;//文件编码格式
	
	private Integer errorCounts;
	
	private Integer mobileCounts;//有效号码个数
	
	private String targetFilePath;
	
	private Long fileSize;
	
	private String targetFileName;
	
	private String sourceFileName;
}
