package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.MethodMetaContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_CLASS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_CLASS_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_HTTP_PROXY_FACTORY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_META_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_RESPONSE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_THIS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_THROWABLE_$;

/**
 * 基于上下文实现的参数实例获取器
 */
public class ContextParameterInstanceGetter implements ParameterInstanceGetter {

    private final Context context;

    public ContextParameterInstanceGetter(Context context) {
        this.context = context;
    }

    @Override
    public Object getParameterInstance(Parameter parameter) {
        Class<?> parameterType = parameter.getType();

        if (parameterType == MethodContext.class) {
            return context.getRootVar($_METHOD_CONTEXT_$);
        }
        else if (parameterType == MethodMetaContext.class) {
            return context.getRootVar($_METHOD_META_CONTEXT_$);
        }
        else if (parameterType == ClassContext.class) {
            return context.getRootVar($_CLASS_CONTEXT_$);
        }
        else if (parameterType == Method.class) {
            return context.getRootVar($_METHOD_$);
        }
        else if (parameterType == Class.class) {
            return context.getRootVar($_CLASS_$);
        }
        else if (parameterType == context.lookupContext(ClassContext.class).getCurrentAnnotatedElement()) {
            return context.getRootVar($_THIS_$);
        }
        else if (parameterType == Request.class) {
            return context.getRootVar($_REQUEST_$);
        }
        else if (parameterType == Response.class) {
            return context.getRootVar($_RESPONSE_$);
        }
        else if (Throwable.class.isAssignableFrom(parameterType)) {
            return context.getRootVar($_THROWABLE_$);
        }
        else if (HttpClientProxyObjectFactory.class.isAssignableFrom(parameterType)) {
            return context.getRootVar($_HTTP_PROXY_FACTORY_$);
        }
        else {
            return null;
        }
    }
}
