package com.luckyframework.httpclient.generalapi.ai.tool;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 工具参数按声明Schema校验失败时的异常：参数值无法按声明类型转换、或必填项缺失。
 * 回环引擎把本异常收敛为失败的工具结果回填给模型，而不是抛给业务方。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，参数绑定的实现细节，不属于对外承诺的公开接口
 */
public class ToolArgumentsException extends LuckyRuntimeException {

    public ToolArgumentsException(String message) {
        super(message);
    }

    public ToolArgumentsException(String message, Throwable ex) {
        super(message, ex);
    }
}
