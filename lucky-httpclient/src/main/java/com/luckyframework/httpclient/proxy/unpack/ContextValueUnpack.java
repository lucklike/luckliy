package com.luckyframework.httpclient.proxy.unpack;

import java.lang.annotation.Annotation;

/**
 * 上下文参数值拆包器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/11/24 13:35
 */
@FunctionalInterface
public interface ContextValueUnpack {

    /**
     * 用于将包装值转化为真实值的方法
     *
     * @param wrapperValue 包装值
     * @param unpackAnn 拆包注解实例
     * @return 真实值
     * @throws ContextValueUnpackException 拆包失败时会抛出该异常
     */
    Object getRealValue(Object wrapperValue, Annotation unpackAnn) throws ContextValueUnpackException;
}
