package com.luckyframework.httpclient.generalapi.ai.mcp;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * JSON-RPC请求标识生成器：进程内自增，用于请求-响应配对。
 *
 * @author fukang
 * @version 3.0.3
 * @apiNote 内部API
 */
public final class JsonRpcIdGenerator {

    private final AtomicInteger sequence = new AtomicInteger();

    /**
     * 取下一个请求标识，从1开始（0容易与「未赋值」混淆）
     *
     * @return 请求标识
     */
    public int next() {
        return sequence.incrementAndGet();
    }
}
