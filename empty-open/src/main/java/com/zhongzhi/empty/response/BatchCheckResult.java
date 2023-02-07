package com.zhongzhi.empty.response;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 空号检测返回结果实体类
 * @author liuh
 * @date 2021年10月27日
 */
@Data
@ApiModel("空号检测返回结果实体类")
public class BatchCheckResult implements Serializable {
	private static final long serialVersionUID = -6043755344840276053L;

    private Integer chargeCount;

    private List<UnnMobileStatus> mobiles;
}
