package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

/**
 * 键值对后置处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/3 00:49
 */
@FunctionalInterface
public interface KeyValuePostProcess {

    /**
     * 处理表达式信息并生成参数信息对象
     *
     * @param expression           原始表达式
     * @param keyExpression        Key表达式
     * @param valueExpression      Value表达式
     * @param keyExpressionValue   Key表达式结果
     * @param valueExpressionValue Value表达式结果
     * @return 参数信息对象
     */
    ParamInfo process(String expression, String keyExpression, String valueExpression, Object keyExpressionValue, Object valueExpressionValue);

}
