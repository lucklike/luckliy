package com.luckyframework.httpclient.core;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 13:54
 */
@FunctionalInterface
public interface InputStreamFactory {

    InputStream getInputStream() throws IOException;
}
