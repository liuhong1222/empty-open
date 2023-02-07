package com.zhongzhi.empty.http.realtime;

/**
 * 实时接口枚举
 * @author liuh
 * @date 2021年10月28日
 */
public enum RealtimeIntefaceEnum {
	
    /**
     * 号码实时查询基础版
     */
    MOB_STATUS_STATIC("open/mobstatus/mobstatus-query-basic",RealtimeResponse.class,"cl_realtime"),
    
    /**
     * 号码实时查询
     */
    MOB_STATUS("open/mobstatus/mobstatus-query",RealtimeResponse.class,"cl_realtime"),
    ;

	/**
     * http 访问子url
     */
    private String subUrl;
    
    /**
     * 接口属性-用于获取httpclient
     */
    private String urlProperty;
    
    /**
     * 返回的结果类型
     */
    private Class<?> clazz;

    private RealtimeIntefaceEnum(String subUrl,Class<?> clazz,String urlProperty) {
        this.subUrl = subUrl;
        this.clazz = clazz;
        this.urlProperty = urlProperty;
    }

    public String getSubUrl() {
		return subUrl;
	}

	public void setSubUrl(String subUrl) {
		this.subUrl = subUrl;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	 /**
     * 通过subUrl获取
     *
     * @param subUrl subUrl
     * @return MIaoDiIntefaceEnum
     */
    public static RealtimeIntefaceEnum getBySubUrl(String subUrl) {
        for (RealtimeIntefaceEnum miaoType : RealtimeIntefaceEnum.values()) {
            if (subUrl.equals(miaoType.subUrl)) {
                return miaoType;
            }
        }
        return null;
    }

	public String getUrlProperty() {
		return urlProperty;
	}

	public void setUrlProperty(String urlProperty) {
		this.urlProperty = urlProperty;
	}
}
