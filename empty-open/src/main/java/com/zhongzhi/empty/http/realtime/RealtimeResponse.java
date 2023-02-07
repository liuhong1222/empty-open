package com.zhongzhi.empty.http.realtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 号码实时接口结果响应实体类
 * @author liuh
 * @date 2021年11月2日
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RealtimeResponse {

    /**
     * 计费号码数量
     */
    private int chargeStatus;

    /**
     * 消息
     */
    private String message;

    /**
     * 响应码，000000成功，其他失败
     */
    private String code;

    /**
     * 检测结果响应数据
     */
    private MobileRealtimeStatus data;
}
