package com.luckyframework.httpclient.proxy.logging.support;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Log4j2 内存日志收集Appender
 * <pre>
 * 将日志事件的消息文本收集到内存列表中，供测试用例对真实的日志输出进行断言。
 * 挂载在 {@code BeautifulLoggerPrintHandler} 对应的Logger上时，收集到的是脱敏处理后的
 * 最终日志文本（含ANSI着色、JSON美化等真实的日志后处理链路），
 * 可用脱敏结果子串（如 138****5678）直接断言。
 * </pre>
 */
public class LogMemoryAppender extends AbstractAppender {

    /**
     * 已收集的日志消息
     */
    private final List<String> messages = new CopyOnWriteArrayList<>();

    public LogMemoryAppender(String name) {
        super(name, null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
        messages.add(event.getMessage().getFormattedMessage());
    }

    /**
     * 获取全部日志消息
     */
    public List<String> getMessages() {
        return messages;
    }

    /**
     * 清空已收集的日志消息
     */
    public void clear() {
        messages.clear();
    }

    /**
     * 拼接包含指定关键字的所有日志消息
     *
     * @param keyword 过滤关键字
     * @return 包含该关键字的所有日志文本（以换行分隔）
     */
    public String textContaining(String keyword) {
        StringBuilder builder = new StringBuilder();
        for (String message : messages) {
            if (message.contains(keyword)) {
                builder.append(message).append('\n');
            }
        }
        return builder.toString();
    }
}
