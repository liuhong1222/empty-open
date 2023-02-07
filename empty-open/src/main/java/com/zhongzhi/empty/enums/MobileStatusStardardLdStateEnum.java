package com.zhongzhi.empty.enums;

/**
 * 创蓝实时监测接口返回状态枚举
 * @author liuh
 * @date 2021年11月2日
 */
public enum MobileStatusStardardLdStateEnum{
    NOT_START(1, "未启用"),
    QIANFEI(2, "欠费停机"),
    NORMAL(3, "正常"),
    TINGJI(4, "其它停机"),
    NOT_EXISTS(5, "已销号"),
    GUANJI(7, "关机"),
    QUERY_FAILED(10, "查询失败"),
    SERVER_EXCEPTION(9, "服务器异常"),
    ;
    private final Integer code;
    private final String desc;

    MobileStatusStardardLdStateEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

	public Integer getCode() {
		return code;
	}

	public String getDesc() {
		return desc;
	}
	
	public static MobileStatusStardardLdStateEnum getEnum(Integer code){
		MobileStatusStardardLdStateEnum result = null;
		MobileStatusStardardLdStateEnum[] enums = MobileStatusStardardLdStateEnum.values();
		for (MobileStatusStardardLdStateEnum enumtemp : enums) {
			if (enumtemp.getCode().equals(code)) {
				result = enumtemp;
				break;
			}
		}
		return result;
	}
}
