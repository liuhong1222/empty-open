package com.zhongzhi.empty.entity;

import java.util.Date;

import lombok.Data;

/**
 * 文件上传实体类
 * @author liuh
 * @date 2021年11月4日
 */
@Data
public class FileUpload {

	private Long id;
	
	private Long customerId;
	
	private String fileName;
	
	private Integer fileRows;
	
	private String fileUploadUrl;
	
	private String fileMd5;
	
	private String fileSize;
	
	private Date createTime;
}
