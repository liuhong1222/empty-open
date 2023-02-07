package com.zhongzhi.empty.response;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 号码实时查询基础版接口返回结果实体类
 * @author liuh
 * @date 2021年11月2日
 */
@Data
@ApiModel("号码实时查询基础版接口返回结果实体类")
public class MobileStatusStaticResult implements Serializable {

	private static final long serialVersionUID = -4679503983500255644L;

	private Integer chargeCount;

    private List<RealtimeResult> mobiles;
}
