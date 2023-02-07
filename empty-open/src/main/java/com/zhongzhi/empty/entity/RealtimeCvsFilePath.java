package com.zhongzhi.empty.entity;

import java.util.Date;

import lombok.Data;

/**
 * 实时检测结果 下载地址类
 * @author liuh
 * @date 2021年10月30日
 */
@Data
public class RealtimeCvsFilePath{

	private Integer id;
	
	private Long customerId;

	private Date createDate;

	private Long realtimeId;

	private String normalFilePath;

	private String normalFileSize;

	private Integer normalNumber;

	private String emptyFilePath;

	private String emptyFileSize;

	private Integer emptyNumber;
	
	private String oncallFilePath;
	
	private String oncallFileSize;
	
	private Integer oncallNumber;
	
	private String notOnlineFilePath; 
	
	private String notOnlineFileSize;

	private Integer notOnlineNumber;
	
	private String shutdownFilePath; 
	
	private String shutdownFileSize;

	private Integer shutdownNumber;
	
	private String likeShutdownFilePath; 
	
	private String likeShutdownFileSize;

	private Integer likeShutdownNumber;
	
	private String tingjiFilePath; 
	
	private String tingjiFileSize;

	private Integer tingjiNumber;
	
	private String mnpFilePath; 
	
	private String mnpFileSize;

	private Integer mnpNumber;
	
	private String moberrFilePath; 
	
	private String moberrFileSize;

	private Integer moberrNumber;
	
	private String unknownFilePath; 
	
	private String unknownFileSize;

	private Integer unknownNumber;

	private String zipName;

	private String zipPath;

	private String zipSize;

	private Integer totalNumber;
	
	private Date createTime;

}
