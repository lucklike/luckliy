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
        RespConvert classCsAnn = methodContext.getParentContext().getMergedAnnotation(RespConvert.class);
        RespConvert methodCsAnn = methodContext.getMergedAnnotation(RespConvert.class);

        boolean hasClassCsAnn = classCsAnn != null;
        boolean hasMethodCsAnn = methodCsAnn != null;

        if (hasClassCsAnn) {
            branches.addAll(Arrays.asList(classCsAnn.branch()));
        }

        if (hasMethodCsAnn) {
            branches.addAll(Arrays.asList(methodCsAnn.branch()));
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


        // 获取result
        String classResult = hasClassCsAnn ? (StringUtils.hasText(classCsAnn.result()) ? classCsAnn.result() : "") : "";
        String methodResult = hasMethodCsAnn ? (StringUtils.hasText(methodCsAnn.result()) ? methodCsAnn.result() : "") : "";
        String result = StringUtils.hasText(methodResult) ? methodResult : classResult;

        // 获取Exception
        String classException = hasClassCsAnn ? (StringUtils.hasText(classCsAnn.exception()) ? classCsAnn.exception() : "") : "";
        String methodException = hasMethodCsAnn ? (StringUtils.hasText(methodCsAnn.exception()) ? methodCsAnn.exception() : "") : "";
        String exception = StringUtils.hasText(methodException) ? methodException : classException;

        return resoponseConvert(context, response, result, exception);
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
