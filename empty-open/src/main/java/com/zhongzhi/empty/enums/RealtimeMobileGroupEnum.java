package com.zhongzhi.empty.enums;

public enum RealtimeMobileGroupEnum {

	//正常
	NORMAL("normal", "blue"),
    //空号
	EMPTY("kong", "gray"),
    //通话中
	ON_CALL("oncall", "yellow"),
    //不在网
	NOT_ONLINE("notonline", "gray"),
    //关机
	GUANJI("guanji", "red"),
    //疑似关机
	LIKE_GUANJI("likeguanji", "red"),
    //停机
	TINGJI("tingji", "gray"),
	//携号转网
	MNP("mnp", "blue"),
    //号码错误
	MOBILE_ERROR("mobileerror", "gray"),
    //未知
	UNKNOWN("unknown", "yellow"),;
	
	
	public String group;
	public String color;

	private RealtimeMobileGroupEnum(String group, String color) {
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
		RealtimeMobileGroupEnum[] mobileGroupEnums = RealtimeMobileGroupEnum.values();
		for (RealtimeMobileGroupEnum mobileGroupEnum : mobileGroupEnums) {
			if (mobileGroupEnum.getGroup().equals(group)) {
				color = mobileGroupEnum.getColor();
				break;
			}
		}
		return color;
	}
	

}
