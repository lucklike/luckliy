package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.sse.EventListener;

/**
 * SSE事件监听器配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/10 0:58
 */
public class SseListenerConf {

    private String beanName = "";

    private Class<?> className = EventListener.class;

    private Scope scope;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Class<?> getClassName() {
        return className;
    }

    public void setClassName(Class<?> className) {
        this.className = className;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }
}
