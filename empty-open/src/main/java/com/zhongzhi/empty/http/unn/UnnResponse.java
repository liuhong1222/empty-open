package com.zhongzhi.empty.http.unn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 空号检测结果响应实体类
 * @author liuh
 * @date 2021年10月28日
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnnResponse {

    /**
     * 计费号码数量
     */
    private int chargeCounts;

    /**
     * 消息
     */
    private String resultMsg;

    /**
     * 响应码，000000成功，其他失败
     */
    private String resultCode;

    /**
     * 检测结果响应数据
     */
    private List<UnnData> resultObj;
}
