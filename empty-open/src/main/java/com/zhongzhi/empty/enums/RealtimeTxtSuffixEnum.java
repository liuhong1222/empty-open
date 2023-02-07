package com.zhongzhi.empty.enums;

/**
 * 实时检测文件后缀
 * @author liuh
 * @date 2021年11月3日
 */
public enum RealtimeTxtSuffixEnum {
	/**
	 * 所有号码
	 */
	ALL("all.txt"),
	/**
	 * 正常
	 */
	NORMAL("normal.txt"),
	/**
	 * 空号
	 */
	EMPTY("kong.txt"),
	/**
	 * 通话中
	 */
	ON_CALL("oncall.txt"),
	/**
	 * 不在网
	 */
	NOT_ONLINE("notonline.txt"),
	/**
	 * 关机
	 */
	GUANJI("guanji.txt"),
	/**
	 * 疑似关机
	 */
	LIKE_GUANJI("likeguanji.txt"),
	/**
	 * 停机
	 */
	TINGJI("tingji.txt"),
	/**
	 * 携号转网
	 */
	MNP("mnp.txt"),
	/**
	 * 号码错误
	 */
	MOBILE_ERROR("mobileerror.txt"),
	/**
	 * 未知
	 */
	UNKNOWN("unknown.txt"),;
	private String txtSuffix;

	RealtimeTxtSuffixEnum(String txtSuffix) {
		this.txtSuffix = txtSuffix;
	}

	public String getTxtSuffix() {
		return txtSuffix;
	}
}
