package com.zhongzhi.empty.response;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * 空号状态结果实体类
 * @author liuh
 * @date 2021年10月27日
 */
@Data
@ApiModel("空号状态新结果实体类")
public class UnnMobileNewStatus implements Serializable {

	private static final long serialVersionUID = 4063828338259473551L;

	private String mobile;
	
	private String lastTime;
	
	private String area;
	
	private String numberType;

    private String chargesStatus;
    
    private String status;
}
