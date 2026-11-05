package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 流式协议异常：事件流未给出结束信号即终止、单帧结构非法、或结束时刻参数JSON仍不完整时抛出，
 * 异常信息带上中断时已接收的内容与未完成的工具调用（工具名与已拼接的参数文本前缀）。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class StreamProtocolException extends LuckyRuntimeException {

    public StreamProtocolException(String message) {
        super(message);
    }

    public StreamProtocolException(String message, Throwable ex) {
        super(message, ex);
    }
}
