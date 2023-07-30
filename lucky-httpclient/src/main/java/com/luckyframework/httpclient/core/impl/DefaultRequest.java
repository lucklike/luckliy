package com.luckyframework.httpclient.core.impl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.Header;
import com.luckyframework.httpclient.core.HttpHeaderManager;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.RequestMethod;
import com.luckyframework.httpclient.core.RequestParameter;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;

/**
 * 请求的实现类
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 4:05 下午
 */
public class DefaultRequest implements Request {

    private static Integer commonConnectTimeout;
    private static Integer commonReadTimeout;
    private static Integer commonWriterTimeout;
    private static HttpHeaderManager commonHttpHeaderManager;
    private static RequestParameter commonRequestParameter;

    private String urlTemplate;
    private Integer connectTimeout;
    private Integer readTimeout;
    private Integer writerTimeout;
    private final RequestMethod requestMethod;
    private final HttpHeaderManager httpHeaderManager;
    private final RequestParameter requestParameter;

    public DefaultRequest(@NonNull String url,
                          @NonNull RequestMethod requestMethod,
                          @NonNull HttpHeaderManager httpHeaderManager,
                          @NonNull RequestParameter requestParameter
    ) {
        this.urlTemplate = url;
        this.requestMethod = requestMethod;
        this.httpHeaderManager = httpHeaderManager;
        this.requestParameter = requestParameter;
    }

    public DefaultRequest(@NonNull String url,
                          @NonNull RequestMethod requestMethod) {
        this(url, requestMethod, new DefaultHttpHeaderManager(), new DefaultRequestParameter());
    }

    public static void setCommonConnectTimeout(Integer commonConnectTimeout) {
        DefaultRequest.commonConnectTimeout = commonConnectTimeout;
    }

    public static void setCommonReadTimeout(Integer commonReadTimeout) {
        DefaultRequest.commonReadTimeout = commonReadTimeout;
    }

    public static void setCommonWriterTimeout(Integer commonWriterTimeout) {
        DefaultRequest.commonWriterTimeout = commonWriterTimeout;
    }

    public static void setCommonHttpHeaderManager(HttpHeaderManager commonHttpHeaderManager) {
        DefaultRequest.commonHttpHeaderManager = commonHttpHeaderManager;
    }

    public static void setCommonRequestParameter(RequestParameter commonRequestParameter) {
        DefaultRequest.commonRequestParameter = commonRequestParameter;
    }

    public void init() {
        if (commonConnectTimeout != null) {
            this.connectTimeout = commonConnectTimeout;
        }
        if (commonReadTimeout != null) {
            this.readTimeout = commonReadTimeout;
        }
        if (commonWriterTimeout != null) {
            this.writerTimeout = commonWriterTimeout;
        }
        if (commonHttpHeaderManager != null) {
            this.httpHeaderManager.setHeaders(commonHttpHeaderManager.getHeaderMap());
        }
        if (commonRequestParameter != null) {
            this.requestParameter.setRequestParameter(commonRequestParameter.getRequestParameters());
            this.requestParameter.setPathParameter(commonRequestParameter.getPathParameters());
            this.setQueryParameters(commonRequestParameter.getQueryParameters());
            this.setBody(commonRequestParameter.getBody());
        }
    }

