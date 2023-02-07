package com.zhongzhi.empty.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 代理商管理 查询结果对象
 * @author liuh
 * @date 2021年10月26日
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "AgentQueryVo对象", description = "代理商管理查询参数")
public class AgentQueryVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "联系人姓名")
    private String linkmanName;

    @ApiModelProperty(value = "联系人手机号码")
    private String linkmanPhone;

    @ApiModelProperty(value = "联系人邮箱")
    private String linkmanEmail;

    @ApiModelProperty(value = "营业执照地址")
    private String businessLicensePath;

    @ApiModelProperty(value = "公司名称")
    private String companyName;

    @ApiModelProperty(value = "公司简称")
    private String companyShortName;

    @ApiModelProperty(value = "营业执照所在地")
    private String businessLicenseAddress;

    @ApiModelProperty(value = "营业执照号")
    private String businessLicenseNumber;

    @ApiModelProperty(value = "企业法人姓名")
    private String legalPerson;

    @ApiModelProperty(value = "营业执照有效期开始时间")
    private String businessLicenseExpireStartTime;

    @ApiModelProperty(value = "营业执照有效期结束时间")
    private String businessLicenseExpireEndTime;

    @ApiModelProperty(value = "代理商空号检测等级名称")
    private String agentLevel;

    @ApiModelProperty(value = "单价（元/条）")
    private BigDecimal price;

    @ApiModelProperty(value = "最小支付金额")
    private Integer minPaymentAmount;

    @ApiModelProperty(value = "最小充值条数")
    private Integer minRechargeNumber;

    @ApiModelProperty(value = "预警条数")
    private Integer warningsNumber;

    @ApiModelProperty(value = "代理商实时检测等级名称")
    private String realLevel;

    @ApiModelProperty(value = "实时检测单价（元/条）")
    private BigDecimal realPrice;

    @ApiModelProperty(value = "实时检测最小支付金额")
    private Integer realMinPaymentAmount;

    @ApiModelProperty(value = "实时检测最小充值条数")
    private Integer realMinRechargeNumber;

    @ApiModelProperty(value = "实时检测预警条数")
    private Integer realWarningsNumber;

    @ApiModelProperty(value = "是否注册赠送，0：否，1：是")
    private Integer registerGift;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "版本")
    private Integer version;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "修改时间")
    private Date updateTime;

    @ApiModelProperty(value = "状态，0：禁用，1：启用")
    private Integer state;


    /*********************充值统计*******************************/

    /**
     * 空号检测代理商充值总金额
     */
    @ApiModelProperty(value = "空号检测代理商充值总金额")
    private long paymentAmountTotal;

    /**
     * 空号检测代理商充值总条数
     */
    @ApiModelProperty(value = "空号检测代理商充值总条数")
    private long rechargeNumberTotal;

    /**
     * 空号检测代理商余额（条数）
     */
    @ApiModelProperty(value = "空号检测代理商剩余条数")
    private long remainNumberTotal;

    @ApiModelProperty(value = "空号检测客户充值总条数")
    private long emptyCustomerConsumeTotalCount;

    @ApiModelProperty(value = "空号检测客户退款总条数")
    private long emptyCustomerRefundTotalCount;

    @ApiModelProperty(value = "空号检测客户退款总金额")
    private String emptyCustomerRefundTotalPay;

    /**
     * 实时检测统计
     */
    @ApiModelProperty(value = "实时检测代理商剩余条数")
    private long realtimeAgentBalance;

    @ApiModelProperty(value = "实时检测代理商充值总条数")
    private long realtimeAgentRechargeTotalCount;

    @ApiModelProperty(value = "实时检测客户消耗总条数")
    private long realtimeCustomerConsumeTotalCount;

    @ApiModelProperty(value = "实时检测客户退款总条数")
    private long realtimeCustomerRefundTotalCount;

    @ApiModelProperty(value = "实时检测代理商充值总金额")
    private long realtimeAgentRechargeTotalPay;

    @ApiModelProperty(value = "实时检测客户退款总金额")
    private String realtimeCustomerRefundTotalPay;

}