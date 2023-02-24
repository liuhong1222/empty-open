package com.zhongzhi.empty.http.international;

/**
 * 国际接口枚举
 * @author liuh
 * @date 2022年6月9日
 */
public enum InternationalIntefaceEnum {
	
    /**
     *定向上传
     */
    UPLOAD("Interface/api/UploadDx.ashx",InternationalUploadResponse.class,"cl_international"),
    
    /**
     *上传
     */
    COMMON_UPLOAD("Interface/api/Upload.ashx",InternationalUploadResponse.class,"cl_international"),
    
    /**
     * 查询
     */
    QUERY("Interface/api/Query.ashx",InternationalQueryResponse.class,"cl_international"),
    
    /**
     * 下载
     */
    DOWNLOAD("Interface/api/Download.ashx",InternationalQueryResponse.class,"cl_international"),
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

    private InternationalIntefaceEnum(String subUrl,Class<?> clazz,String urlProperty) {
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
    public static InternationalIntefaceEnum getBySubUrl(String subUrl) {
        for (InternationalIntefaceEnum miaoType : InternationalIntefaceEnum.values()) {
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
