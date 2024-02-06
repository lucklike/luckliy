package com.luckyframework.httpclient.proxy.creator;

/**
 * 对象工厂信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/6 23:30
 */
@FunctionalInterface
public interface ObjectFactory {

    /**
     * 创建Object实例
     * @param msg 额外信息
     * @return Object实例
     */
    Object createObject(String msg);
}
