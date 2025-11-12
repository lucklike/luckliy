package com.luckyframework.httpclient.core.executor;

import com.luckyframework.httpclient.core.meta.Request;

public class Constant {
    public static final String HTTP_CLIENT_CONTEXT_REQUEST = "__REQUEST__";
    public static final Integer DEFAULT_CONNECTION_REQUEST_TIMEOUT = 3 * 1000;
    public static final Integer DEFAULT_CONNECTION_TIMEOUT = Request.DEF_CONNECTION_TIME_OUT;
    public static final Integer DEFAULT_RESPONSE_TIMEOUT = Request.DEF_READ_TIME_OUT;
    public static final Integer DEFAULT_VALIDATE_AFTER_INACTIVITY = 30 * 1000;
    public static final Integer DEFAULT_MAX_TOTAL = 10;
    public static final Integer DEFAULT_MAX_PER_ROUTE = 50;
    public static final Integer DEFAULT_KEEP_ALIVE_DURATION = 5;

    public static final Integer DEFAULT_READ_TIMEOUT = DEFAULT_RESPONSE_TIMEOUT;
    public static final Integer DEFAULT_WRITE_TIMEOUT = DEFAULT_RESPONSE_TIMEOUT;
    public static final Integer DEFAULT_CALL_TIMEOUT = 100 * 1000;
    public static final Integer DEFAULT_MAX_IDLE_CONNECTIONS = DEFAULT_MAX_TOTAL;
}
