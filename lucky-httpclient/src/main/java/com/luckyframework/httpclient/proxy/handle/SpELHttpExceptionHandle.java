package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandle;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.exeception.AgreedOnMethodExecuteException;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
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

    @Override
    protected Object doExceptionHandler(MethodContext methodContext, Request request, Throwable throwable) {
        ExceptionHandle exceptionHandleAnn = methodContext.getMergedAnnotationCheckParent(ExceptionHandle.class);
        String expression = exceptionHandleAnn.excHandleExp();

        // 存在异常处理表达式
        if (StringUtils.hasText(expression)) {
            return handleExceptionExpression(methodContext, request, throwable, expression);
        }

        // 存在约定的异常处理方法
        Method agreedOnExceptionHandleMethod = getAgreedOnExceptionHandleMethod(methodContext);
        if (agreedOnExceptionHandleMethod != null) {

            // 执行约定的异常处理方法
            Object handleResult = executeAgreedOnMethod(methodContext, agreedOnExceptionHandleMethod);

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
     * 获取约定的ExceptionHandle方法
     *
     * @param context 方法上下文
     * @return 约定的ExceptionHandle方法
     */
    private Method getAgreedOnExceptionHandleMethod(MethodContext context) {
        final String SUFFIX = "ExceptionHandle";

        String handleVarName = context.getCurrentAnnotatedElement().getName() + SUFFIX;
        Method agreedOnHandleMethod = context.getVar(handleVarName, Method.class);
        if (agreedOnHandleMethod == null) {
            return null;
        }

        // 检查方法返回值类型的兼容性，不兼容直接返回null
        ResolvableType aohmType = ResolvableType.forMethodReturnType(agreedOnHandleMethod);
        if (aohmType.resolve() != void.class && !ClassUtils.compatibleOrNot(context.getReturnResolvableType(), aohmType)) {
            return null;
        }

        return agreedOnHandleMethod;
    }

    /**
     * 执行约定方法
     *
     * @param context        方法上下文
     * @param agreedOnMethod 约定方法
     * @return 执行结果
     */
    private Object executeAgreedOnMethod(MethodContext context, Method agreedOnMethod) {
        try {
            return MethodUtils.invoke(null, agreedOnMethod, context.getMethodParamObject(agreedOnMethod));
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
