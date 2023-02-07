package com.zhongzhi.empty.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * 实时检测结果
 * @author liuh
 * @date 2021年10月30日
 */
@Data
public class RealtimeResultData implements Serializable{

	private static final long serialVersionUID = 5156297098473289487L;

	private int totalCount;//总条数
	
	private RealtimeCvsFilePath realtimeCvsFilePath;//检测结果实体类

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public RealtimeCvsFilePath getCvsFilePath() {
		return realtimeCvsFilePath;
	}

	public void setCvsFilePath(RealtimeCvsFilePath realtimeCvsFilePath) {
		this.realtimeCvsFilePath = realtimeCvsFilePath;
	}

	public RealtimeResultData() {
		
	}
	
	public RealtimeResultData(int totalCount,RealtimeCvsFilePath realtimeCvsFilePath) {
		this.totalCount = totalCount;
		this.realtimeCvsFilePath = realtimeCvsFilePath;
	}
}
