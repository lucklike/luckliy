package com.luckyframework.webmvc.webcore;

import com.luckyframework.httpclient.core.RequestMethod;
import com.luckyframework.webmvc.applicationcontext.WebAnnotationScannerApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public abstract class AbstractLuckyServlet extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(AbstractLuckyServlet.class);
    protected WebAnnotationScannerApplicationContext webApplicationContent;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        webApplicationContent = (WebAnnotationScannerApplicationContext) config.getServletContext()
                .getAttribute(LuckyServletContextListener.WEB_APPLICATION_NAME);
        environmentInit(config);
    }


    private void environmentInit(ServletConfig config) {
//        Environment environment = webApplicationContent.getEnvironment();
//        Enumeration<String> initParameterNames = config.getInitParameterNames();
//        while (initParameterNames.hasMoreElements()){
//            String key = initParameterNames.nextElement();
//            environment.setProperty(key,config.getInitParameter(key));
//        }
//        environment.setProperty("web.realPath",config.getServletContext().getRealPath(""));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req,resp,RequestMethod.GET);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req,resp,RequestMethod.HEAD);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req,resp,RequestMethod.POST);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req,resp,RequestMethod.PUT);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req,resp,RequestMethod.DELETE);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req,resp,RequestMethod.OPTIONS);
    }

    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req,resp,RequestMethod.TRACE);
    }

    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp, RequestMethod requestMethod){
        try {
            // 初始化Web上下文
            initWebContext(req, resp, requestMethod);
            // 请求参数信息的处理
            requestInformationProcess();
            // 处理请求得到结果
            ModelAndView modelAndView = doHandleRequest(req, resp);

        } catch (Throwable e){
            // 出现异常时的异常处理
            requestExceptionHandle(e);
        }finally {
            WebContext.clearContext();
        }

    }


    protected abstract ModelAndView doHandleRequest(HttpServletRequest req, HttpServletResponse resp);



    private void requestInformationProcess() {
        requestMethodConversion();
    }


    private void requestExceptionHandle(Throwable e) {
        e.printStackTrace();
    }


    /**
     * 初始化Web上下文
     * @param req Request
     * @param resp Response
     * @param requestMethod 请求方法
     * @throws UnsupportedEncodingException 设置请求和响应编码是出错会抛出该异常
     */
    private void initWebContext(HttpServletRequest req, HttpServletResponse resp, RequestMethod requestMethod) throws UnsupportedEncodingException {
        req.setCharacterEncoding("utf8");
        resp.setCharacterEncoding("utf8");
        resp.setHeader("Content-Type", "text/html;charset=utf-8");
        WebContext webContext = WebContext.createContext();
        webContext.setRequest(req);
        webContext.setResponse(resp);
        webContext.setSession(req.getSession());
        webContext.setApplication(req.getServletContext());
        webContext.setRequestMethod(requestMethod);
        webContext.setServletConfig(getServletConfig());
        WebContext.setContext(webContext);
    }

    /**
     * 请求方法转换,特定条件下可以通过_method参数来改变请求方法
     */
    private void requestMethodConversion() {
        RequestMethod requestMethod = RequestUtils.getRequestMethod();
        if(requestMethod == RequestMethod.POST){
            String methodStr = RequestUtils.getRequestParameter("_method");
            if("GET".equalsIgnoreCase(methodStr)){
                RequestUtils.setRequestMethod(RequestMethod.GET);
            }else if("POST".equalsIgnoreCase(methodStr)){
                RequestUtils.setRequestMethod(RequestMethod.POST);
            }else if("DELETE".equalsIgnoreCase(methodStr)){
                RequestUtils.setRequestMethod(RequestMethod.DELETE);
            }else if("PUT".equalsIgnoreCase(methodStr)){
                RequestUtils.setRequestMethod(RequestMethod.PUT);
            }else if("HEAD".equalsIgnoreCase(methodStr)){
                RequestUtils.setRequestMethod(RequestMethod.HEAD);
            }else if("PATCH".equalsIgnoreCase(methodStr)){
                RequestUtils.setRequestMethod(RequestMethod.PATCH);
            }else if("CONNECT".equalsIgnoreCase(methodStr)){
                RequestUtils.setRequestMethod(RequestMethod.CONNECT);
            }else if("OPTIONS".equalsIgnoreCase(methodStr)){
                RequestUtils.setRequestMethod(RequestMethod.OPTIONS);
            }else if("TRACE".equalsIgnoreCase(methodStr)){
                RequestUtils.setRequestMethod(RequestMethod.TRACE);
            }else{
                logger.debug("Invalid _method parameter value '{}'.",methodStr);
            }
        }
    }
}
