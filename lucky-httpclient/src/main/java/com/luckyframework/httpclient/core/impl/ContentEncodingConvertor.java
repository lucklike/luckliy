package com.luckyframework.httpclient.core.impl;

import java.io.InputStream;

/**
 * 内容编码转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/5 01:12
 */
public interface ContentEncodingConvertor {


    /**
     * 将输入流转换为byte[]
     *
     * @param in 输入流
     * @return byte[]
     */
    byte[] byteConvert(InputStream in);

}
