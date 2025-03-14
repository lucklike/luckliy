package com.luckyframework.httpclient.proxy.unpack;

/**
 * 参数转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/15 00:47
 */
public interface ParameterConvert {

    /**
     * 是否可以转换
     *
     * @param value 参数值
     * @return 是否可以转换
     */
    boolean canConvert(Object value);

    /**
     * 转换逻辑
     *
     * @param value 参数值
     * @return 转换后的参数值
     */
    Object convert(Object value);
}
