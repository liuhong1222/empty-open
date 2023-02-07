package com.zhongzhi.empty.entity;

import java.util.Set;

import com.google.common.collect.ListMultimap;
import com.zhongzhi.empty.enums.MobileReportGroupEnum;

/**
 * @author liuh
 * @date 2021年10月30日
 */
public class EmptyNumApiDetectionResult {
    /**
     * 手机号及状态码map
     */
    private ListMultimap<MobileReportGroupEnum,EmptyNumApiDetectionData> data;
    /**
     * 新状态码set
     */
    private Set<String> newDelivrdSet;


    public ListMultimap<MobileReportGroupEnum, EmptyNumApiDetectionData> getData() {
        return data;
    }

    public void setData(ListMultimap<MobileReportGroupEnum, EmptyNumApiDetectionData> data) {
        this.data = data;
    }

    public Set<String> getNewDelivrdSet() {
        return newDelivrdSet;
    }


    public void setNewDelivrdSet(Set<String> newDelivrdSet) {
        this.newDelivrdSet = newDelivrdSet;
    }
}
