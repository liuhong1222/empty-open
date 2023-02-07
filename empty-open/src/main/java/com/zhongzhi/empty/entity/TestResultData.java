package com.zhongzhi.empty.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * 检测结果
 * @author liuh
 * @date 2021年10月30日
 */
@Data
public class TestResultData implements Serializable{

	private static final long serialVersionUID = 5156297098473289487L;

	private int totalCount;//总条数
	
	private CvsFilePath cvsFilePath;//检测结果实体类

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public CvsFilePath getCvsFilePath() {
		return cvsFilePath;
	}

	public void setCvsFilePath(CvsFilePath cvsFilePath) {
		this.cvsFilePath = cvsFilePath;
	}

	public TestResultData() {
		
	}
	
	public TestResultData(int totalCount,CvsFilePath cvsFilePath) {
		this.totalCount = totalCount;
		this.cvsFilePath = cvsFilePath;
	}
}
