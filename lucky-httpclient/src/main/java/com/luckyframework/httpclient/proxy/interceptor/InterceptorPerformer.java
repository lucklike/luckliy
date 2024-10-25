package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * 拦截器执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 07:15
 */
public class InterceptorPerformer {

    public static final String BEFORE_EXE_RECORD_VAR = "$interceptorBeforeExecutionRecord$";
    public static final String AFTER_EXE_RECORD_VAR = "$interceptorAfterExecutionRecord$";

    private final Function<MethodContext, Interceptor> interceptorFunction;
    private final Annotation interceptorRegisterAnn;
    private final Integer priority;

    public InterceptorPerformer(Function<MethodContext, Interceptor> interceptorFunction, Annotation interceptorRegisterAnn, Integer priority) {
        this.interceptorFunction = interceptorFunction;
        this.interceptorRegisterAnn = interceptorRegisterAnn;
        this.priority = priority;
    }

    public InterceptorPerformer(Function<MethodContext, Interceptor> interceptorFunction, Integer priority) {
        this(interceptorFunction, null, priority);
    }

    public InterceptorPerformer(Interceptor interceptor, Annotation interceptorRegisterAnn, Integer priority) {
        this(context -> interceptor, interceptorRegisterAnn, priority);
    }

    public InterceptorPerformer(Interceptor interceptor, Annotation interceptorRegisterAnn) {
        this(interceptor, interceptorRegisterAnn, interceptor.priority());
    }

    public InterceptorPerformer(Interceptor interceptor, Integer priority) {
        this(interceptor, null, priority);
    }

    public InterceptorPerformer(Interceptor interceptor) {
        this(interceptor, null, interceptor.priority());
    }

    /**
     * 获取拦截器的优先级
     *
     * @param context 拦截器上注解下文
     * @return 拦截器的优先级
     */
    public int getPriority(MethodContext context) {
        return this.priority == null ? getInterceptor(context).priority() : priority;
    }


    /**
     * 请求执行之前会执行该方法
     *
     * @param request 请求对象
     * @param context 拦截器上注解下文
     */

    public void beforeExecute(Request request, MethodContext context) {
        beforeExecute(request, new InterceptorContext(context, interceptorRegisterAnn), getInterceptor(context));
    }

    /**
     * 请求处理完毕得到响应之后会执行该方法
     *
     * @param response 响应数据
     * @param context  响应拦截器注解上下文
     */
    public Response afterExecute(Response response, MethodContext context) {
        return afterExecute(response, new InterceptorContext(context, interceptorRegisterAnn), getInterceptor(context));
    }

    /**
     * 获取拦截器实例
     *
     * @param context 响应拦截器注解上下文
     * @return 拦截器实例
     */
    public Interceptor getInterceptor(MethodContext context) {
        return interceptorFunction.apply(context);
    }

    //------------------------------------------------------------------------------------------------
    //                                  static method
    //------------------------------------------------------------------------------------------------

    /**
     * 请求执行之前会执行该方法
     *
     * @param request     请求对象
     * @param context     拦截器上注解下文
     * @param interceptor 拦截器实例
     */
    public static void beforeExecute(Request request, InterceptorContext context, Interceptor interceptor) {
        if (haveExecuted(context.getContext(), interceptor, BEFORE_EXE_RECORD_VAR)) {
            return;
        }
        interceptor.beforeExecute(request, context);
    }

    /**
     * 请求处理完毕得到响应之后会执行该方法
     *
     * @param response    响应对象
     * @param context     拦截器上注解下文
     * @param interceptor 拦截器实例
     */
    public static Response afterExecute(Response response, InterceptorContext context, Interceptor interceptor) {
        if (haveExecuted(context.getContext(), interceptor, AFTER_EXE_RECORD_VAR)) {
            return response;
        }
        return interceptor.afterExecute(response, context);
    }

    /**
     * 判断当前拦截器是否已经执行过了
     *
     * @param context     方法上下文
     * @param interceptor 拦截器实例
     * @param varName     变量名
     * @return 当前拦截器是否已经执行过了
     */
    @SuppressWarnings("all")
    public static boolean haveExecuted(MethodContext context, Interceptor interceptor, String varName) {
        Set<String> idSet = (Set<String>) context.getContextVar().getRootObject().computeIfAbsent(varName, _k -> new HashSet<>());
        String id = interceptor.uniqueIdentification();
        if (idSet.contains(id)) {
            return true;
        }
        idSet.add(id);
        return false;
    }
}
