package com.luckyframework.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 扫描元素，被该注解标注的类将会被扫描到
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/4 下午11:23
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScannerElement {

    /**
     * 为该扫描元素指定一个唯一ID，默认会使用[首字母小写类名]作为组件的唯一ID
     * @return 扫描元素ID
     */
    String value() default "";
}
