package com.zhongzhi.empty.http.unn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 空号检测响应结果data实体类
 * @author liuh
 * @date 2021年10月28日
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnnData {
    /**
     * 电话号码，仅支持国内号码
     */
    private String mobile;

    /**
     * 时间戳
     */
    private String lastTime;

    /**
     * 手机号所属区域
     */
    private String area;

    /**
     * 手机号运营商类型
     */
    private String numberType;

    /**
     * 是否收费。1：收费 0：不收费
     */
    private String chargesStatus;

    /**
     * 检测结果。0-空号 1-实号 2-停机 3-库无 4-沉默号 5-风险号
     */
    private String status;
}
