package com.luckyframework.httpclient.generalapi.token;

/**
 * 基于内存存储的Token管理器
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
