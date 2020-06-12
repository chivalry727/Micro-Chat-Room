package com.micro.common.code;

/**
 * @author Mr.zxb
 * @date 2020-06-04 21:44:43
 */
public enum BusinessCode {

    /**
     * ok
     */
    OK(0, "请求成功"),
    FAIL(100, "请求失败"),
    PARAM_ERROR(101, "参数错误"),
    SYSTEM_ERROR(500, "系统错误"),
    USER_INVALID(102, "用户名和密码错误"),
    NO_LOGIN(103, "没有登录");

    private Integer code;
    private String message;

    BusinessCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
