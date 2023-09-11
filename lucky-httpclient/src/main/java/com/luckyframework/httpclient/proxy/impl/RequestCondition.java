package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.exception.ConditionNotSatisfiedException;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.RequestAfterProcessor;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.SpELRuntime;

import java.lang.annotation.Annotation;

/**
 * 请求条件判断
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/12 03:24
 */
public class RequestCondition implements RequestAfterProcessor {

    @Override
    public void requestProcess(Request request, Annotation requestAfterHandleAnn) {
        String[] conditions = (String[]) AnnotationUtils.getValue(requestAfterHandleAnn, getRequestConditionFieldName());
        if (!ContainerUtils.isEmptyArray(conditions)) {
            SpELRuntime spELRuntime = HttpClientProxyObjectFactory.getSpELConverter().getSpELRuntime();
            for (String condition : conditions) {
                boolean isPass = spELRuntime.getValueForType(new ParamWrapper(condition).setRootObject(request).setExpectedResultType(boolean.class));
                if (!isPass) {
                    throw new ConditionNotSatisfiedException("The current request instance does not meet the condition '{}' configured in the condition comment, the current request is :{}", condition, request);
                }
            }
        }
    }

    protected String getRequestConditionFieldName(){
        return "value";
    }
}
