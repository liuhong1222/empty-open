package com.zhongzhi.empty.entity;

import java.util.Date;

import lombok.Data;

/**
 * 检测结果 下载地址类
 * @author liuh
 * @date 2021年10月30日
 */
@Data
public class CvsFilePath{

	private Integer id;
	
	private Long customerId;

	private Date createDate;

	private Long emptyId;

	private String realFilePath;

	private String realFileSize;

	private Integer realNumber;

	private String emptyFilePath;

	private String emptyFileSize;

	private Integer emptyNumber;
	
	private String riskFilePath;
	
	private String riskFileSize;
	
	private Integer riskNumber;
	
	private String silentFilePath; 
	
	private String silentFileSize;

	private Integer silentNumber;

	private String zipName;

	private String zipPath;

	private String zipSize;

	private Integer totalNumber;
	
	private Date createTime;

}
