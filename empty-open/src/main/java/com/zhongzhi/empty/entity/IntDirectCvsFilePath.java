package com.zhongzhi.empty.entity;

import java.util.Date;

import lombok.Data;

/**
 * 定向国际检测下载地址类
 * @author liuh
 * @date 2022年10月18日
 */
@Data
public class IntDirectCvsFilePath{

	private Integer id;
	
	private Long customerId;

	private Date createDate;

	private Long intDirectId;
	
	private String countryCode;

	private String productType;

	private String activeFilePath;

	private String activeFileSize;

	private Integer activeNumber;

	private String noRegisterFilePath;

	private String noRegisterFileSize;

	private Integer noRegisterNumber;

	private String zipName;

	private String zipPath;

	private String zipSize;

	private Integer totalNumber;
	
	private Date createTime;

	private Integer deleted;
	
	private Date updateTime;
}
