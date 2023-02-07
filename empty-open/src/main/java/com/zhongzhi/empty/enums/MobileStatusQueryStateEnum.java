package com.zhongzhi.empty.enums;

/**
 * 创蓝实时监测接口返回状态枚举
 * @author liuh
 * @date 2021年11月2日
 */
public enum MobileStatusQueryStateEnum{
    NORMAL(1, "正常"),
    EMPTY(2, "空号"),
    ON_CALL(3, "通话中"),
    ONLINE_BUT_NOT_AVAILABLE(4, "不在网(空号)"),
    SHUTDOWN(5, "关机"),
    SUSPECTED_SHUTDOWN(7, "疑似关机"),
    SERVICE_SUSPENDED(13, "停机"),
    EXCEPTION_FAIL(10, "未知"),
    SERVER_EXCEPTION(9, "服务器异常"),
    NUMBER_ERROR(12, "号码错误"),
    NUMBER_PORTABILITY(11, "携号转网");

    private final Integer code;
    private final String desc;

    MobileStatusQueryStateEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

	public Integer getCode() {
		return code;
	}

	public String getDesc() {
		return desc;
	}
}
