package com.luckyframework.webmvc.scope;

import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.webmvc.WebBeanScope;

public final class WebScopeBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory) {
        listableBeanFactory.registerScope(WebBeanScope.REQUEST,new RequestScope());
        listableBeanFactory.registerScope(WebBeanScope.SESSION,new SessionScope());
        listableBeanFactory.registerScope(WebBeanScope.SERVLET_CONTEXT,new ServletContextScope());
    }
}
