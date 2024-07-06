package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.annotations.Condition;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于注解value属性、配置使用特定符号进行分隔并支持SpEL表达式的静态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 14:39
 */
public class SpELValueFieldSeparationStaticParamResolver implements StaticParamResolver {


    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        String[] annotationAttributeValues = context.getAnnotationAttribute(getConfigAttribute(), String[].class);
        List<ParamInfo> paramInfoList = new ArrayList<>(annotationAttributeValues.length);
        String separation = getSeparation(context);
        for (String value : annotationAttributeValues) {
            int index = value.indexOf(separation);
            if (index == -1) {
                throw new IllegalArgumentException("Wrong static parameter expression: '" + value + "'. Please use the correct separator: '" + separation + "'");
            }

            String nameExpression = value.substring(0, index).trim();
            String valueExpression = value.substring(index + separation.length()).trim();

            ParamInfo paramInfo = new ParamInfo(context.parseExpression(nameExpression), context.parseExpression(valueExpression));
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

    protected String getSeparation(StaticParamAnnContext context) {
        try {
            return context.getAnnotationAttribute("separator", String.class);
        }catch (Exception e) {
            return "=";
        }
    }
}
