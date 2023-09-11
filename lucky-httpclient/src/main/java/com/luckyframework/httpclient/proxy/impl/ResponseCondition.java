package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.exception.ConditionNotSatisfiedException;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.RequestAfterProcessor;
import com.luckyframework.httpclient.proxy.ResponseAfterProcessor;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.SpELRuntime;

import java.lang.annotation.Annotation;

/**
 * 响应条件判断
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/12 03:24
 */
public class ResponseCondition implements ResponseAfterProcessor {


    @Override
    public void responseProcess(Response response, Annotation responseAfterHandleAnn) {
        String[] conditions = (String[]) AnnotationUtils.getValue(responseAfterHandleAnn, getResponseConditionFieldName());
        if (!ContainerUtils.isEmptyArray(conditions)) {
            SpELRuntime spELRuntime = HttpClientProxyObjectFactory.getSpELConverter().getSpELRuntime();
            for (String condition : conditions) {
                boolean isPass = spELRuntime.getValueForType(new ParamWrapper(condition).setRootObject(response).setExpectedResultType(boolean.class));
                if (!isPass) {
                    throw new ConditionNotSatisfiedException("The response to the current request does not meet the condition '{}' in the condition comment, the current request is: {}, the response is: {}",
                            condition,
                            response.getRequest(),
                            StringUtils.format("[{}] {}", response.getState(), response.getStringResult()));
                }
            }
        }
    }

    protected String getResponseConditionFieldName(){
        return "value";
    }
}
