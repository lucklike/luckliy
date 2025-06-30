package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyInvocationTargetException;
import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.MethodWrap;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionExecuteException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionMismatchException;
import com.luckyframework.httpclient.proxy.exeception.SpELFunctionNotFoundException;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;

/**
 * 支持结果选择的响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 02:28
 */
@SuppressWarnings("unchecked")
public class ResultSelectionResponseConvert extends AbstractConditionalSelectionResponseConvert {

    /**
     * 约定的转换方法后缀
     */
    public final String CONVERT_FUNCTION_SUFFIX = "$Convert";

    @Override
    protected <T> T doConvert(Response response, ConvertContext context) throws Throwable {
        MethodContext methodContext = context.getContext();

        // 获取方法上和类上的@RespConvert注解
        RespConvert classRcAnn = methodContext.getParentContext().getMergedAnnotation(RespConvert.class);
        RespConvert methodRcAnn = methodContext.getMergedAnnotation(RespConvert.class);

        boolean hasClassRcAnn = classRcAnn != null;
        boolean hasMethodRcAnn = methodRcAnn != null;

        // 获取result，如果result不为null则直接执行表达式返回结果
        String classResult = hasClassRcAnn ? (StringUtils.hasText(classRcAnn.result()) ? classRcAnn.result() : null) : null;
        String methodResult = hasMethodRcAnn ? (StringUtils.hasText(methodRcAnn.result()) ? methodRcAnn.result() : null) : null;
        String result = StringUtils.hasText(methodResult) ? methodResult : classResult;

        if (StringUtils.hasText(result)) {
            return context.parseExpression(
                    result,
                    methodContext.getResultType()
            );
        }

        // 获取用于结果处理的SpEL函数
        String classResultFuncName = hasClassRcAnn ? (StringUtils.hasText(classRcAnn.resultFunc()) ? classRcAnn.resultFunc() : null) : null;
        String methodResultFuncName = hasMethodRcAnn ? (StringUtils.hasText(methodRcAnn.resultFunc()) ? methodRcAnn.resultFunc() : null) : null;
        String resultFuncName = StringUtils.hasText(methodResultFuncName) ? methodResultFuncName : classResultFuncName;

        Method respConvertFuncMethod = getRespConvertFuncMethod(methodContext, resultFuncName);
        if (respConvertFuncMethod != null) {
            return (T) executeConvertFuncMethod(methodContext, respConvertFuncMethod);
        }

        // 获取exception，如果exception不为null则直接执行表达式抛出异常
        String classException = hasClassRcAnn ? (StringUtils.hasText(classRcAnn.exception()) ? classRcAnn.exception() : null) : null;
        String methodException = hasMethodRcAnn ? (StringUtils.hasText(methodRcAnn.exception()) ? methodRcAnn.exception() : null) : null;
        String exception = StringUtils.hasText(methodException) ? methodException : classException;

        if (StringUtils.hasText(exception)) {
            throwException(context, exception);
        }

        // result、exception均为null则尝试直接将响应内容转换为方法返回值类型结果
        return getMethodResult(response, context.getContext());

    }

    /**
     * 获取用于响应结果处理的SpEL函数
     *
     * @param context  上下文对象
     * @param funcName 指定的转换函数
     * @return 对应的Method对象
     */
    private Method getRespConvertFuncMethod(MethodContext context, String funcName) {

        // 是否指定了处理函数
        boolean isAppoint = StringUtils.hasText(funcName);

        // 获取指定的Convert函数名，如果不存在则使用约定的Convert函数名
        MethodWrap convertFuncMethodWrap = context.getSpELFuncOrDefault(funcName, CONVERT_FUNCTION_SUFFIX);

        // 找不到函数时的处理
        if (convertFuncMethodWrap.isNotFound()) {
            if (isAppoint) {
                throw new SpELFunctionNotFoundException("Response Convert SpEL function named '{}' is not found in context.", funcName);
            }
            return null;
        }

        // 函数返回值类型不匹配时的处理
        Method convertFuncMethod = convertFuncMethodWrap.getMethod();
        if (!ClassUtils.compatibleOrNot(ResolvableType.forMethodReturnType(convertFuncMethod), context.getRealMethodReturnResolvableType())) {
            if (isAppoint) {
                throw new SpELFunctionMismatchException(
                        "The SpEL function '{}' that responds to the conversion returns a value of a type incompatible with the target type of the conversion. \n\t--- func-return-type: {} \n\t--- target-type: {}",
                        funcName,
                        ResolvableType.forMethodReturnType(convertFuncMethod),
                        context.getRealMethodReturnResolvableType()
                );
            }
            return null;
        }

        // 校验条件满足
        return convertFuncMethod;
    }

    /**
     * 执行响应转换方法
     *
     * @param context           方法上下文
     * @param convertFuncMethod 响应转换方法
     * @return 执行结果
     */
    private Object executeConvertFuncMethod(MethodContext context, Method convertFuncMethod) throws Throwable {
        try {
            return context.invokeMethod(null, convertFuncMethod);
        }
        catch (LuckyInvocationTargetException e) {
            throw e.getCause();
        }
        catch (MethodParameterAcquisitionException | LuckyReflectionException e) {
            throw new SpELFunctionExecuteException(e, "Response Convert method run exception: ['{}']", MethodUtils.getLocation(convertFuncMethod));
        }
    }
}
