package com.luckyframework.conversion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在{@link Interconversion}接口的子接口方法中，表示将单个方法转换器
 * 注册到{@link ConversionManager}中
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/29 08:53
 */
@Target({ElementType.METHOD,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConversionRegister {

    /** 转换器的名字*/
    String value();

}
