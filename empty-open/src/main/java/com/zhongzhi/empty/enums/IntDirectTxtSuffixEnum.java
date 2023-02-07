package com.zhongzhi.empty.enums;

/**
 * 定向国际检测文件
 * @author liuh
 * @date 2022年10月18日
 */
public enum IntDirectTxtSuffixEnum {
	/**
	 * 所有号码
	 */
	ALL("all.txt"),
	/**
	 * 已激活
	 */
	ACTIVATE("active.txt"),
	/**
	 * 未注册
	 */
	NOREGISTER("no_register.txt")
	;
	  
	private String txtSuffix;

	IntDirectTxtSuffixEnum(String txtSuffix) {
		this.txtSuffix = txtSuffix;
	}

	public String getTxtSuffix() {
		return txtSuffix;
	}
}
