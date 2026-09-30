package com.luckyframework.httpclient.proxy.mock.recordreplay;


import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 记录 ID 生成器
 *
 * @author fk7075
 * @version 3.0.3
 * @since 2026-07-30 01:20:27
 */
public interface RecordIdGenerator {

    /**
     * 生成记录的唯一 ID
     *
     * @param mc 方法上下文
     * @return 记录的唯一 ID
     */
    String generateId(MethodContext mc);
}
