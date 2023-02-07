package com.zhongzhi.empty.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * 用户余额表
 * @author liuh
 * @date 2021年10月28日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "用户余额表", description = "用户余额表")
public class CustomerBalance extends BaseEntity {

	private static final long serialVersionUID = 4580486341158091825L;

	@ApiModelProperty(value = "主键id")
    private Integer id;

    @ApiModelProperty(value = "客户编号")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @ApiModelProperty(value = "空号检测余额,单位：条")
    private Long emptyCount;

    @ApiModelProperty(value = "实时检测余额,单位：条")
    private Long realtimeCount;
    
    @ApiModelProperty(value = "国际检测余额,单位：条")
    private Long internationalCount;
    
    @ApiModelProperty(value = "定向通用检测余额,单位：条")
    private Long directCommonCount;
    
    @ApiModelProperty(value = "line定向检测余额,单位：条")
    private Long lineDirectCount;

    @ApiModelProperty(value = "版本")
    private Integer version;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "修改时间")
    private Date updateTime;

    @ApiModelProperty(value = "上次结算时间")
    private Long lastTime;
}
