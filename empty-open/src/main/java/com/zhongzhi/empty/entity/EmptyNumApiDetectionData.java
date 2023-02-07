package com.zhongzhi.empty.entity;

import java.util.Date;

/**
 * 空号api检测单个结果数据
 * @author liuh
 * @date 2021年10月30日
 */
public class EmptyNumApiDetectionData {

    private String mobile;

    private Date date;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
