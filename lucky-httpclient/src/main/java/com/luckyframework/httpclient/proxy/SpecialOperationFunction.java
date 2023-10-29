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

    Object change(String paramName, Object originalValue, Annotation specialAnn);
}
