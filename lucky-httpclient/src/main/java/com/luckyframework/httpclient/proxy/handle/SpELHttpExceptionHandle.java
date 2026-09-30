package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.common.FontUtil;
import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyInvocationTargetException;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandle;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.MethodWrap;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionExecuteException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionNotFoundException;
import com.luckyframework.reflect.MethodUtils;

import java.lang.reflect.Method;

/**
 * 支持使用SpEL表达式的异常处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/26 15:11
 */
public class SpELHttpExceptionHandle extends AbstractHttpExceptionHandle {

    /**
     * 约定的异常处理方法后缀
     */
    public final String EXCEPTION_HANDLE_FUNCTION_SUFFIX = "$ExceptionHandle";

    @Override
    protected Object doExceptionHandler(MethodContext methodContext, Request request, Throwable throwable) throws Throwable {
        ExceptionHandle exceptionHandleAnn = methodContext.getMergedAnnotationCheckParent(ExceptionHandle.class);
        String expression = exceptionHandleAnn.excHandleExp();

        // 存在异常处理表达式
        if (StringUtils.hasText(expression)) {
            return handleExceptionExpression(methodContext, request, throwable, expression);
        }

        // 检查是否配置了处理函数名以及约定处理函数
        Method exceptionHandleFuncMethod = getExceptionHandleFuncMethod(methodContext, exceptionHandleAnn.handleFunc());
        if (exceptionHandleFuncMethod != null) {
            return handleExceptionResult(methodContext, request, throwable, executeExceptionHandleFunc(methodContext, exceptionHandleFuncMethod));
        }

        // 默认的异常处理
        return throwExceptionPrintLog(methodContext, request, throwable);
    }

    /**
     * 获取指定的ExceptionHandle处理方法
     *
     * @param context 方法上下文
     * @return 约定的ExceptionHandle方法
     */
    private Method getExceptionHandleFuncMethod(MethodContext context, String funcName) {

        // 是否指定了处理函数
        boolean isAppoint = StringUtils.hasText(funcName);

        // 获取指定异常处理的SpEL函数
        MethodWrap handleFuncMethodWrap = context.getSpELFuncOrDefault(funcName, EXCEPTION_HANDLE_FUNCTION_SUFFIX);

        // 找不到函数时的处理
        if (handleFuncMethodWrap.isNotFound()) {
            if (isAppoint) {
                throw new SpELFunctionNotFoundException("Exception handle SpEL function named '{}' is not found in context.", funcName);
            }
            return null;
        }

        // 校验条件满足
        return handleFuncMethodWrap.getMethod();
    }

    /**
     * 执行异常处理函数
     *
     * @param context          方法上下文
     * @param handleFuncMethod 约定方法
     * @return 执行结果
     */
    private Object executeExceptionHandleFunc(MethodContext context, Method handleFuncMethod) throws Throwable {
        try {
            return context.autoInjectParamExecuteMethod(null, handleFuncMethod);
        } catch (LuckyInvocationTargetException e) {
            throw e.getCause();
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new SpELFunctionExecuteException(e, "Exception handling method running exception: ['{}']", FontUtil.getRedUnderline(MethodUtils.getLocation(handleFuncMethod)));
        }
    }

    /**
     * 处理异常表达式
     *
     * @param methodContext 方法上下文
     * @param request       请求实例
     * @param throwable     异常实例
     * @param expression    异常处理表达式
     * @return 处理结果
     */
    private Object handleExceptionExpression(MethodContext methodContext, Request request, Throwable throwable, String expression) throws Throwable {
        return handleExceptionResult(methodContext, request, throwable, methodContext.parseExpression(expression));
    }

    /**
     * 处理异常表达式
     *
     * @param methodContext    方法上下文
     * @param request          请求实例
     * @param throwable        异常实例
     * @param expressionResult 异常处理结果
     * @return 处理结果
     */
    @SuppressWarnings("unchecked")
    private Object handleExceptionResult(MethodContext methodContext, Request request, Throwable throwable, Object expressionResult) throws Throwable {
        if (expressionResult instanceof HttpExceptionHandle) {
            return ((HttpExceptionHandle) expressionResult).exceptionHandler(methodContext, request, throwable);
        }
        if ((expressionResult instanceof Class) && (HttpExceptionHandle.class.isAssignableFrom((Class<?>) expressionResult))) {
            return methodContext.generateObject((Class<HttpExceptionHandle>) expressionResult, Scope.SINGLETON).exceptionHandler(methodContext, request, throwable);
        }
        if (expressionResult instanceof Throwable) {
            throw (Throwable) expressionResult;
        }
        return expressionResult;
    }

}
