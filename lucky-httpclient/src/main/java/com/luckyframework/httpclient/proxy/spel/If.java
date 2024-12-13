package com.luckyframework.httpclient.proxy.spel;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 条件注入
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/9 01:37
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface If {

    /**
     * 条件
     */
    String condition();

    /**
     * 条件成立时才会注入这里的class
     */
    Class<?>[] classes();
}
