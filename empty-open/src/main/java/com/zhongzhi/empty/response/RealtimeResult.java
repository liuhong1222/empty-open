package com.zhongzhi.empty.response;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * 号码实时接口结果实体类
 * @author liuh
 * @date 2021年11月2日
 */
@Data
@ApiModel("号码实时接口结果实体类")
public class RealtimeResult implements Serializable {

	private static final long serialVersionUID = -6809577578506378459L;
	
	private String orderNo;
	
	private String handleTime;

	private String mobile;
	
	private Integer chargeStatus;

    private String mnpStatus;
    
    private Integer status;
    
    private String carrier;
    
    private String area;
    
    private String remark;
}
