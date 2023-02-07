package com.zhongzhi.empty.entity;

import com.zhongzhi.empty.enums.MobileReportGroupEnum;

/**
 * 解析结果类
 * @author liuh
 * @date 2021年10月30日
 */
public class FileDetectionEnumAndDelivrdSet {
    /**
     * 文件检测结果组别枚举
     */
    private MobileReportGroupEnum.FileDetection fd;

    /**
     * 检测结果 0：有正常结果 1：状态未知 2：没有结果
     */
    private int detectedStatus;


    public MobileReportGroupEnum.FileDetection getFd() {
        return fd;
    }


    public void setFd(MobileReportGroupEnum.FileDetection fd) {
        this.fd = fd;
    }

    public int getDetectedStatus() {
        return detectedStatus;
    }


    public void setDetectedStatus(int detectedStatus) {
        this.detectedStatus = detectedStatus;
    }
}
