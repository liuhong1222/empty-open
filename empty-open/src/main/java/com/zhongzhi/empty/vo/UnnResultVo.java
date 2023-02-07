package com.zhongzhi.empty.vo;

import java.util.List;

import com.zhongzhi.empty.response.UnnMobileStatus;

import lombok.Data;

/**
 * 空号结果vo
 * @author liuh
 * @date 2021年12月2日
 */
@Data
public class UnnResultVo {

	private List<UnnMobileStatus> list;
	
	private int noCacheCount;
	
	public UnnResultVo() {}
	
	public UnnResultVo(List<UnnMobileStatus> list,int noCacheCount) {
		this.list = list;
		this.noCacheCount = noCacheCount;
	}
	
	
}
