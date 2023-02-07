package com.zhongzhi.empty.http.unn;

/**
 * 空号接口枚举
 * @author liuh
 * @date 2021年10月28日
 */
public enum UnnIntefaceEnum {
	
    /**
     * 空号检测
     */
    UNN_CHECK("SRCreditBus/creditBus/findByMobilesBig",UnnResponse.class,"cl_unn"),
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

    private UnnIntefaceEnum(String subUrl,Class<?> clazz,String urlProperty) {
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
    public static UnnIntefaceEnum getBySubUrl(String subUrl) {
        for (UnnIntefaceEnum miaoType : UnnIntefaceEnum.values()) {
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
