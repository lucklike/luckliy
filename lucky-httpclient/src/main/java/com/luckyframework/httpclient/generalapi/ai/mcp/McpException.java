package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * MCP客户端异常：握手未完成、传输断开、请求超时、JSON-RPC错误响应等不可恢复的协议层问题。
 * 「远端工具执行失败」不走本异常，而是转成失败的工具结果交回环回填。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class McpException extends LuckyRuntimeException {

    public McpException(String message) {
        super(message);
    }

    public McpException(Throwable ex, String message) {
        super(ex, message);
    }

    /**
     * 会话未初始化（握手前就发起工具相关请求）
     */
    public static McpException notInitialized(String method) {
        return new McpException("The MCP session is not initialized, refuse to send '" + method
                + "'. 请先完成 initialize 握手（call McpClient#initialize()）。");
    }

    /**
     * 单次请求超时
     */
    public static McpException timeout(String method, int id, long timeoutMillis) {
        return new McpException("The MCP request '" + method + "' (id=" + id + ") timed out after "
                + timeoutMillis + " ms（请求超时无响应）.");
    }

    /**
     * 连接已断开
     */
    public static McpException disconnected(Throwable cause) {
        return new McpException(cause, "The MCP transport connection has been closed（连接已断开）: " + cause);
    }
}
