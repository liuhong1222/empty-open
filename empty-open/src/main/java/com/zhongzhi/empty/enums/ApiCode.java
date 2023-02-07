package com.zhongzhi.empty.enums;

/**
 * REST API响应码
 * @author liuh
 * @date 2021年10月26日
 */
public enum ApiCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "坏请求"),

    UNAUTHORIZED(401, "非法访问"),

    NOT_PERMISSION(403, "没有权限,请先实名认证"),

    NOT_FOUND(404, "你请求的资源不存在"),

    FAIL(500, "操作失败"),


    LOGIN_EXCEPTION(4000, "登陆失败"),


    SYSTEM_EXCEPTION(5000, "系统异常!"),

    PARAMETER_EXCEPTION(5001, "请求参数校验异常"),

    PARAMETER_PARSE_EXCEPTION(5002, "请求参数解析异常"),

    HTTP_MEDIA_TYPE_EXCEPTION(5003, "HTTP Media 类型异常"),

    SPRING_BOOT_PLUS_EXCEPTION(5100, "系统处理异常"),

    BUSINESS_EXCEPTION(5101, "业务处理异常"),

    DAO_EXCEPTION(5102, "数据库处理异常"),

    VERIFICATION_CODE_EXCEPTION(5103, "验证码校验异常"),

    AUTHENTICATION_EXCEPTION(5104, "登陆授权异常"),

    UNAUTHENTICATED_EXCEPTION(5105, "没有访问权限"),

    UNAUTHORIZED_EXCEPTION(5106, "没有访问权限"),

    SMS_VERIFY_EXCEPTION(5107, "需要二次图形验证"),

    LIMITER_EXCEPTION(500008, "请求速率超限"),
    MOBILE_COUNT_EXCEPTION(5201, "手机号码个数超过最大值"),
    BALANCE_EXCEPTION(5202, "检测账户余额不足"),
    CONSUME_STATUS_EXCEPTION(5203, "消耗条数冻结状态更新失败"),
    COUNT_EXCEPTION(5204, "计数异常"),
    ;

    private final int code;
    private final String msg;

    ApiCode(final int code, final String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static ApiCode getApiCode(int code) {
        ApiCode[] ecs = ApiCode.values();
        for (ApiCode ec : ecs) {
            if (ec.getCode() == code) {
                return ec;
            }
        }
        return SUCCESS;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

}
