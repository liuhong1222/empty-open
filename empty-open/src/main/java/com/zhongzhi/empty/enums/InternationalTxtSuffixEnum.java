package com.zhongzhi.empty.enums;

/**
 * 国际文件后缀名枚举
 * @author liuh
 * @date 2022年9月13日
 */
public enum InternationalTxtSuffixEnum {
	/**
	 * 所有号码
	 */
	ALL("all.txt"),
	/**
	 * 已激活
	 */
	ACTIVATE("activate.txt"),
	/**
	 * 未激活
	 */
	NOACTIVE("noactive.txt"),
	/**
	 * 未知
	 */
	UNKNOWN("unknown.txt"),
	;
	  
	private String txtSuffix;

	InternationalTxtSuffixEnum(String txtSuffix) {
		this.txtSuffix = txtSuffix;
	}

	public String getTxtSuffix() {
		return txtSuffix;
	}
}
