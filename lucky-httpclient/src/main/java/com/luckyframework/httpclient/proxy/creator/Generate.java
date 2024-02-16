package com.luckyframework.httpclient.proxy.creator;

import com.luckyframework.httpclient.proxy.context.Context;

/**
 * 对象生成器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/16 19:54
 */
@FunctionalInterface
public interface Generate<T> {

    /**
     * 用于生成目标对象
     * @param context 上下文对象
     * @return 目标对象
     */
    T create(Context context);

}
