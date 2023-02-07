package com.zhongzhi.empty.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 实时检测记录
 * @author liuh
 * @date 2021年11月2日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "RealtimeCheck对象", description = "实时检测记录")
public class RealtimeCheck extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "所属代理商编号")
    @NotNull(message = "所属代理商编号不能为空")
    private Long agentId;

    @ApiModelProperty(value = "代理商名称")
    private String agentName;

    @ApiModelProperty(value = "客户编号")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @ApiModelProperty(value = "文件名称")
    @NotBlank(message = "文件名称不能为空")
    private String name;

    @ApiModelProperty(value = "文件大小")
    @NotBlank(message = "文件大小不能为空")
    private String size;

    @ApiModelProperty(value = "实号（条）")
    private Long normal;

    @ApiModelProperty(value = "空号（条）")
    private Long empty;

    @ApiModelProperty(value = "通话中（条）")
    private Long onCall;

    @ApiModelProperty(value = "在网但不可用（条）")
    private Long onlineButNotAvailable;

    @ApiModelProperty(value = "关机（条）")
    private Long shutdown;

    @ApiModelProperty(value = "呼叫转移（条）")
    private Long callTransfer;

    @ApiModelProperty(value = "疑似关机（条）")
    private Long suspectedShutdown;

    @ApiModelProperty(value = "停机（条）")
    private Long serviceSuspended;

    @ApiModelProperty(value = "携号转网（条）")
    private Long numberPortability;

    @ApiModelProperty(value = "号码错误或未知（条）")
    private Long unknown;

    @ApiModelProperty(value = "检测失败（条）")
    private Long exceptionFailCount;

    @ApiModelProperty(value = "检测文件中无效号码（条）")
    private Long illegalNumber;

    @ApiModelProperty(value = "总条数（不含无效号码）；null表示未检测条数")
    private Long totalNumber;

    @ApiModelProperty(value = "客户上传文件地址")
    @NotBlank(message = "客户上传文件地址不能为空")
    private String fileUrl;

    @ApiModelProperty(value = "状态；-1：号码数量为0；-2：客户余额不足，-3：代理商余额不足，0：待扣款，1：扣款成功，9：最终成功，10：用户已取消此任务")
    private Integer status;

    @ApiModelProperty(value = "第三方实时检测接口，0：创蓝")
    private Integer checkType;

    @ApiModelProperty(value = "接口重试次数")
    private Integer retryCount;

    @ApiModelProperty(value = "逻辑删除，0：未删除，1：已删除")
    @Null(message = "逻辑删除不用传")
    private Integer deleted;

    @ApiModelProperty(value = "文件md5校验值")
    private String md5;

    @ApiModelProperty(value = "接口返回检测数")
    private String line;

    @ApiModelProperty(value = "已去重待检测数")
    private Long sendCount;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "版本")
    private Integer version;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "修改时间")
    private Date updateTime;

    public enum RealtimeCheckStatus {
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

        RealtimeCheckStatus(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
