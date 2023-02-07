package com.zhongzhi.empty.enums;

/**
 * 创蓝实时监测标准版接口移动号码返回状态枚举
 * @author liuh
 * @date 2021年11月2日
 */
public enum MobileStatusStardardYdStateEnum{
    NORMAL(1, "正常"),
    TINGJI(2, "停机"),
    ONLINE_NOTUSE(3, "在网但不可用"),
    NOT_AVAILABLE(4, "不在网(空号)"),
    NO_MESSAGE(5, "无短信能力"),
    QIANFEI(6, "欠费"),
    GUANJI(7, "长时间关机"),
    NOT_EXISTS(8, "销号/未启用"),
    SERVER_EXCEPTION(9, "服务器异常"),
    QUERY_FAILED(10, "查询失败"),
    ;
    private final Integer code;
    private final String desc;

    MobileStatusStardardYdStateEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

	public final Integer getCode() {
		return code;
	}

	public final String getDesc() {
		return desc;
	}
	
	public static MobileStatusStardardYdStateEnum getEnum(Integer code){
		MobileStatusStardardYdStateEnum result = null;
		MobileStatusStardardYdStateEnum[] enums = MobileStatusStardardYdStateEnum.values();
		for (MobileStatusStardardYdStateEnum enumtemp : enums) {
			if (enumtemp.getCode().equals(code)) {
				result = enumtemp;
				break;
			}
		}
		return result;
	}
}
