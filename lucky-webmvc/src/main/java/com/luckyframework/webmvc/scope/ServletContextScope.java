package com.luckyframework.webmvc.scope;

import com.luckyframework.bean.factory.ObjectFactory;
import com.luckyframework.proxy.scope.Scope;
import com.luckyframework.webmvc.webcore.WebContext;

import javax.servlet.ServletContext;

public class ServletContextScope implements Scope {

    @Override
    public Object get(String beanName, ObjectFactory<?> objectFactory) {
        WebContext webContext = WebContext.getCurrentContext();
        if(webContext == null){
            return objectFactory.getObject();
        }else {
            ServletContext servletContext = webContext.getApplication();
            Object servletContextBean = servletContext.getAttribute(beanName);
            if(servletContextBean == null){
                servletContextBean = objectFactory.getObject();
                servletContext.setAttribute(beanName,servletContextBean);
            }
            return servletContextBean;
        }
    }

    @Override
    public Object remove(String beanName) {
        WebContext webContext = WebContext.getCurrentContext();
        if(webContext != null){
            ServletContext servletContext = webContext.getApplication();
            Object servletContextBean = servletContext.getAttribute(beanName);
            if(servletContextBean != null){
                servletContext.removeAttribute(beanName);
            }
            return servletContextBean;
        }
        return null;
    }
}
