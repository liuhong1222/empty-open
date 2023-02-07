package com.zhongzhi.empty.entity;

import com.google.common.collect.ListMultimap;
import com.zhongzhi.empty.enums.RealtimeReportGroupEnum;

public class RealtimeFileDetectionResult {

    /**
     * 手机号及状态码map
     */
    private ListMultimap<RealtimeReportGroupEnum,String> data;

    /**
     * 状态组别号-手机号
     * @return
     */
    public ListMultimap<RealtimeReportGroupEnum,String> getData() {
        return data;
    }


    public void setData(ListMultimap<RealtimeReportGroupEnum,String> data) {
        this.data = data;
    }
}
