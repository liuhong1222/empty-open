package com.zhongzhi.empty.enums;

public enum MobileGroupEnum {

	//实号1组
    REAL_ONE("real", "blue"),
    //空号1组
    EMPTY_ONE("empty", "gray"),
    //沉默
    SILENCE("silence", "yellow"),
    //停机
    OUT_SERVICE("out_service", "gray"),
    //关机
    SHUT("shut", "red"),
    //状态不能识别
    UNKNOWN("unknown", "yellow"),
    //没有检测结果
    NO_RESULT("no_result", "yellow"),;
	
	
	public String group;
	public String color;

	private MobileGroupEnum(String group, String color) {
		this.group = group;
		this.color = color;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public static String getColor(String group){
		String color = "";
		MobileGroupEnum[] mobileGroupEnums = MobileGroupEnum.values();
		for (MobileGroupEnum mobileGroupEnum : mobileGroupEnums) {
			if (mobileGroupEnum.getGroup().equals(group)) {
				color = mobileGroupEnum.getColor();
				break;
			}
		}
		return color;
	}
	

}
