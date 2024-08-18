package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.conversion.TargetField;
import com.luckyframework.httpclient.proxy.creator.Scope;

/**
 * 扩展处理器配置类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/8/18 20:15
 */
public class ExtendHandleConfig<T> {

    @TargetField("bean-name")
    private String beanName = "";

    @TargetField("class-name")
    private Class<T> className;

    private Scope scope = Scope.SINGLETON;


    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Class<T> getClassName() {
        return className;
    }

    public void setClassName(Class<T> className) {
        this.className = className;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }
}
