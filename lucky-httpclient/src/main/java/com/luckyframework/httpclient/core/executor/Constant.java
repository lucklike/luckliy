package com.luckyframework.httpclient.core.executor;

public class Constant {
    public static final String HTTP_CLIENT_CONTEXT_REQUEST = "__REQUEST__";
    public static final Integer DEFAULT_CONNECTION_REQUEST_TIMEOUT = 3 * 1000;
    public static final Integer DEFAULT_CONNECTION_TIMEOUT = 5 * 1000;
    public static final Integer DEFAULT_RESPONSE_TIMEOUT = 30 * 1000;
    public static final Integer DEFAULT_VALIDATE_AFTER_INACTIVITY = 30 * 1000;
    public static final Integer DEFAULT_MAX_TOTAL = 200;
    public static final Integer DEFAULT_MAX_PER_ROUTE = 50;
    public static final Integer DEFAULT_KEEP_ALIVE_DURATION = 2;

    public static final Integer DEFAULT_READ_TIMEOUT = DEFAULT_RESPONSE_TIMEOUT;
    public static final Integer DEFAULT_WRITE_TIMEOUT = DEFAULT_RESPONSE_TIMEOUT;
    public static final Integer DEFAULT_CALL_TIMEOUT = 100 * 1000;
    public static final Integer DEFAULT_MAX_IDLE_CONNECTIONS = 50;


    public static final String OKHTTP_PM_WRITE_TIMEOUT = "OKHTTP#PM:WRITE_TIMEOUT";
    public static final String OKHTTP_PM_CALL_TIMEOUT = "OKHTTP#PM:CALL_TIMEOUT";

    public static final String HTTPCLIENT_PM_CONNECTION_REQUEST_TIMEOUT = "HTTPCLIENT#PM:CONNECTION_REQUEST_TIMEOUT";
}
