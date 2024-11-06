package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandle;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 支持使用SpEL表达式的
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
        if (!StringUtils.hasText(expression)) {
            // 配进行任何配置时尝试查找约定好的异常处理逻辑
            String agreedOnExceptionHandleExpression = getAgreedOnExceptionHandleExpression(methodContext);
            if (StringUtils.hasText(agreedOnExceptionHandleExpression)) {
                return methodContext.parseExpression(agreedOnExceptionHandleExpression);
            }
            return throwExceptionPrintLog(methodContext, throwable);
        }
        return methodContext.parseExpression(expression);
    }

    private String getAgreedOnExceptionHandleExpression(MethodContext methodContext) {
        final String SUFFIX = "ExceptionHandle";
        String handleVarName = methodContext.getCurrentAnnotatedElement().getName() + SUFFIX;
        Method agreedOnHandleMethod = methodContext.getVar(handleVarName, Method.class);
        if (agreedOnHandleMethod == null) {
            return null;
        }
        // 检查方法返回值类型的兼容性，不兼容直接返回null
        ResolvableType aohmType = ResolvableType.forMethodReturnType(agreedOnHandleMethod);
        if (aohmType.resolve() != void.class && !ClassUtils.compatibleOrNot(methodContext.getReturnResolvableType(), aohmType)) {
            return null;
        }

        String expressionTemp = "#{#%s(%s)}";
        List<String> varNameList = methodContext.getMethodParamVarNames(agreedOnHandleMethod);
        String varStr = StringUtils.join(varNameList, ", ");
        return String.format(expressionTemp, handleVarName, varStr);
    }
}
