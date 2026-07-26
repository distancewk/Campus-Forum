package com.campus.ai.exception;

import com.campus.common.exception.BusinessException;

/**
 * 内容未通过安全审核（被拒绝/需人工复核）时抛出。
 * 继承 BusinessException，因而由全局异常处理器以 4xx + 中文文案返回，不泄露内部细节。
 */
public class ContentRejectedException extends BusinessException {

    public ContentRejectedException(String message) {
        super(422, message);
    }

    public ContentRejectedException(String message, Throwable cause) {
        super(422, message);
        initCause(cause);
    }
}
