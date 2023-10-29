package com.luckyframework.httpclient.proxy.impl.statics;

import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.StaticParamResolver;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于注解value属性、配置使用'='分隔并支持SpEL表达式的静态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 14:39
 */
public class SpELValueFieldEqualSeparationStaticParamResolver implements StaticParamResolver {


    @Override
    public List<ParamInfo> parser(MethodContext context, Annotation staticParamAnn) {
        String[] annotationAttributeValues = context.getAnnotationAttribute(staticParamAnn, getConfigAttribute(), String[].class);
        List<ParamInfo> paramInfoList = new ArrayList<>(annotationAttributeValues.length);
        for (String value : annotationAttributeValues) {
            int index = value.indexOf("=");
            if (index == -1) {
                throw new IllegalArgumentException("Wrong static expression: '" + value + "'");
            }
            String nameExpression = value.substring(0, index).trim();
            String valueExpression = value.substring(index + 1).trim();
            ParamInfo paramInfo = new ParamInfo(parseExpression(nameExpression, context, staticParamAnn), parseExpression(valueExpression, context, staticParamAnn));

            paramInfoList.add(postProcess(context, staticParamAnn, paramInfo));
        }
        return paramInfoList;
    }

    protected ParamInfo postProcess(MethodContext context, Annotation staticParamAnn, ParamInfo originalParamInfo) {
        return originalParamInfo;
    }

    protected String getConfigAttribute(){
        return "value";
    }

}
