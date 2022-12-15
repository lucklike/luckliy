package com.luckyframework.httpclient.core.impl;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.RequestParameter;

import java.util.*;

public class DefaultRequestParameter implements RequestParameter {

    private final static Map<String,Object> EMPTY_MAP = new HashMap<>();

    /** 请求的参数*/
    private final Map<String,Object> requestParams = new LinkedHashMap<>();
    /** rest风格参数，在URL中的存在的参数*/
    private final Map<String,Object> restParams = new LinkedHashMap<>();
    /** rest风格参数，在URL中的存在的参数*/
    private final Map<String,List<Object>> urlParams = new LinkedHashMap<>();
    /** 放在请求体中的参数*/
    private BodyObject bodyParameter;


    @Override
    public Map<String, Object> getRequestParameters() {
        return this.requestParams;
    }

    @Override
    public Map<String, Object> getRestParameters() {
        return this.restParams;
    }

    @Override
    public Map<String, List<Object>> getUrlParameters() {
        return this.urlParams;
    }

    @Override
    public void setBody(BodyObject body) {
        this.bodyParameter = body;
    }

    @Override
    public BodyObject getBody() {
        return this.bodyParameter;
    }

    @Override
    public void addRestParameter(String name, Object value) {
        this.restParams.put(name, value);
    }

    @Override
    public void setRestParameter(Map<String, Object> restParamMap) {
        restParamMap = restParamMap == null ? EMPTY_MAP : restParamMap;
        this.restParams.putAll(restParamMap);
    }

    @Override
    public void addRequestParameter(String name, Object value) {
        this.requestParams.put(name, value);
    }

    @Override
    public void setRequestParameter(Map<String, Object> requestParamMap) {
        requestParamMap = requestParamMap == null ? EMPTY_MAP : requestParamMap;
        this.requestParams.putAll(requestParamMap);
    }

    @Override
    public void addUrlParameter(String name, Object value) {
        List<Object> valueList = urlParams.get(name);
        if(valueList == null){
            valueList = new LinkedList<>();
            valueList.add(value);
            urlParams.put(name,valueList);
        }else{
            valueList.add(value);
        }
    }

    @Override
    public void setUrlParameter(String name, Object value) {
        List<Object> valueList = new LinkedList<>();
        valueList.add(value);
        urlParams.put(name,valueList);
    }

    @Override
    public void removerRequestParameter(String name) {
        this.requestParams.remove(name);
    }

    @Override
    public void removerRestParameter(String name) {
        this.restParams.remove(name);
    }

    @Override
    public void removerUrlParameter(String name) {
        this.urlParams.remove(name);
    }

    @Override
    public void removerUrlParameter(String name, int index) {
        List<Object> valueList = urlParams.get(name);
        if(!ContainerUtils.isEmptyCollection(valueList)){
            valueList.remove(index);
        }
    }

    public String  requestParamsToString(){
        StringBuilder sb = new StringBuilder("PARAMETERS:{");
        for (Map.Entry<String, Object> entry : requestParams.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if(value == null){
                sb.append(name).append("=,");
            }else{
                sb.append(name).append("=").append(value).append(",");
            }
        }
        String ss = sb.toString();
        ss = ss.endsWith(",")?ss.substring(0,ss.length()-1):ss;
        ss = ss+"}";
        return ss;
    }
}
