package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.conversion.TargetField;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.creator.Scope;

/**
 * SSE事件监听器配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/10 0:58
 */
public class HttpExecutorConf {

    @TargetField("bean-name")
    private String beanName = "";

    @TargetField("class-name")
    private Class<?> className = HttpExecutor.class;

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
