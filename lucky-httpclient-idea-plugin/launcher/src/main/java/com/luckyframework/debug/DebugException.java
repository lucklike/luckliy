package com.luckyframework.debug;

/** 携带协议错误码的受控异常，由 RPC 层转换成错误响应。 */
final class DebugException extends Exception {

    private final String code;
    private final String stack;

    DebugException(String code, String message, String stack) {
        super(message);
        this.code = code;
        this.stack = stack;
    }

    String getCode() {
        return code;
    }

    String getStack() {
        return stack;
    }
}
