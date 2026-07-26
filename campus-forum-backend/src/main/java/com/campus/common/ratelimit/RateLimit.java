package com.campus.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多维限流注解。作用于 Controller 写接口方法。
 * key 为 Spring EL 表达式，默认引用当前用户 ID（#userId）；scope="ip" 时按请求 IP 限流。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String scope() default "user";

    String key() default "#userId";

    int limit() default 10;

    int window() default 60;
}
