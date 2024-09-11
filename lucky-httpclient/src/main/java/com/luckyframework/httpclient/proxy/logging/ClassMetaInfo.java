package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.mock.MockMeta;
import com.luckyframework.reflect.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.List;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.MOCK_RESPONSE_FACTORY;

/**
 * 类的元信息
 */
public class ClassMetaInfo {

    private final MethodContext context;

    public ClassMetaInfo(MethodContext context) {
        this.context = context;
    }

    public String getPoxyModel() {
        Object proxyObject = context.getProxyObject();
        return ClassUtils.isJDKProxy(proxyObject) ? "JDK" : "CGLIB";
    }

    public String getApiClassName() {
        return ((Class<?>)context.getParentContext().getCurrentAnnotatedElement()).getName();
    }

    public String getApiMethodName() {
        return context.getCurrentAnnotatedElement().toGenericString();
    }

    public String getHttpExecutorName() {
        return context.getHttpExecutor().getClass().getName();
    }

    public ParameterContext[] getArgsInfo() {
        return context.getParameterContexts();
    }

    private boolean isMock(MethodContext methodContext) {
        if(methodContext.getVar(MOCK_RESPONSE_FACTORY) != null) {
            return true;
        }
        MockMeta mockAnn = methodContext.getSameAnnotationCombined(MockMeta.class);
        return  mockAnn != null &&(
                !StringUtils.hasText(mockAnn.condition()) ||
                        methodContext.parseExpression(mockAnn.condition(), boolean.class));
    }
}
