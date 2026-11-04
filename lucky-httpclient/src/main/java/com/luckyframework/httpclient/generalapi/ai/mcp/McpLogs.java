package com.luckyframework.httpclient.generalapi.ai.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP模块的日志出口：只输出可诊断的协议信息，凭据值一律不落地（HTTP侧由框架的按名脱敏链路负责）。
 *
 * @author fukang
 * @version 3.0.3
 * @apiNote 内部API
 */
final class McpLogs {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpLogs.class);

    private McpLogs() {
    }

    static void warn(String template, Object... args) {
        LOGGER.warn(template, args);
    }

    static void info(String template, Object... args) {
        LOGGER.info(template, args);
    }
}
