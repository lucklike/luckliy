package com.luckyframework.httpclient.proxy;

import java.lang.annotation.Annotation;

/**
 * 特殊操作方法
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/28 02:27
 */
@FunctionalInterface
public interface SpecialOperationFunction {

    /**
     * 执行一个特殊操作，返回一个对象，这个对象将作为最终的参数值
     *
     * @param paramName     参数名
     * @param originalValue 原参数值
     * @param specialAnn    特殊操作注解
     * @return 执行一个特殊操作，返回一个对象，这个对象将作为最终的参数值
     */
    Object change(String paramName, Object originalValue, Annotation specialAnn);
}
