package com.zhongzhi.empty.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.Date;

/**
 * 空号检测记录
 * @author liuh
 * @date 2021年10月28日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "EmptyCheck对象", description = "空号检测记录")
public class EmptyCheck extends BaseEntity {

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

    @ApiModelProperty(value = "实号包（条）")
    private Long realNumber;

    @ApiModelProperty(value = "沉默包（条）")
    private Long silentNumber;

    @ApiModelProperty(value = "空号包（条）")
    private Long emptyNumber;

    @ApiModelProperty(value = "风险包（条）")
    private Long riskNumber;

    @ApiModelProperty(value = "缓存未识别的条数（条）")
    private Long unknownNumber;

    @ApiModelProperty(value = "检测文件中无效号码（条）")
    private Long illegalNumber;

    @ApiModelProperty(value = "总条数；null表示未检测条数")
    private Long totalNumber;

    @ApiModelProperty(value = "客户上传检测号码文件URL")
    @NotBlank(message = "客户上传检测号码文件URL不能为空")
    private String fileUrl;

    @ApiModelProperty(value = "状态；-1：号码数量为0；-2：客户余额不足，-3：代理商余额不足，0：待扣款，1：扣款成功，2：缓存分析成功，3:接口检测成，9：最终成功，10：用户已取消此任务，11：定期删除文件完成")
    private Integer status;

    @ApiModelProperty(value = "待上传检测条数")
    private Long sendCount;

    @ApiModelProperty(value = "上传id")
    private String sendId;

    @ApiModelProperty(value = "接口返回检测条数")
    private String line;

    @ApiModelProperty(value = "检测接口类型：0 磬音（旧），1 磬音（新），2 创蓝")
    private Integer checkType;

    @ApiModelProperty(value = "号码池缓存完成，0：未完成，1：已完成")
    private Integer cacheFinish;

    @ApiModelProperty(value = "上传文件查询接口重试次数")
    private Integer retryCount;

    @ApiModelProperty(value = "逻辑删除，0：未删除，1：已删除")
    @Null(message = "逻辑删除不用传")
    private Integer deleted;

    @ApiModelProperty(value = "产品类别，0：空号检测产品，1：实时检测产品")
//    @NotNull(message = "产品类别不能为空")
    @Range(min = 0, max = 1, message = "产品类别输入有误")
    private Integer category;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "版本")
    private Integer version;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "修改时间")
    private Date updateTime;

    @ApiModelProperty(value = "文件md5校验值")
    private String md5;

    public enum EmptyCheckStatus {
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
        TO_DEDUCT(0),
        /**
         * 扣款成功
         */
        DEDUCTION_SUCCESS(1),
        /**
         * 缓存分类完成
         */
        CACHE_ANALYSIS_COMPLETE(2),
        /**
         * 调用检测接口发送检测号码文件成功，返回上传id
         */
        SEND_TO_CHECK(3),
        /**
         * 最终分类成功，任务结束
         */
        WORK_FINISH(9),
        /**
         * 检测失败
         */
        CHECK_FAIL(10),
        /**
         * 定期删除文件完成
         */
        FILE_DELETE_FINISH(11);

        private final int status;

        EmptyCheckStatus(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
