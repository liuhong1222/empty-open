package com.zhongzhi.empty.enums;

/**
 * 实时检测接口类型枚举
 * @author liuh
 * @date 2021年11月2日
 */
public enum RealtimeCheckTypeEnum {
    CHUANGLAN(0, "创蓝");

    private Integer code;
    private String desc;

    RealtimeCheckTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
