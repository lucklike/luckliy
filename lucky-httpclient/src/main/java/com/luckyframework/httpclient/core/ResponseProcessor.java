package com.luckyframework.httpclient.core;

import java.io.IOException;
import java.io.InputStream;

/**
 * 响应处理器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/6/18 16:17
 */
@FunctionalInterface
public interface ResponseProcessor {

    void process(int status,HttpHeaderManager header, InputStream result) throws IOException;

}
