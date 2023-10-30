package com.luckyframework.httpclient.proxy.impl.convert;

import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.SpELConvert;
import com.luckyframework.httpclient.proxy.annotations.Branch;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.core.ResolvableType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.ANNOTATION_INSTANCE;

/**
 * 条件转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 02:28
 */
public class ConditionalSelectionResponseConvert extends AbstractSpELResponseConvert {
    @Override
    public <T> T convert(Response response, MethodContext methodContext, Annotation resultConvertAnn) throws Exception {
        // 获取配置
        Branch[] branches = (Branch[]) AnnotationUtils.getValue(resultConvertAnn, "branch");

        SpELConvert spELConverter = getSpELConverter();
        for (Branch branch : branches) {
            boolean assertion = spELConverter.parseExpression(
                    getResponseSpElParamWrapper(response, methodContext)
                            .addVariable(ANNOTATION_INSTANCE, resultConvertAnn)
                            .setExpression(branch.assertion())
                            .setExpectedResultType(boolean.class));
            if (assertion) {
                return spELConverter.parseExpression(
                        getResponseSpElParamWrapper(response, methodContext)
                                .addVariable(ANNOTATION_INSTANCE, resultConvertAnn)
                                .setExpression(branch.result())
                                .setExpectedResultType(getReturnType(methodContext, branch.returnType()))
                );
            }
        }

        return getDefaultValue(response, methodContext, resultConvertAnn);
    }

    private Type getReturnType(MethodContext methodContext, Class<?> branchClass) {
        Type realMethodReturnType = methodContext.getRealMethodReturnType();
        // 方法返回值类型与@Branch注解中配置的类型兼容时返回@Branch注解中配置的类型
        if (ResolvableType.forType(realMethodReturnType).getRawClass().isAssignableFrom(branchClass)) {
           return branchClass;
        }
        return realMethodReturnType;
    }
}
