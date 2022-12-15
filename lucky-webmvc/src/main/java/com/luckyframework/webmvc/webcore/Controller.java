package com.luckyframework.webmvc.webcore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 处理请求的逻辑接口
 * @author FK7075
 * @version 1.0.0
 * @date 2022/6/1 17:45
 */
public interface Controller {

    ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception;

}
