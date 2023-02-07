package com.zhongzhi.empty.vo;

import java.io.File;
import java.util.List;

import lombok.Data;

/**
 * 文件分割vo
 * @author liuh
 * @date 2021年12月4日
 */
@Data
public class SplitFileVo {

	private File[] fileList;
	
	private Integer errorCounts = 0;
	
	private List<String> mobileList ;
	
	public SplitFileVo() {}
	
	public SplitFileVo(File[] fileList,Integer errorCounts,List<String> mobileList) {
		this.fileList = fileList;
		this.errorCounts = errorCounts;
		this.mobileList = mobileList;
	}
}
