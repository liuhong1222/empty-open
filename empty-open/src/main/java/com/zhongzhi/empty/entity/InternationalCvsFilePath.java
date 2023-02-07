package com.zhongzhi.empty.entity;

import java.util.Date;

import lombok.Data;

/**
 *  国际检测结果 下载地址类
 * @author liuh
 * @date 2022年6月8日
 */
@Data
public class InternationalCvsFilePath{

	private Integer id;
	
	private Long customerId;

	private Date createDate;

	private Long internationalId;

	private String activeFilePath;

	private String activeFileSize;

	private Integer activeNumber;

	private String noRegisterFilePath;

	private String noRegisterFileSize;

	private Integer noRegisterNumber;
	
	private String unknownFilePath;

	private String unknownFileSize;

	private Integer unknownNumber;

	private String zipName;

	private String zipPath;

	private String zipSize;

	private Integer totalNumber;
	
	private Date createTime;

	private Integer deleted;
	
	private Date updateTime;
}
