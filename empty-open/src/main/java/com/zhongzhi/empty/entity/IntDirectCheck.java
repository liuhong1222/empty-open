package com.zhongzhi.empty.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import javax.validation.constraints.Null;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 定向国际检测记录
 * @author liuh
 * @date 2022年10月18日
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "IntDirectCheck对象", description = "定向国际检测记录")
public class IntDirectCheck {

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "所属代理商编号")
    private Long agentId;

    @ApiModelProperty(value = "代理商名称")
    private String agentName;

    @ApiModelProperty(value = "客户编号")
    private Long customerId;

    @ApiModelProperty(value = "国际码号")
    private String countryCode;
    
    @ApiModelProperty(value = "外部文件id")
    private String externFileId;
    
    @ApiModelProperty(value = "产品类型")
    private String productType;
    
    @ApiModelProperty(value = "文件名称")
    private String fileName;

    @ApiModelProperty(value = "文件大小")
    private String fileSize;
    
    @ApiModelProperty(value = "客户上传文件地址")
    private String fileUrl;

    @ApiModelProperty(value = "已激活（条）")
    private Long activeCount;

    @ApiModelProperty(value = "未注册（条）")
    private Long noRegisterCount;

    @ApiModelProperty(value = "总条数")
    private Long totalNumber;

    @ApiModelProperty(value = "无效号码数")
    private Long illegalNumber;

    @ApiModelProperty(value = "状态；-1：号码数量为0；-2：客户余额不足，-3：代理商余额不足，0：待扣款，1：扣款成功，9：最终成功，10：用户已取消此任务")
    private Integer status;

    @ApiModelProperty(value = "第三方接口，0：聚赢")
    private Integer checkType;

    @ApiModelProperty(value = "逻辑删除，0：未删除，1：已删除")
    @Null(message = "逻辑删除不用传")
    private Integer deleted;

    @ApiModelProperty(value = "文件md5校验值")
    private String md5;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "版本")
    private Integer version;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "修改时间")
    private Date updateTime;

    public enum IntDirectCheckStatus {
        /**
         * 待分类号码数量太少，任务结束
         */
        AMOUNT_LESS(-1),
        /**
         * 客户余额不足，任务结束
         */
        CUSTOMER_NOT_ENOUGH(-2),
        /**
         * 代理商余额不足，任务结束
         */
        AGENT_NOT_ENOUGH(-3),
        /**
         * 待分类号码数量超过最大值，任务结束
         */
        AMOUNT_MORE(-4),
        /**
         * 待扣款
         */
        INIT(0),
        /**
         * 扣款成功
         */
        DEDUCTION_SUCCESS(1),
        /**
         * 调用接口检测成功
         */
        CHECK_SUCCESS(3),
        /**
         * 最终分类成功，任务结束
         */
        WORK_FINISH(9),
        /**
         * 检测失败
         */
        CHECK_FAIL(10);

        private final int status;

        IntDirectCheckStatus(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
