package com.luckyframework.httpclient.core.impl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.*;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;

/**
 * 请求的实现类
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 4:05 下午
 */
 public class DefaultRequest implements Request {

    private String url;
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
        this.url = url;
        this.requestMethod = requestMethod;
        this.httpHeaderManager = httpHeaderManager;
        this.requestParameter = requestParameter;
    }

    public DefaultRequest(@NonNull String url,
                          @NonNull RequestMethod requestMethod){
        this(url,requestMethod,new DefaultHttpHeaderManager(),new DefaultRequestParameter());
    }

    private void urlSetting() {
        // 1.填充Rest占位符处的参数 "{xxx}" -> http://localhost:80/Lucky/{project}?name={username}
        Map<String, Object> restParameters = getRestParameters();
        url = StringUtils.format(url,restParameters);
        url = url.endsWith("/")?url.substring(0,url.length()-1):url;

        // 2.拼接URL参数 http://localhost:80/Lucky + ?name=Jack&opId=21koknscsa-2132
        Map<String, List<Object>> urlParameters = getUrlParameters();
        StringBuilder paramSb = new StringBuilder();
        for (Map.Entry<String, List<Object>> entry : urlParameters.entrySet()) {
            String name = entry.getKey();
            List<Object> valueList = entry.getValue();
            if(ContainerUtils.isEmptyCollection(valueList)){
                paramSb.append(name).append("=&");
            }else{
                for (Object value : valueList) {
                    paramSb.append(name).append("=").append(value.toString()).append("&");
                }
            }
        }
        String paramStr = paramSb.toString();
        paramStr = paramStr.endsWith("&")?paramStr.substring(0,paramStr.length()-1):paramStr;
        if(org.springframework.util.StringUtils.hasText(paramStr)){
            if(url.endsWith("&")){
                url += paramStr;
            }
            else if(url.contains("?")){
                url = url+"&"+paramStr;
            }
            else{
                url = url+"?"+paramStr;
            }
        }
    }

    //--------------------------------------------------------------
    //                        Request Methods
    //--------------------------------------------------------------

    @Override
    public String getUrl() {
        urlSetting();
        return url;
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
    public Map<String,List<Header>> getHeaderMap() {
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
    public Map<String, Object> getRestParameters() {
        return this.requestParameter.getRestParameters();
    }

    @Override
    public Map<String, List<Object>> getUrlParameters() {
        return this.requestParameter.getUrlParameters();
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
    public void addRestParameter(String name, Object value) {
        this.requestParameter.addRestParameter(name, value);
    }

    @Override
    public void setRestParameter(Map<String, Object> restParamMap) {
        this.requestParameter.setRestParameter(restParamMap);
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
    public void addUrlParameter(String name, Object value) {
        this.requestParameter.addUrlParameter(name, value);
    }

    @Override
    public void setUrlParameter(String name, Object value) {
        this.requestParameter.setUrlParameter(name, value);
    }

    @Override
    public void removerRequestParameter(String name) {
        this.requestParameter.removerRequestParameter(name);
    }

    @Override
    public void removerRestParameter(String name) {
        this.requestParameter.removerRestParameter(name);
    }

    @Override
    public void removerUrlParameter(String name) {
        this.requestParameter.removerUrlParameter(name);
    }

    @Override
    public void removerUrlParameter(String name, int index) {
        this.requestParameter.removerUrlParameter(name, index);
    }

    @Override
    public String toString() {
        String temp = "[{0}] {1}; {2}{3}";
        String requestParametersStr = "";
        if(requestParameter instanceof DefaultRequestParameter){
            requestParametersStr = "; "+((DefaultRequestParameter)requestParameter).requestParamsToString();
        }
        return StringUtils.format(temp,url, httpHeaderManager,requestParametersStr);
    }
}
