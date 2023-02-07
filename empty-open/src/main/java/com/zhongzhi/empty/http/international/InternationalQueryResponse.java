package com.zhongzhi.empty.http.international;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 国际查询接口返回结果实体类
 * @author liuh
 * @date 2022年6月9日
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InternationalQueryResponse {

    /**
     * 消息
     */
    private String ERR;

    /**
     * 响应码，100成功，其他失败
     */
    private String RES;

    /**
     * 检测结果响应数据
     */
    private QueryResponse DATA;
}
