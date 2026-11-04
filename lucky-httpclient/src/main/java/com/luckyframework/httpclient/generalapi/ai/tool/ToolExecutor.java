package com.luckyframework.httpclient.generalapi.ai.tool;

import java.util.Map;

/**
 * 工具执行器：入参为按工具声明的JSON Schema解析出的参数集合，出参为可序列化的对象。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@FunctionalInterface
public interface ToolExecutor {

    /**
     * 执行工具
     *
     * @param args 按声明Schema解析出的参数集合
     * @return 工具返回值，由回环引擎序列化为工具结果内容
     * @throws Throwable 执行过程中的任何异常，由回环引擎按失败策略处理
     */
    Object execute(Map<String, Object> args) throws Throwable;
}
