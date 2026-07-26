package com.campus.auth.exception;

/**
 * 刷新令牌校验失败（jti 不存在/已被吊销）时抛出。
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
