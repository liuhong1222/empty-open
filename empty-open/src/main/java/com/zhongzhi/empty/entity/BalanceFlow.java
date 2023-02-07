package com.zhongzhi.empty.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 余额变更流水表实体类
 * @author liuh
 * @date 2021年11月9日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "余额变更流水表对象", description = "余额变更流水表实体类")
public class BalanceFlow extends BaseEntity {

	private static final long serialVersionUID = -7644527666918040290L;

	@ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "用户id")
    private Long customerId;
    
    @ApiModelProperty(value = "产品类别，0-空号检测 1-实时检测")
    private Integer category;

    @ApiModelProperty(value = "冻结条数")
    private Long freezdMoney;

    @ApiModelProperty(value = "redis余额条数")
    private Long redisMoney;

    @ApiModelProperty(value = "实际扣款条数")
    private Long realMoney;

    @ApiModelProperty(value = "mysql数据库余额")
    private Long dbMoney;

    @ApiModelProperty(value = "修改前的mysql余额")
    private Long oldDbMoney;

    @ApiModelProperty(value = "上次记录时间")
    private Long lastTime;

    @ApiModelProperty(value = "本次记录时间")
    private Long curTime;

    @ApiModelProperty(value = "操作类型 -4-结算扣款")
    private Integer optType;
}
