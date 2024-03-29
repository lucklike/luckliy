package com.luckyframework.httpclient.proxy;

import java.lang.annotation.Annotation;

/**
 * 参数处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 10:16
 */
@FunctionalInterface
public interface ParameterProcessor {

    /**
     * 参数加工方法，将原始参数加工成目标参数的方法
     *
     * @param originalParam   原始参数
     * @param dynamicParamAnn 当前注解实例
     * @return 目标参数
     */
    Object paramProcess(Object originalParam, Annotation dynamicParamAnn);

    /**
     * 遇到复杂类型是否需要展开解析
     */
    default boolean needExpansionAnalysis() {
        return true;
    }

}
