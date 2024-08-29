package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.Branch;
import com.luckyframework.httpclient.proxy.annotations.RespConvert;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 条件转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 02:28
 */
public class ConditionalSelectionResponseConvert extends AbstractSpELResponseConvert {

    @Override
    public <T> T convert(Response response, ConvertContext context) throws Throwable {
        List<Branch> branches = new ArrayList<>();
        MethodContext methodContext = context.getContext();

        // 获取方法上和类上的@RespConvert注解
        RespConvert classRcAnn = methodContext.getParentContext().getMergedAnnotation(RespConvert.class);
        RespConvert methodRcAnn = methodContext.getMergedAnnotation(RespConvert.class);

        boolean hasClassRcAnn = classRcAnn != null;
        boolean hasMethodRcAnn = methodRcAnn != null;

        if (hasClassRcAnn) {
            branches.addAll(Arrays.asList(classRcAnn.conditions()));
        }

        if (hasMethodRcAnn) {
            branches.addAll(Arrays.asList(methodRcAnn.conditions()));
        }

        for (Branch branch : branches) {
            boolean assertion = context.parseExpression(branch.assertion(), boolean.class);
            if (assertion) {
                String result = branch.result();
                if (StringUtils.hasText(result)) {
                    return context.parseExpression(
                            result,
                            getReturnType(context.getContext(), branch.returnType())
                    );
                }

                String exception = branch.exception();
                if (StringUtils.hasText(exception)) {
                    throwException(context, exception);
                }
                throw new ConditionalSelectionException("@RespConvert annotation branch attribute The 'result' and 'exception' attributes of @Branch cannot be null at the same time");
            }
        }


        // 获取result，如果result不为null则直接执行表达式返回结果
        String classResult = hasClassRcAnn ? (StringUtils.hasText(classRcAnn.result()) ? classRcAnn.result() : "") : "";
        String methodResult = hasMethodRcAnn ? (StringUtils.hasText(methodRcAnn.result()) ? methodRcAnn.result() : "") : "";
        String result = StringUtils.hasText(methodResult) ? methodResult : classResult;

        if (StringUtils.hasText(result)) {
            return context.parseExpression(
                    result,
                    context.getRealMethodReturnType()
            );
        }


        // 获取exception，如果exception不为null则直接执行表达式抛出异常
        String classException = hasClassRcAnn ? (StringUtils.hasText(classRcAnn.exception()) ? classRcAnn.exception() : "") : "";
        String methodException = hasMethodRcAnn ? (StringUtils.hasText(methodRcAnn.exception()) ? methodRcAnn.exception() : "") : "";
        String exception = StringUtils.hasText(methodException) ? methodException : classException;

        if (StringUtils.hasText(exception)) {
            throwException(context, exception);
        }

        // result、exception均为null则尝试直接将响应内容转换为方法返回值类型结果
        return getMethodResult(response, context.getContext());
    }

    private Type getReturnType(MethodContext methodContext, Class<?> branchClass) {
        Type realMethodReturnType = methodContext.getRealMethodReturnType();
        // 方法返回值类型与@Branch注解中配置的类型兼容时返回@Branch注解中配置的类型
        if (Objects.requireNonNull(ResolvableType.forType(realMethodReturnType).getRawClass()).isAssignableFrom(branchClass)) {
            return branchClass;
        }
        return realMethodReturnType;
    }

}
