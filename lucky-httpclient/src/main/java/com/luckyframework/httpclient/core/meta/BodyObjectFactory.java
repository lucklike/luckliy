package com.luckyframework.httpclient.core.meta;

import org.springframework.lang.NonNull;

/**
 * 请求体对象工厂
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/13 01:59
 */
public interface BodyObjectFactory {

    /**
     * 创建{@link BodyObject}对象
     *
     * @return BodyObject对象
     */
    @NonNull
    BodyObject create();
}
