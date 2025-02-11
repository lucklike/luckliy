package com.luckyframework.httpclient.proxy.stream;

import com.luckyframework.httpclient.proxy.stream.sse.AnnotationSseEventListener;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义消息方法的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/19 01:10
 * @see AnnotationSseEventListener
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface OnMessage {

    /**
     * 执行条件，默认为true
     * <pre>
     *     {@link AnnotationSseEventListener}在初始化时会搜集所有被{@link OnMessage @OnMessage}注解标注的方法
     *     当监听到<b>onMessage</b>事件时{@link AnnotationSseEventListener}会将事件通过@OnMessage注解的{@link OnMessage#value()}
     *     属性配置的SpEL表达式的执行结果来路由到对应的方法，方法的查找顺序为方法在类中的定义顺序，当找到并执行某一个方法后则会立即结束事件
     * </pre>
     */
    String value() default "true";

}
