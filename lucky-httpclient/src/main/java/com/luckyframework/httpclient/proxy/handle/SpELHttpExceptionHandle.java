package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyInvocationTargetException;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandle;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.MethodWrap;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionExecuteException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionMismatchException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionNotFoundException;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
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

            // 执行指定的异常处理函数
            Object handleResult = executeExceptionHandleFunc(methodContext, exceptionHandleFuncMethod);

            // 如果目标方法返回值为非void，但是异常处理方法为void方法，此时依然需要报错打日志
            if (methodContext.getRealMethodReturnType() != void.class && exceptionHandleFuncMethod.getReturnType() == void.class) {
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
        MethodWrap handleFuncMethodWrap = context.getSpELFuncOrDefault(funcName, EXCEPTION_HANDLE_FUNCTION_SUFFIX);

        // 找不到函数时的处理
        if (handleFuncMethodWrap.isNotFound()) {
            if (isAppoint) {
                throw new SpELFunctionNotFoundException("Exception handle SpEL function named '{}' is not found in context.", funcName);
            }
            return null;
        }

        // 函数返回值类型不匹配时的处理
        Method handleFuncMethod = handleFuncMethodWrap.getMethod();
        ResolvableType handleFuncReturnType = ResolvableType.forMethodReturnType(handleFuncMethod);
        if (handleFuncReturnType.resolve() != void.class && !ClassUtils.compatibleOrNot(context.getReturnResolvableType(), handleFuncReturnType)) {
            if (isAppoint) {
                throw new SpELFunctionMismatchException("The SpEL function '{}' specified for exception handling has a return value type that is incompatible with the original method.\n\t--- func-return-type: {} \n\t--- source-return-type: {}", funcName, handleFuncReturnType, context.getReturnResolvableType());
            }
            return null;
        }

        // 校验条件满足
        return handleFuncMethod;
    }

    /**
     * 执行异常处理函数
     *
     * @param context          方法上下文
     * @param handleFuncMethod 约定方法
     * @return 执行结果
     */
    private Object executeExceptionHandleFunc(MethodContext context, Method handleFuncMethod) {
        try {
            return context.invokeMethod(null, handleFuncMethod);
        } catch (LuckyInvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new ActivelyThrownException(cause);
        } catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new SpELFunctionExecuteException(e, "Exception Handling Method Running exception: ['{}']", MethodUtils.getLocation(handleFuncMethod));
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
    private Object handleExceptionExpression(MethodContext methodContext, Request request, Throwable throwable, String expression) throws Throwable {
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
