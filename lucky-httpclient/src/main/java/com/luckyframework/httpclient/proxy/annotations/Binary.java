package com.luckyframework.httpclient.proxy.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 二进制
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/9 01:37
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Binary {

    /**
     * 条件
     */
    String condition();

    /**
     * 条件成立时才会注入这里的class
     */
    String name();

    /**
     * 资源表达式
     */
    String file();

    /**
     * 文件名称
     */
    String filename();

}
