package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.conversion.TargetField;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.interceptor.Interceptor;
import com.luckyframework.httpclient.proxy.interceptor.PriorityConstant;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/3 00:36
 */
public class InterceptorConf {

//    @TargetField("bean-name")
    private String beanName = "";

//    @TargetField("class-name")
    private Class<?> className = Interceptor.class;

    private Scope scope = Scope.SINGLETON;

    private Integer priority = PriorityConstant.DEFAULT_PRIORITY;


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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
