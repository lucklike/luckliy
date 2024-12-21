package com.luckyframework.httpclient.core.encoder;

import java.io.IOException;
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
     * 输入流转换
     *
     * @param sourceInputStream 原始输入流
     * @return 转换后的输入流
     */
    InputStream inputStreamConvert(InputStream sourceInputStream) throws IOException;

    /**
     * Content-Encoding
     *
     * @return Content-Encoding
     */
    String contentEncoding();

}
