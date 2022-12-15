package com.luckyframework.webmvc.scope;

import com.luckyframework.bean.factory.ObjectFactory;
import com.luckyframework.proxy.scope.Scope;
import com.luckyframework.webmvc.webcore.WebContext;

import javax.servlet.http.HttpServletRequest;

public class RequestScope implements Scope {

    @Override
    public Object get(String beanName, ObjectFactory<?> objectFactory) {
        WebContext webContext = WebContext.getCurrentContext();
        if(webContext == null){
            return objectFactory.getObject();
        }else {
            HttpServletRequest request = webContext.getRequest();
            Object requestBean = request.getAttribute(beanName);
            if(requestBean == null){
                requestBean = objectFactory.getObject();
                request.setAttribute(beanName,requestBean);
            }
            return requestBean;
        }
    }

    @Override
    public Object remove(String beanName) {
        WebContext webContext = WebContext.getCurrentContext();
        if(webContext != null){
            HttpServletRequest request = webContext.getRequest();
            Object requestBean = request.getAttribute(beanName);
            if(requestBean != null){
                request.removeAttribute(beanName);
            }
            return requestBean;
        }
        return null;
    }
}
