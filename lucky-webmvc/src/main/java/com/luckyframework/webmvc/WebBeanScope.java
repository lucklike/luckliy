package com.luckyframework.webmvc;

import com.luckyframework.proxy.scope.BeanScope;

/**
 * WebBean的作用域扩展
 */
public class WebBeanScope extends BeanScope {

    /** Request单例*/
    public final static String REQUEST              = "request";
    /** Session单例*/
    public final static String SESSION              = "session";
    /** Application单例*/
    public final static String SERVLET_CONTEXT      = "servletContext";

}
