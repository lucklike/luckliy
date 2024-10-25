package com.luckyframework.conversion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于定义一个方法转换器，被该注解标注的方法将会被封装成一个{@link ConversionService}实例
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/29 08:53
 */
@Target({ElementType.METHOD,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConversionMethod {

    String value() default "" ;

    String name() default "" ;

}
