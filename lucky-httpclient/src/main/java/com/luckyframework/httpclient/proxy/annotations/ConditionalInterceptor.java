package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.impl.interceptor.RequestAndResponseConditionInterceptor;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@RequestConditionalInterceptor(requestProcessor = RequestAndResponseConditionInterceptor.class)
@ResponseConditionalInterceptor(responseProcessor = RequestAndResponseConditionInterceptor.class)
@Combination({RequestConditionalInterceptor.class, ResponseConditionalInterceptor.class})
public @interface ConditionalInterceptor {

    /**
     * 请求实例必须满足的条件
     */
    String[] request() default {};

    /**
     * 响应实例必须满足的条件
     */
    String[] response() default {};

}