    private String getCompleteUrl(String urlTemp) {
        // 填充URL占位符{}
        urlTemp = StringUtils.format(urlTemp, getPathParameters());

        // 将Query参数转化为查询字符串 ?k1=v1&k2=v2
        String paramStr = ((DefaultRequestParameter) requestParameter).getQueryParameterString();

        // 组装完整的URL
        return StringUtils.joinUrlAndParams(urlTemp, paramStr);
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    //--------------------------------------------------------------
    //                        Request Methods
    //--------------------------------------------------------------

    @Override
    public String getUrl() {
        return getCompleteUrl(urlTemplate);
    }

    @Override
    public RequestMethod getRequestMethod() {
        return this.requestMethod;
    }

    @Override
    public HttpHeaderManager getHeaderManager() {
        return this.httpHeaderManager;
    }

    @Override
    public RequestParameter getRequestParameter() {
        return this.requestParameter;
    }

    @Override
    public Integer getConnectTimeout() {
        return this.connectTimeout;
    }

    @Override
    public void setConnectTimeout(Integer connectionTime) {
        this.connectTimeout = connectionTime;
    }

    @Override
    public Integer getReadTimeout() {
        return this.readTimeout;
    }

    @Override
    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public Integer getWriterTimeout() {
        return this.writerTimeout;
    }

    @Override
    public void setWriterTimeout(Integer writerTimeout) {
        this.writerTimeout = writerTimeout;
    }


    //--------------------------------------------------------------
    //                  RequestHeader Methods
    //--------------------------------------------------------------

    @Override
    public void addHeader(String name, Object header) {
        this.httpHeaderManager.addHeader(name, header);
    }

    @Override
    public void setHeader(String name, Object header) {
        this.httpHeaderManager.setHeader(name, header);
    }

    @Override
    public void putHeader(String name, Object header) {
        this.httpHeaderManager.putHeader(name, header);
    }

    @Override
    public void setHeaders(Map<String, List<Header>> headers) {
        this.httpHeaderManager.setHeaders(headers);
    }

    @Override
    public List<Header> getHeader(String name) {
        return this.httpHeaderManager.getHeader(name);
    }

    @Override
    public void removerHeader(String name) {
        this.httpHeaderManager.removerHeader(name);
    }

    @Override
    public void removerFirstHeader(String name) {
        this.httpHeaderManager.removerFirstHeader(name);
    }

    @Override
    public void removerLastHeader(String name) {
        this.httpHeaderManager.removerLastHeader(name);
    }

    @Override
    public void removerHeader(String name, int index) {
        this.httpHeaderManager.removerHeader(name, index);
    }

    @Override
    public Map<String, List<Header>> getHeaderMap() {
        return this.httpHeaderManager.getHeaderMap();
    }


    //--------------------------------------------------------------
    //                  RequestParameter Methods
    //--------------------------------------------------------------

    @Override
    public Map<String, Object> getRequestParameters() {
        return this.requestParameter.getRequestParameters();
    }

    @Override
    public Map<String, Object> getPathParameters() {
        return this.requestParameter.getPathParameters();
    }

    @Override
    public Map<String, List<Object>> getQueryParameters() {
        return this.requestParameter.getQueryParameters();
    }

    @Override
    public void setBody(BodyObject body) {
        this.requestParameter.setBody(body);
        setContentType(body.getContentType());
    }

    @Override
    public BodyObject getBody() {
        return this.requestParameter.getBody();
    }

    @Override
    public void addPathParameter(String name, Object value) {
        this.requestParameter.addPathParameter(name, value);
    }

    @Override
    public void setPathParameter(Map<String, Object> pathParamMap) {
        this.requestParameter.setPathParameter(pathParamMap);
    }

    @Override
    public void addRequestParameter(String name, Object value) {
        this.requestParameter.addRequestParameter(name, value);
    }

    @Override
    public void setRequestParameter(Map<String, Object> requestParamMap) {
        this.requestParameter.setRequestParameter(requestParamMap);
    }

    @Override
    public void addQueryParameter(String name, Object value) {
        this.requestParameter.addQueryParameter(name, value);
    }

    @Override
    public void setQueryParameter(String name, Object value) {
        this.requestParameter.setQueryParameter(name, value);
    }

    @Override
    public void setQueryParameters(Map<String, List<Object>> queryParameters) {
        this.requestParameter.setQueryParameters(queryParameters);
    }

    @Override
    public void removerRequestParameter(String name) {
        this.requestParameter.removerRequestParameter(name);
    }

    @Override
    public void removerPathParameter(String name) {
        this.requestParameter.removerPathParameter(name);
    }

    @Override
    public void removerQueryParameter(String name) {
        this.requestParameter.removerQueryParameter(name);
    }

    @Override
    public void removerQueryParameter(String name, int index) {
        this.requestParameter.removerQueryParameter(name, index);
    }

    @Override
    public String toString() {
        String temp = "URL: {{0}}; {1}; {2}";
        return StringUtils.format(temp, urlTemplate, httpHeaderManager, requestParameter);
    }
}
