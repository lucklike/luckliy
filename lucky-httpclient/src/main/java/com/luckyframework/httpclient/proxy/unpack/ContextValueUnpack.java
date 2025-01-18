package com.luckyframework.httpclient.proxy.unpack;

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
     * @param unpackContext 上下文
     * @param wrapperValue  包装值
     * @return 真实值
     * @throws ContextValueUnpackException 拆包失败时会抛出该异常
     */
    Object getRealValue(ValueUnpackContext unpackContext, Object wrapperValue) throws ContextValueUnpackException;
}
