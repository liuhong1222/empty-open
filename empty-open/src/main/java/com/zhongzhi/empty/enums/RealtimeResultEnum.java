package com.zhongzhi.empty.enums;

/**
 * 实时结果说明枚举
 * @author liuh
 * @date 2021年11月30日
 */
public enum RealtimeResultEnum {

    NM(1,"正常"),
    EP(2,"空号"),
    CL(3,"通话中"),
    NO(4,"不在网(空号)"),
    GJ(5,"关机"),
    LGJ(7,"疑似关机"),
    SE(9,"服务器异常"),
    UK(10,"未知"),
    ME(12,"号码错误"),
    TJ(13,"停机"),
    FD(-1,"检测失败"),
    ;

    private Integer status;
    
    private String desc;

    RealtimeResultEnum(int status,String desc){
        this.status=status;
        this.desc=desc;
    }
    
    public static String getDesc(int status) {
    	RealtimeResultEnum[] pes = RealtimeResultEnum.values();
        for (RealtimeResultEnum pe : pes) {
            if (pe.getStatus() == status) {
                return pe.getDesc();
            }
        }
        
        return null;
    }

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
