package com.zhongzhi.empty.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Range;

/**
 * 消耗记录
 * @author liuh
 * @date 2021年10月28日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "CustomerConsume对象", description = "客户消耗记录")
public class CustomerConsume extends BaseEntity {

	private static final long serialVersionUID = 4235150194310367098L;

	@ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "代理商编号")
    @NotNull(message = "代理商编号不能为空")
    private Long agentId;

    @ApiModelProperty(value = "客户编号")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @ApiModelProperty(value = "客户名称")
    @NotBlank(message = "客户名称不能为空")
    private String name;

    @ApiModelProperty(value = "手机号码")
    @NotBlank(message = "手机号码不能为空")
    private String phone;

    @ApiModelProperty(value = "消耗条数")
    private Long consumeNumber;

    @ApiModelProperty(value = "消费方式，0：冻结，1：扣款成功，2：解冻")
    private Integer consumeType;

    @ApiModelProperty(value = "空号检测主键")
    private Long emptyId;

    @ApiModelProperty(value = "产品类别，0：空号检测产品，1：实时检测产品")
//    @NotNull(message = "产品类别不能为空")
    @Range(min = 0, max = 1, message = "产品类别输入有误")
    private Integer category;

    @ApiModelProperty(value = "期初余条")
    private Long openingBalance;

    @ApiModelProperty(value = "期末余条")
    private Long closingBalance;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "版本")
    private Integer version;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "修改时间")
    private Date updateTime;

    public enum ConsumeType {
        /**
         * 冻结
         */
        FREEZE(0),
        /**
         * 扣款成功
         */
        DEDUCTION_SUCCESS(1),
        /**
         * 解冻
         */
        UNFREEZE(2);

        private int value;

        ConsumeType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

}
