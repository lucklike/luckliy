package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.ParamInfo;

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
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        String[] annotationAttributeValues = context.getAnnotationAttribute(getConfigAttribute(), String[].class);
        List<ParamInfo> paramInfoList = new ArrayList<>(annotationAttributeValues.length);
        for (String value : annotationAttributeValues) {
            int index = value.indexOf("=");
            if (index == -1) {
                throw new IllegalArgumentException("Wrong static parameter expression: '" + value + "'");
            }
            String nameExpression = value.substring(0, index).trim();
            String valueExpression = value.substring(index + 1).trim();
            ParamInfo paramInfo = new ParamInfo(parseExpression(nameExpression, context), parseExpression(valueExpression, context));

            paramInfoList.add(postProcess(context, paramInfo));
        }
        return paramInfoList;
    }

    protected ParamInfo postProcess(StaticParamAnnContext context, ParamInfo originalParamInfo) {
        return originalParamInfo;
    }

    protected String getConfigAttribute(){
        return "value";
    }

}
