package com.luckyframework.httpclient.proxy.annotations;

import java.lang.annotation.*;

/**
 * 禁止重定向
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/29 03:59
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedirectProhibition {
}
