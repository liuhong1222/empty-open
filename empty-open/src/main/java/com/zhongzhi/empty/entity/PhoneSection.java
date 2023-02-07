package com.zhongzhi.empty.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 号码归属地实体类
 * @author liuh
 * @date 2021年11月4日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "号码归属地", description = "号码归属地实体类")
public class PhoneSection extends BaseEntity {

	private static final long serialVersionUID = -6017506650899726029L;

	@ApiModelProperty(value = "市区编码")
    private String areaCode;
	
	@ApiModelProperty(value = "市区")
    private String city;
	
	@ApiModelProperty(value = "省份编码")
    private String cityCode;
	
	@ApiModelProperty(value = "省份")
	private String province;
	
	@ApiModelProperty(value = "运营商")
    private String isp;
	
	@ApiModelProperty(value = "邮编")
    private String postCode;
	
	@ApiModelProperty(value = "3位号码段")
    private String prefix;
	
	@ApiModelProperty(value = "7位号码段")
    private String section;
}
