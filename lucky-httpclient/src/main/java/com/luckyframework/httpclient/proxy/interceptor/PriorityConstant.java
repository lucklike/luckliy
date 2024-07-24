package com.luckyframework.httpclient.proxy.interceptor;

public class PriorityConstant {

    public static final int DEFAULT_PRIORITY = Integer.MAX_VALUE;

    public static final int REDIRECT_PRIORITY = 0;

    public static final int COOKIE_MANAGE_PRIORITY = 10000;

    public static final int CONFIG_API_PRIORITY = -10000;
}
