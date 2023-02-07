package com.zhongzhi.empty.enums;

/**
 * 定向国际检测类型
 * @author liuh
 * @date 2022年10月18日
 */
public enum DirectTypeEnum {
    VIBER("viber", "7A4A66DB1B7B7777",ProductEnum.DIRECT_COMMON),
    ZALO("zalo", "431039DC0568D3FD",ProductEnum.DIRECT_COMMON),
    BOTIM("botim", "E6C1CD22E635B389",ProductEnum.DIRECT_COMMON),
    LINE("line", "28D47F60DA5B52FC",ProductEnum.LINE_DIRECT),
    ;

    private String name;
    private String code;
    private ProductEnum productEnum;

    DirectTypeEnum(String name, String code,ProductEnum productEnum) {
        this.code = code;
        this.name = name;
        this.setProductEnum(productEnum);
    }
    
    public static String getCodeByName(String name) {
    	DirectTypeEnum[] pes = DirectTypeEnum.values();
        for (DirectTypeEnum pe : pes) {
            if (pe.getName().equals(name)) {
                return pe.getCode();
            }
        }
        return null;
    }
    
    public static ProductEnum getProductEnumByName(String name) {
    	DirectTypeEnum[] pes = DirectTypeEnum.values();
        for (DirectTypeEnum pe : pes) {
            if (pe.getName().equals(name)) {
                return pe.getProductEnum();
            }
        }
        return null;
    }
    
    public static ProductEnum getProductEnumByCode(String code) {
    	DirectTypeEnum[] pes = DirectTypeEnum.values();
        for (DirectTypeEnum pe : pes) {
            if (pe.getCode().equals(code)) {
                return pe.getProductEnum();
            }
        }
        return null;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public ProductEnum getProductEnum() {
		return productEnum;
	}

	public void setProductEnum(ProductEnum productEnum) {
		this.productEnum = productEnum;
	}
}
