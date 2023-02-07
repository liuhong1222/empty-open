package com.zhongzhi.empty.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 空号检测参数实体类
 * @author liuh
 * @date 2021年10月27日
 */
@Data
@ApiModel("对外API接口参数")
public class OpenApiParam implements Serializable {
    private static final long serialVersionUID = -5353973980674510450L;

    @NotBlank(message = "appId不能为空")
    private String appId;

    @NotBlank(message = "appKey不能为空")
    private String appKey;

    @NotBlank(message = "手机号码不能为空")
    private String mobiles;

    @ApiModelProperty("查询类型 1：MD5(32位小写)，0：普通手机号；默认0")
    private Integer type;
}
