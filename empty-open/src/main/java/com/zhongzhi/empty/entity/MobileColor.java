package com.zhongzhi.empty.entity;

import java.io.Serializable;

public class MobileColor implements Serializable{

	private static final long serialVersionUID = -918820434350081336L;

	private String mobile;//手机号码
	
	private String color;//颜色

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
	
	public MobileColor(String mobile,String color) {
		this.mobile = mobile;
		this.color = color;
	}
}
