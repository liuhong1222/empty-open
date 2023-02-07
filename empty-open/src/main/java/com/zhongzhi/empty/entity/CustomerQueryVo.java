package com.zhongzhi.empty.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

/**
 * 客户管理 查询结果对象
 * @author liuh
 * @date 2021年10月26日
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "CustomerQueryVo对象", description = "客户管理查询参数")
public class CustomerQueryVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "客户名称")
    private String name;

    @ApiModelProperty(value = "代理商编号")
    private Long agentId;

    @ApiModelProperty(value = "公司名称")
    private String companyName;

    @ApiModelProperty(value = "公司简称")
    private String companyShortName;

    @ApiModelProperty(value = "手机号码")
    private String phone;

    @ApiModelProperty(value = "登录密码")
    private String password;

    @ApiModelProperty(value = "联系邮箱")
    private String email;

    @ApiModelProperty(value = "客户类型（1：企业，0：个人，9：其他）")
    private Integer customerType;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "审批状态，0：待审核，9：已认证，1：已驳回")
    private Integer state;

    @ApiModelProperty("IP")
    private String ip;

    @ApiModelProperty("区域")
    private String area;

    @ApiModelProperty("运营商")
    private String operator;

    @ApiModelProperty(value = "referer")
    private String referer;

    @ApiModelProperty(value = "版本")
    private Integer version;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "修改时间")
    private Date updateTime;

    @ApiModelProperty(value = "客户认证信息")
    private CustomerExt customerExt;

    @ApiModelProperty(value = "appId")
    private String appId;

    @ApiModelProperty(value = "appKey")
    private String appKey;

    @ApiModelProperty(value = "接口状态，0：禁用，1：启用")
    private Integer apiState;

    @ApiModelProperty(value = "身份证名称")
    private String idCardName;

    @ApiModelProperty(value = "已认证企业名称")
    private String certifiedCompanyName;

    public String getName() {
        if (StringUtils.isNotBlank(certifiedCompanyName)) {
            return certifiedCompanyName;
        } else if (StringUtils.isNotBlank(idCardName)) {
            return idCardName;
        } else {
            return phone;
        }
    }


    /*****************账户统计信息***********************/
    /**
     * 充值总金额
     */
    @ApiModelProperty(value = "充值总计（元）")
    private String paymentAmountTotal;

    /**
     * 充值总条数
     */
    @ApiModelProperty(value = "充值总条数")
    private long rechargeNumberTotal;

    /**
     * 剩余总条数（不含冻结条数）
     */
    @ApiModelProperty(value = "剩余条数")
    private long remainNumberTotal;

    /**
     * 赠送条数
     */
    @ApiModelProperty(value = "赠送条数")
    private long giftNumber;

    @ApiModelProperty(value = "空号检测消耗总条数")
    private long emptyConsumeTotalCount;

    @ApiModelProperty(value = "空号检测退款总条数")
    private long emptyRefundTotalCount;

    @ApiModelProperty(value = "空号检测退款总金额")
    private String emptyRefundTotalPay;

    /**
     * 实时检测统计
     */
    @ApiModelProperty(value = "实时检测剩余条数")
    private long realtimeBalance;

    @ApiModelProperty(value = "实时检测充值总条数")
    private long realtimeRechargeTotalCount;

    @ApiModelProperty(value = "实时检测消耗总条数")
    private long realtimeConsumeTotalCount;

    @ApiModelProperty(value = "实时检测退款总条数")
    private long realtimeRefundTotalCount;

    @ApiModelProperty(value = "实时检测赠送总条数")
    private long realtimeGiftTotalCount;

    @ApiModelProperty(value = "实时检测充值总金额")
    private String realtimeRechargeTotalPay;

    @ApiModelProperty(value = "实时检测退款总金额")
    private String realtimeRefundTotalPay;

}
