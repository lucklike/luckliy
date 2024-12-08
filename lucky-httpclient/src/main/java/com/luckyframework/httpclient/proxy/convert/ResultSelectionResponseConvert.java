package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 支持结果选择的响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 02:28
 */
public class ResultSelectionResponseConvert extends AbstractConditionalSelectionResponseConvert {

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
                    context.getRealMethodReturnType()
            );
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
}
