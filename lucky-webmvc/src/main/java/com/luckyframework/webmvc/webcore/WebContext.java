package com.luckyframework.webmvc.webcore;


import com.luckyframework.httpclient.core.RequestMethod;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Web上下文
 *
 * @author fk7075
 * @version 1.0
 * @date 2020/11/18 11:13
 */
public class WebContext {

    private static final ThreadLocal<WebContext> context = new ThreadLocal<>();

    private HttpServletRequest request = null;
    private HttpServletResponse response = null;
    private ServletContext application = null;
    private HttpSession session = null;
    private RequestMethod requestMethod = null;
    private ServletConfig servletConfig = null;

    public static WebContext getCurrentContext() {
        return context.get();
    }

    public static WebContext createContext() {
        return new WebContext();
    }

    public static void setContext(WebContext context1) {
        context.set(context1);
    }

    public static void clearContext() {
        context.set(null);
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(RequestMethod requestMethod) {
        this.requestMethod = requestMethod;
    }

    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    public void setServletConfig(ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
    }

    public ServletContext getApplication() {
        return application;
    }

    public void setApplication(ServletContext application) {
        this.application = application;
    }

    public HttpSession getSession() {
        return session;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }
}
