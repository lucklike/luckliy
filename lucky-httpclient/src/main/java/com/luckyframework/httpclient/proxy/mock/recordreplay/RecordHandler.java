package com.luckyframework.httpclient.proxy.mock.recordreplay;


import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 记录处理器，用于保存或读取记录
 * https://www.hae67.cc:5200/vod/details/be449c5da759451b
 *
 * @author fk7075
 * @version 3.0.3
 * @since 2026-07-30 01:02:16
 */
public interface RecordHandler {

    /**
     * 保存记录
     *
     * @param mc     方法上下文
     * @param record 记录实体
     */
    void save(MethodContext mc, Record record);

    /**
     * 加载记录
     *
     * @param mc 方法上下文
     * @param id 记录 ID
     * @return 记录实体
     */
    Record load(MethodContext mc, String id);
}
