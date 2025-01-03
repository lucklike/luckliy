package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandle;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.exeception.AgreedOnMethodExecuteException;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionMismatchException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionNotFoundException;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;

/**
 * 支持使用SpEL表达式的异常处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/26 15:11
 */
public class SpELHttpExceptionHandle extends AbstractHttpExceptionHandle {

    @Override
    protected Object doExceptionHandler(MethodContext methodContext, Request request, Throwable throwable) {
        ExceptionHandle exceptionHandleAnn = methodContext.getMergedAnnotationCheckParent(ExceptionHandle.class);
        String expression = exceptionHandleAnn.excHandleExp();

        // 存在异常处理表达式
        if (StringUtils.hasText(expression)) {
            return handleExceptionExpression(methodContext, request, throwable, expression);
        }

        // 存在约定的异常处理方法
        Method agreedOnExceptionHandleMethod = getExceptionHandleFuncMethod(methodContext, exceptionHandleAnn.handleFunc());
        if (agreedOnExceptionHandleMethod != null) {

            // 执行约定的异常处理方法
            Object handleResult = executeExceptionHandleFunc(methodContext, agreedOnExceptionHandleMethod);

            // 如果目标方法返回值为非void，但是异常处理方法为void方法，此时依然需要报错打日志
            if (methodContext.getRealMethodReturnType() != void.class && agreedOnExceptionHandleMethod.getReturnType() == void.class) {
                return throwExceptionPrintLog(methodContext, throwable);
            }
            return handleResult;
        }

        // 默认的异常处理
        return throwExceptionPrintLog(methodContext, throwable);
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
        String handleFuncName = isAppoint ? funcName : context.getCurrentAnnotatedElement().getName() + "ExceptionHandle";

        Method handleFuncMethod = context.getVar(handleFuncName, Method.class);
        if (handleFuncMethod == null) {
            if (isAppoint) {
                throw new SpELFunctionNotFoundException("SpEL function named '{}' is not found in context.", handleFuncName);
            }
            return null;
        }

        // 检查方法返回值类型的兼容性，不兼容直接返回null
        ResolvableType handleFuncReturnType = ResolvableType.forMethodReturnType(handleFuncMethod);
        if (handleFuncReturnType.resolve() != void.class && !ClassUtils.compatibleOrNot(context.getReturnResolvableType(), handleFuncReturnType)) {
            if (isAppoint) {
                throw new SpELFunctionMismatchException("The return value type of the SpEL function used for exception handling is incompatible with the original method.\n func: {} \n source: {}", handleFuncReturnType, context.getReturnResolvableType());
            }
            return null;
        }

        return handleFuncMethod;
    }

    /**
     * 执行异常处理函数
     *
     * @param context        方法上下文
     * @param agreedOnMethod 约定方法
     * @return 执行结果
     */
    private Object executeExceptionHandleFunc(MethodContext context, Method agreedOnMethod) {
        try {
            return context.invokeMethod(null, agreedOnMethod);
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new AgreedOnMethodExecuteException(e, "Exception Handling Method Running exception: {}", agreedOnMethod.toGenericString());
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
    @SuppressWarnings("unchecked")
    private Object handleExceptionExpression(MethodContext methodContext, Request request, Throwable throwable, String expression) {
        Object expressionResult = methodContext.parseExpression(expression);
        if (expressionResult instanceof HttpExceptionHandle) {
            return ((HttpExceptionHandle) expressionResult).exceptionHandler(methodContext, request, throwable);
        }
        if ((expressionResult instanceof Class) && (HttpExceptionHandle.class.isAssignableFrom((Class<?>) expressionResult))) {
            return methodContext.generateObject((Class<HttpExceptionHandle>) expressionResult, Scope.SINGLETON).exceptionHandler(methodContext, request, throwable);
        }
        return expressionResult;
    }

}
