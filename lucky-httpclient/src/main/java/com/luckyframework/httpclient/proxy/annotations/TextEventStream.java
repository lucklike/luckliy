package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义文本时间流请求
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Timeout
@AutoCloseResponse(false)
@RespConvert("#{$resp$}")
@Combination({RespConvert.class, Timeout.class})
public @interface TextEventStream {

    /**
     * 连接超时时间
     */
    @AliasFor(annotation = Timeout.class, attribute = "connectTimeout")
    int connectionTimeout() default -1;

    /**
     * 连接超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     */
    @AliasFor(annotation = Timeout.class, attribute = "connectTimeoutExp")
    String connectionTimeoutExp() default "";

    /**
     * 读取超时时间
     */
    @AliasFor(annotation = Timeout.class, attribute = "readTimeout")
    int readTimeout() default 600000;

    /**
     * 读取超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     */
    @AliasFor(annotation = Timeout.class, attribute = "readTimeoutExp")
    String readTimeoutExp() default "";

    /**
     * 写超时时间
     */
    @AliasFor(annotation = Timeout.class, attribute = "writeTimeout")
    int writeTimeout() default -1;

    /**
     * 写超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     */
    @AliasFor(annotation = Timeout.class, attribute = "writeTimeoutExp")
    String writeTimeoutExp() default "";

}
