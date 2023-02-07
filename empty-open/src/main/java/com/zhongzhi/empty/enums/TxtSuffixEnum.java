package com.zhongzhi.empty.enums;

/**
 * 文件后缀名枚举
 * 
 */
public enum TxtSuffixEnum {
	/**
	 * 所有号码
	 */
	ALL("all.txt"),
	/**
	 * 实号一组
	 */
	REAL("real.txt"),
	/**
	 * 空号一组
	 */
	KONG("kong.txt"),
	/**
	 * 沉默号
	 */
	SILENCE("silence.txt"),
	/**
	 * 停机号
	 */
	OUT_SERVICE("outService.txt"),

	/**
	 * 关机号
	 */
	SHUTDOWN("shutdown.txt"),;
	private String txtSuffix;

	TxtSuffixEnum(String txtSuffix) {
		this.txtSuffix = txtSuffix;
	}

	public String getTxtSuffix() {
		return txtSuffix;
	}
}
