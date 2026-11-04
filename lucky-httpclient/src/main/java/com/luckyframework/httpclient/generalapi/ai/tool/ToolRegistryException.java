package com.luckyframework.httpclient.generalapi.ai.tool;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 工具注册表异常：同名冲突、按名未命中等注册表层面的错误表达。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class ToolRegistryException extends LuckyRuntimeException {

    public ToolRegistryException(String message) {
        super(message);
    }

    public ToolRegistryException(String message, Throwable ex) {
        super(message, ex);
    }
}
