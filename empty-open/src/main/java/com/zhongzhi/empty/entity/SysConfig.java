package com.zhongzhi.empty.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * oem参数管理实体类
 * @author liuh
 * @date 2021年10月28日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "参数管理实体类", description = "参数管理实体类")
public class SysConfig extends BaseEntity {
	private static final long serialVersionUID = 1903628367760964664L;

	@ApiModelProperty(value = "主键id")
    private Long id;

    @ApiModelProperty(value = "key")
    @NotNull(message = "key")
    private String paramKey;

    @ApiModelProperty(value = "value")
    private String paramValue;

    @ApiModelProperty(value = "状态   0：隐藏   1：显示")
    private Integer status;

    @ApiModelProperty(value = "备注")
    private String remark;
}
