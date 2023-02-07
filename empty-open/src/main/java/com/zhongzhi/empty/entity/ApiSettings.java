package com.zhongzhi.empty.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * 对外api接口管理
 * @author liuh
 * @date 2021年10月26日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "api对象", description = "对外api接口管理")
public class ApiSettings extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键appId")
    private String appId;

    @ApiModelProperty(value = "客户编号")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @ApiModelProperty(value = "appKey")
    private String appKey;

    @ApiModelProperty(value = "状态，0：禁用，1：启用")
    private Integer state;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "版本")
    private Integer version;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "修改时间")
    private Date updateTime;

}
