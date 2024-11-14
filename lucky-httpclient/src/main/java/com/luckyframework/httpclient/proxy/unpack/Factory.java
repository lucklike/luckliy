package com.luckyframework.httpclient.proxy.unpack;

/**
 * 工厂接口
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/16 20:06
 */
@FunctionalInterface
public interface Factory<T> {

    T create();

}
