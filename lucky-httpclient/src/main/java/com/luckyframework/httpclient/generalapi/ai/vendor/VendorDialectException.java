package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 方言层异常：发出的报文不满足厂商的必填约束时抛出，用于在请求发出前提前失败。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class VendorDialectException extends LuckyRuntimeException {

    public VendorDialectException(String message) {
        super(message);
    }

    public VendorDialectException(String message, Throwable ex) {
        super(message, ex);
    }
}
