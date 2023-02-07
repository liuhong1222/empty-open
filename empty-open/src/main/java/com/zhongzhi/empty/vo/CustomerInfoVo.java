package com.zhongzhi.empty.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

import com.zhongzhi.empty.param.OpenApiParam;

/**
 * 用户信息实体类
 * @author liuh
 * @date 2021年10月26日
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "CustomerInfoVo对象", description = "用户信息线程缓存实体类")
public class CustomerInfoVo implements Serializable {
	private static final long serialVersionUID = 8601319101680444700L;

	@ApiModelProperty(value = "代理商Id")
    private Long agentId;

    @ApiModelProperty(value = "用户id")
    private Long customerId;

    @ApiModelProperty(value = "代理商名称")
    private String companyName;

    @ApiModelProperty(value = "客户名称")
    private String customerName;

    @ApiModelProperty(value = "客户手机号码")
    private String phone;
    
    private OpenApiParam openApiParam;
}