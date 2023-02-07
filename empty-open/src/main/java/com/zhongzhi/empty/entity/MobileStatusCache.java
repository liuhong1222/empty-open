package com.zhongzhi.empty.entity;

import java.io.Serializable;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * 号码缓存库实体类
 * @author liuh
 * @date 2021年10月28日
 */
@Data
@Document(collection = "MobileStatusCache")
public class MobileStatusCache implements Serializable{

	private static final long serialVersionUID = 1115337201515146296L;

	@Id
	private String mobile;

	/**
	 * 号码状态  0：空号 1：实号 4：沉默号 5：风险号 12：号码错误
	 */
	private String status;

	/**
	 * 创建时间
	 */
	private Date createTime;
}
