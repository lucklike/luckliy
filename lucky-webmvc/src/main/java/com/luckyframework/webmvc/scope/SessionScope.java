package com.luckyframework.webmvc.scope;

import com.luckyframework.bean.factory.ObjectFactory;
import com.luckyframework.proxy.scope.Scope;
import com.luckyframework.webmvc.webcore.WebContext;

import javax.servlet.http.HttpSession;

public class SessionScope implements Scope {

    @Override
    public Object get(String beanName, ObjectFactory<?> objectFactory) {
        WebContext webContext = WebContext.getCurrentContext();
        if(webContext == null){
            return objectFactory.getObject();
        }else {
            HttpSession session = webContext.getSession();
            Object sessionBean = session.getAttribute(beanName);
            if(sessionBean == null){
                sessionBean = objectFactory.getObject();
                session.setAttribute(beanName,sessionBean);
            }
            return sessionBean;
        }
    }

    @Override
    public Object remove(String beanName) {
        WebContext webContext = WebContext.getCurrentContext();
        if(webContext != null){
            HttpSession session = webContext.getSession();
            Object sessionBean = session.getAttribute(beanName);
            if(sessionBean != null){
                session.removeAttribute(beanName);
            }
            return sessionBean;
        }
        return null;
    }
}
