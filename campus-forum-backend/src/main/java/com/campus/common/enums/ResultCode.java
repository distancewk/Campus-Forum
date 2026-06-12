package com.campus.common.enums;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(0, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    USER_DISABLED(410, "账号已被禁用"),
    VERIFY_CODE_EXPIRED(411, "验证码已过期"),
    VERIFY_CODE_WRONG(412, "验证码错误"),
    STUDENT_NO_EXISTS(413, "学号已注册"),
    EMAIL_EXISTS(414, "邮箱已注册"),
    LOGIN_FAIL(415, "学号或密码错误"),
    ACCOUNT_LOCKED(416, "账号已锁定，请15分钟后重试"),
    FILE_TOO_LARGE(420, "文件超过大小限制"),
    FILE_TYPE_NOT_ALLOWED(421, "不支持的文件格式"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
