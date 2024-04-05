package com.luckyframework.httpclient.core.impl;

/**
 * 内容编码转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/5 01:12
 */
public interface ContentEncodingConvertor {


    /**
     * byte数组转换
     *
     * @param old 原始byte数组
     * @return 转化后的byte数组
     */
    byte[] byteConvert(byte[] old);

}
