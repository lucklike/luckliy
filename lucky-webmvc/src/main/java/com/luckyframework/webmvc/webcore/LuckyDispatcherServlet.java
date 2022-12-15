package com.luckyframework.webmvc.webcore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LuckyDispatcherServlet extends AbstractLuckyServlet {

    private final static Logger logger = LoggerFactory.getLogger(LuckyDispatcherServlet.class);

    @Override
    protected ModelAndView doHandleRequest(HttpServletRequest req, HttpServletResponse resp) {
        return findControllerAndHandle(req, resp);
    }

    private ModelAndView findControllerAndHandle(HttpServletRequest req, HttpServletResponse resp){
        return null;
    }


}
