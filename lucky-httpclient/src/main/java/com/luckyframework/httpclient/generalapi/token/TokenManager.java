package com.luckyframework.httpclient.generalapi.token;

import org.springframework.lang.Nullable;

/**
 * Token管理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/18 16:48
 */
public abstract class TokenManager<T> {

    private T token;

    /**
     * 获取最新的Token数据
     *
     * @return 最新的Token数据
     */
    public synchronized T getToken() {
        if (token == null) {
            token = getCachedToken();
            if (token == null || isExpires(token)) {
                refreshSaveToken();
            }
        } else if (isExpires(token)) {
            refreshSaveToken();
        }
        return token;
    }

    /**
     * 刷新并保存Token数据
     */
    private void refreshSaveToken() {
        token = refreshToken(token);
        saveToken(token);
    }

    /**
     * 获取缓存下来的Token数据
     *
     * @return 存下来的Token数据
     */
    protected abstract T getCachedToken();

    /**
     * 刷新Token
     *
     * @param oldToken 旧的Token数据
     * @return 刷新后的最新Token数据
     */
    protected abstract T refreshToken(@Nullable T oldToken);

    /**
     * 保存Token数据
     *
     * @param token Token数据
     */
    protected abstract void saveToken(T token);

    /**
     * 检查Token是否已经过期
     *
     * @param token Token数据
     * @return 是否已经过期
     */
    protected abstract boolean isExpires(T token);

}
