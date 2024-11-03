package com.luckyframework.httpclient.generalapi.token;

/**
 * 基于内存存储的Token管理器
 * <pre>
 *     该Token管理器不会持久化Token信息，而是将Token保存在内存中
 *     每次程序重新启动时都将会去调用获取Token的接口来获取Token信息
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/18 16:48
 */
public abstract class MemoryTokenManager<T> extends TokenManager<T> {

    @Override
    protected void saveToken(T token) {
        // 不用保存，直接存到内存中
    }

    @Override
    protected T getCachedToken() {
        // 直接返回内存中的Token
        return token;
    }
}
