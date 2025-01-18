package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.ArrayList;
import java.util.Collections;
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
        String condition = context.getAnnotationAttribute(getConditionAttribute(), String.class);
        if (StringUtils.hasText(condition) && !context.parseExpression(condition, boolean.class)) {
            return Collections.emptyList();
        }

        // 使用@if表达式进行过滤
        List<ParamInfo> paramInfoList = new ArrayList<>(annotationAttributeValues.length);
        IfExpressionUtils.filterAndAdd(
                context.getContext(),
                paramInfoList,
                annotationAttributeValues,
                getSeparation(context),
                (e, k, v, kv, vv) -> postProcess(context, new ParamInfo(kv, vv))
        );
        return paramInfoList;
    }

    /**
     * 参数信息的后缀处理
     *
     * @param context           注解上下文
     * @param originalParamInfo 原始参数信息
     * @return 处理后的参数信息
     */
    protected ParamInfo postProcess(StaticParamAnnContext context, ParamInfo originalParamInfo) {
        return originalParamInfo;
    }

    /**
     * 获取用户配置属性名
     *
     * @return 用户配置属性名
     */
    protected String getConfigAttribute() {
        return "value";
    }

    /**
     * 获取条件属性名
     *
     * @return 条件属性名
     */
    protected String getConditionAttribute() {
        return "condition";
    }

    /**
     * 获取表达式分隔符
     *
     * @param context 注解上下文信息
     * @return 表达式分隔符
     */
    protected String getSeparation(StaticParamAnnContext context) {
        try {
            return context.getAnnotationAttribute("separator", String.class);
        } catch (Exception e) {
            return "=";
        }
    }
}
