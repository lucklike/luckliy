package com.luckyframework.httpclient.proxy.impl.convert;

import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.SpELConvert;
import com.luckyframework.httpclient.proxy.annotations.Branch;
import com.luckyframework.reflect.AnnotationUtils;

import java.lang.annotation.Annotation;

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
                                .setExpectedResultType(methodContext.getRealMethodReturnType())
                );
            }
        }

        return getDefaultValue(response, methodContext, resultConvertAnn);
    }
}
