package com.zhongzhi.empty.enums;

/**
 * 创蓝实时监测接口返回状态枚举
 * @author liuh
 * @date 2021年11月2日
 */
public enum MobileStatusStardardDxStateEnum{
	TINGJI(0, "在网状态停机"),
    NORMAL(1, "在网状态正常"),
    ONLINE_NOUSE(2, "在网但不可用"),
    NOT_EXISTS(3, "销号/未启用"),
    NOT_QUERY(4, "无法查询"),
    GUANJI(7, "关机"),
    QUERY_FAILED(10, "查询失败"),
    SERVER_EXCEPTION(9, "服务器异常"),;

    private final Integer code;
    private final String desc;

    MobileStatusStardardDxStateEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

	public Integer getCode() {
		return code;
	}

	public String getDesc() {
		return desc;
	}
	
	public static MobileStatusStardardDxStateEnum getEnum(Integer code){
		MobileStatusStardardDxStateEnum result = null;
		MobileStatusStardardDxStateEnum[] enums = MobileStatusStardardDxStateEnum.values();
		for (MobileStatusStardardDxStateEnum enumtemp : enums) {
			if (enumtemp.getCode().equals(code)) {
				result = enumtemp;
				break;
			}
		}
		return result;
	}
}
