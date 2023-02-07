package com.zhongzhi.empty.enums;

/**
 * 用户检测方式枚举
 * @author liuh
 * @date 2021年10月29日
 */
public enum UserCheckTypeEnum {
    // 页面上传文件检测
    UPLOAD("upload"),
    // 接口上传文件检测
    API_UPLOAD("api_upload"),
    // 接口传参多个号码检测
    API("api"),
    // 号码实时查询标准版
    API_STARDARD("api_stardard"),
    // 页面单条检测
    SINGLE("single");

    private final String name;

    UserCheckTypeEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
