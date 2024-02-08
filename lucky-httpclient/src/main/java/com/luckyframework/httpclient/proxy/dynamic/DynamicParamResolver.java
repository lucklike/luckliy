package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.annotations.DynamicParam;
import com.luckyframework.httpclient.proxy.context.ValueContext;

import java.util.List;

/**
 * 动态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 14:22
 */
public interface DynamicParamResolver {

    /**
     * 动态参数解析
     *
     * @param context 动态注解上下文
     * @return 参数信息集合
     */
    List<? extends ParamInfo> parser(DynamicParamContext context);

    /**
     * 获取原始参数名称
     *
     * @param context 参数上下文
     * @return 原始参数名称
     */
    default String getOriginalParamName(ValueContext context) {
        DynamicParam dynamicParamAnn = context.getMergedAnnotation(DynamicParam.class);
        return (dynamicParamAnn != null && StringUtils.hasText(dynamicParamAnn.name())) ? dynamicParamAnn.name() : context.getName();
    }
}
