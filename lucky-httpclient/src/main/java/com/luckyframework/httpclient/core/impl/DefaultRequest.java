package com.luckyframework.httpclient.core.impl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.ContentType;
import com.luckyframework.httpclient.core.Header;
import com.luckyframework.httpclient.core.HttpFile;
import com.luckyframework.httpclient.core.HttpHeaderManager;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.RequestMethod;
import com.luckyframework.httpclient.core.RequestParameter;
import com.luckyframework.io.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import java.io.File;
import java.io.InputStream;
import java.net.Proxy;
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
    private static Proxy commonProxy;

    private String urlTemplate;
    private Integer connectTimeout;
    private Integer readTimeout;
    private Integer writerTimeout;
    private Proxy proxy;
    private RequestMethod requestMethod;
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

    public static Proxy getCommonProxy() {
        return commonProxy;
    }

    public static void setCommonProxy(Proxy commonProxy) {
        DefaultRequest.commonProxy = commonProxy;
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
        if (commonProxy != null) {
            this.proxy = commonProxy;
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
    public DefaultRequest setRequestMethod(RequestMethod requestMethod) {
        this.requestMethod = requestMethod;
        return this;
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
    public DefaultRequest setConnectTimeout(Integer connectionTime) {
        this.connectTimeout = connectionTime;
        return this;
    }

    @Override
    public Integer getReadTimeout() {
        return this.readTimeout;
    }

    @Override
    public DefaultRequest setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    @Override
    public Integer getWriterTimeout() {
        return this.writerTimeout;
    }

    @Override
    public DefaultRequest setWriterTimeout(Integer writerTimeout) {
        this.writerTimeout = writerTimeout;
        return this;
    }

    @Override
    public DefaultRequest setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    @Override
    public DefaultRequest setProxy(String ip, int port) {
        Request.super.setProxy(ip, port);
        return this;
    }

    @Override
    public Proxy getProxy() {
        return this.proxy;
    }

    @Override
    public DefaultRequest addCookie(String name, String value) {
        Request.super.addCookie(name, value);
        return this;
    }

    @Override
    public DefaultRequest removeCookie(String name) {
        Request.super.removeCookie(name);
        return this;
    }

    //--------------------------------------------------------------
    //                  RequestHeader Methods
    //--------------------------------------------------------------

    @Override
    public DefaultRequest addHeader(String name, Object header) {
        this.httpHeaderManager.addHeader(name, header);
        return this;
    }

    @Override
    public DefaultRequest setHeader(String name, Object header) {
        this.httpHeaderManager.setHeader(name, header);
        return this;
    }

    @Override
    public DefaultRequest putHeader(String name, Object header) {
        this.httpHeaderManager.putHeader(name, header);
        return this;
    }

    @Override
    public DefaultRequest setHeaders(Map<String, List<Header>> headers) {
        this.httpHeaderManager.setHeaders(headers);
        return this;
    }

    @Override
    public List<Header> getHeader(String name) {
        return this.httpHeaderManager.getHeader(name);
    }

    @Override
    public DefaultRequest removerHeader(String name) {
        this.httpHeaderManager.removerHeader(name);
        return this;
    }

    @Override
    public DefaultRequest removerFirstHeader(String name) {
        this.httpHeaderManager.removerFirstHeader(name);
        return this;
    }

    @Override
    public DefaultRequest removerLastHeader(String name) {
        this.httpHeaderManager.removerLastHeader(name);
        return this;
    }

    @Override
    public DefaultRequest removerHeader(String name, int index) {
        this.httpHeaderManager.removerHeader(name, index);
        return this;
    }

    @Override
    public Map<String, List<Header>> getHeaderMap() {
        return this.httpHeaderManager.getHeaderMap();
    }

    @Override
    public DefaultRequest setContentType(String contentType) {
        Request.super.setContentType(contentType);
        return this;
    }

    @Override
    public DefaultRequest setContentType(ContentType contentType) {
        Request.super.setContentType(contentType);
        return this;
    }

    @Override
    public DefaultRequest setAuthorization(String username, String password) {
        Request.super.setAuthorization(username, password);
        return this;
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
    public DefaultRequest setBody(BodyObject body) {
        this.requestParameter.setBody(body);
        setContentType(body.getContentType());
        return this;
    }

    @Override
    public BodyObject getBody() {
        return this.requestParameter.getBody();
    }

    @Override
    public DefaultRequest addPathParameter(String name, Object value) {
        this.requestParameter.addPathParameter(name, value);
        return this;
    }

    @Override
    public DefaultRequest setPathParameter(Map<String, Object> pathParamMap) {
        this.requestParameter.setPathParameter(pathParamMap);
        return this;
    }

    @Override
    public DefaultRequest addRequestParameter(String name, Object value) {
        this.requestParameter.addRequestParameter(name, value);
        return this;
    }

    @Override
    public DefaultRequest setRequestParameter(Map<String, Object> requestParamMap) {
        this.requestParameter.setRequestParameter(requestParamMap);
        return this;
    }

    @Override
    public DefaultRequest addQueryParameter(String name, Object value) {
        this.requestParameter.addQueryParameter(name, value);
        return this;
    }

    @Override
    public DefaultRequest setQueryParameter(String name, Object value) {
        this.requestParameter.setQueryParameter(name, value);
        return this;
    }

    @Override
    public DefaultRequest setQueryParameters(Map<String, List<Object>> queryParameters) {
        this.requestParameter.setQueryParameters(queryParameters);
        return this;
    }

    @Override
    public DefaultRequest removerRequestParameter(String name) {
        this.requestParameter.removerRequestParameter(name);
        return this;
    }

    @Override
    public DefaultRequest removerPathParameter(String name) {
        this.requestParameter.removerPathParameter(name);
        return this;
    }

    @Override
    public DefaultRequest removerQueryParameter(String name) {
        this.requestParameter.removerQueryParameter(name);
        return this;
    }

    @Override
    public DefaultRequest removerQueryParameter(String name, int index) {
        this.requestParameter.removerQueryParameter(name, index);
        return this;
    }

    @Override
    public DefaultRequest addFormParameter(String name, Object value) {
        Request.super.addFormParameter(name, value);
        return this;
    }

    @Override
    public DefaultRequest addHttpFiles(String name, HttpFile... httpFiles) {
        Request.super.addHttpFiles(name, httpFiles);
        return this;
    }

    @Override
    public DefaultRequest addInputStream(String name, String fileName, InputStream inputStream) {
        Request.super.addInputStream(name, fileName, inputStream);
        return this;
    }

    @Override
    public DefaultRequest addFiles(String name, File... files) {
        Request.super.addFiles(name, files);
        return this;
    }

    @Override
    public DefaultRequest addFiles(String name, String... filePaths) {
        Request.super.addFiles(name, filePaths);
        return this;
    }

    @Override
    public DefaultRequest addResources(String name, Resource... resources) {
        Request.super.addResources(name, resources);
        return this;
    }

    @Override
    public DefaultRequest addResources(String name, String... resourcePaths) {
        Request.super.addResources(name, resourcePaths);
        return this;
    }

    @Override
    public DefaultRequest addMultipartFiles(String name, MultipartFile... multipartFiles) {
        Request.super.addMultipartFiles(name, multipartFiles);
        return this;
    }

    @Override
    public DefaultRequest setJsonBody(Object jsonBody) {
        Request.super.setJsonBody(jsonBody);
        return this;
    }

    @Override
    public DefaultRequest setJsonBody(String jsonBodyString) {
        Request.super.setJsonBody(jsonBodyString);
        return this;
    }

    @Override
    public DefaultRequest setXmlBody(Object xmlBody) {
        Request.super.setXmlBody(xmlBody);
        return this;
    }

    @Override
    public DefaultRequest setXmlBody(String xmlBodyString) {
        Request.super.setXmlBody(xmlBodyString);
        return this;
    }

    @Override
    public String toString() {
        String temp = "URL: {{0}{1}}; {2}; {3}";
        String proxyStr = this.proxy == null ? "" : ", PROXY: " + this.proxy;
        return StringUtils.format(temp, urlTemplate, proxyStr, httpHeaderManager, requestParameter);
    }
}
