package com.luckyframework.httpclient.core.meta;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import org.springframework.util.Assert;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DefaultRequestParameter implements RequestParameter {

    private final static Map<String, Object> EMPTY_MAP = new HashMap<>();

    /**
     * 表单参数
     */
    private final Map<String, Object> formParams = new LinkedHashMap<>();
    /**
     * rest风格参数，URL路径上的参数
     */
    private final Map<String, Object> pathParams = new LinkedHashMap<>();

    /**
     * rest风格参数，URL中使用?和&拼接的参数
     */
    private final Map<String, List<Object>> queryParams = new LinkedHashMap<>();

    /**
     * multipart/from-data参数
     */
    private final Map<String, Object> multipartParams = new LinkedHashMap<>();

    /**
     * 请求体参数
     */
    private BodyObject bodyParameter;

    /**
     * 请求体参数工厂
     */
    private BodyObjectFactory bodyObjectFactory;

    public DefaultRequestParameter() {

    }

    public DefaultRequestParameter(DefaultRequestParameter parameter) {
        this.formParams.putAll(parameter.formParams);
        this.pathParams.putAll(parameter.pathParams);
        this.multipartParams.putAll(parameter.multipartParams);
        this.bodyParameter = parameter.bodyParameter;
        this.bodyObjectFactory = parameter.bodyObjectFactory;

        parameter.queryParams.forEach((k, v) -> {
            this.queryParams.put(k, new LinkedList<>(v));
        });
    }


    @Override
    public Map<String, Object> getFormParameters() {
        return this.formParams;
    }

    @Override
    public Map<String, Object> getPathParameters() {
        return this.pathParams;
    }

    @Override
    public Map<String, List<Object>> getQueryParameters() {
        return this.queryParams;
    }

    @Override
    public Map<String, Object> getMultipartFormParameters() {
        return this.multipartParams;
    }

    @Override
    public DefaultRequestParameter setBody(BodyObject body) {
        this.bodyParameter = body;
        return this;
    }

    @Override
    public BodyObject getBody() {
        if (this.bodyObjectFactory != null) {
            this.bodyParameter = this.bodyObjectFactory.create();
        }
        return this.bodyParameter;
    }

    @Override
    public DefaultRequestParameter setBodyFactory(BodyObjectFactory factory) {
        this.bodyObjectFactory = factory;
        return this;
    }

    @Override
    public BodyObjectFactory getBodyFactory() {
        return this.bodyObjectFactory;
    }

    @Override
    public DefaultRequestParameter addPathParameter(String name, Object value) {
        this.pathParams.put(name, value);
        return this;
    }

    @Override
    public DefaultRequestParameter setPathParameter(Map<String, Object> pathParamMap) {
        pathParamMap = pathParamMap == null ? EMPTY_MAP : pathParamMap;
        this.pathParams.putAll(pathParamMap);
        return this;
    }

    @Override
    public RequestParameter addFormParameter(String name, Object value) {
        Assert.notNull(value, "from parameter cannot be null.");
        this.formParams.put(name, value);
        return this;
    }

    @Override
    public RequestParameter setFormParameter(Map<String, Object> requestParamMap) {
        requestParamMap = requestParamMap == null ? EMPTY_MAP : requestParamMap;
        this.formParams.putAll(requestParamMap);
        return this;
    }

    @Override
    public RequestParameter addMultipartFormParameter(String name, Object value) {
        Assert.notNull(value, "multipart from data parameter cannot be null.");
        this.multipartParams.put(name, value);
        return this;
    }

    @Override
    public RequestParameter setMultipartFormParameter(Map<String, Object> requestParamMap) {
        requestParamMap = requestParamMap == null ? EMPTY_MAP : requestParamMap;
        this.multipartParams.putAll(requestParamMap);
        return this;
    }


    @Override
    public DefaultRequestParameter addQueryParameter(String name, Object value) {
        List<Object> valueList = queryParams.get(name);
        if (valueList == null) {
            valueList = new LinkedList<>();
            valueList.add(value);
            queryParams.put(name, valueList);
        } else {
            valueList.add(value);
        }
        return this;
    }

    @Override
    public DefaultRequestParameter setQueryParameter(String name, Object value) {
        List<Object> valueList = new LinkedList<>();
        valueList.add(value);
        queryParams.put(name, valueList);
        return this;
    }

    @Override
    public DefaultRequestParameter setQueryParameters(Map<String, List<Object>> queryParameters) {
        queryParameters.forEach((k, v) -> queryParams.put(k, new LinkedList<>(v)));
        return this;
    }

    @Override
    public DefaultRequestParameter removerFormParameter(String name) {
        this.formParams.remove(name);
        return this;
    }

    @Override
    public DefaultRequestParameter removerMultipartFormParameter(String name) {
        this.multipartParams.remove(name);
        return this;
    }

    @Override
    public DefaultRequestParameter removerPathParameter(String name) {
        this.pathParams.remove(name);
        return this;
    }

    @Override
    public DefaultRequestParameter removerQueryParameter(String name) {
        this.queryParams.remove(name);
        return this;
    }

    @Override
    public DefaultRequestParameter removerQueryParameter(String name, int index) {
        List<Object> valueList = queryParams.get(name);
        if (!ContainerUtils.isEmptyCollection(valueList)) {
            valueList.remove(index);
        }
        return this;
    }

    public String getQueryParameterString() {
        StringBuilder queryParamBuilder = new StringBuilder();
        Map<String, List<Object>> queryParameters = getQueryParameters();
        for (Map.Entry<String, List<Object>> entry : queryParameters.entrySet()) {
            String name = entry.getKey();
            List<Object> valueList = entry.getValue();
            if (ContainerUtils.isEmptyCollection(valueList)) {
                queryParamBuilder.append(name).append("=&");
            } else {
                for (Object value : valueList) {
                    queryParamBuilder.append(name).append("=").append(value).append("&");
                }
            }
        }
        String queryParamStr = queryParamBuilder.toString();
        return queryParamStr.endsWith("&") ? queryParamStr.substring(0, queryParamStr.length() - 1) : queryParamStr;
    }

    public String getUrlencodedParameterString() throws UnsupportedEncodingException {
        StringBuilder queryParamBuilder = new StringBuilder();
        Map<String, Object> requestParameters = getFormParameters();
        for (Map.Entry<String, Object> entry : requestParameters.entrySet()) {
            queryParamBuilder
                    .append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"))
                    .append("&");
        }
        String queryParamStr = queryParamBuilder.toString();
        return queryParamStr.endsWith("&") ? queryParamStr.substring(0, queryParamStr.length() - 1) : queryParamStr;
    }

    private String paramToString(String prefix, Map<String, Object> paramMap) {
        StringBuilder paramBuilder = new StringBuilder(prefix + ": {");
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                paramBuilder.append(name).append("=, ");
            } else {
                paramBuilder.append(name).append("=").append(value).append(", ");
            }
        }
        String paramStr = paramBuilder.toString();
        paramStr = paramStr.endsWith(", ") ? paramStr.substring(0, paramStr.length() - 2) : paramStr;
        return paramStr + "}";
    }

    @Override
    public String toString() {
        String queryParamStr = StringUtils.format("QUERY_PARAM: {{0}}", getQueryParameterString());
        String pathParamStr = paramToString("PATH_PARAM", getPathParameters());
        String fromParamStr = paramToString("FROM_PARAM", getFormParameters());
        String multipartParamStr = paramToString("MULTIPART_FROM_DATA_PARAM", getFormParameters());
        return StringUtils.format("{}\n{}\n{}\n{}\nBODY: {};", queryParamStr, pathParamStr, fromParamStr, multipartParamStr, (bodyParameter == null && bodyObjectFactory == null) ? "{}" : getBody().getBodyAsString());
    }
}
