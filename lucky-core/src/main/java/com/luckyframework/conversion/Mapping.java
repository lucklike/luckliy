package com.luckyframework.conversion;

import java.lang.annotation.*;

/**
 * 配置类型转化映射关系的注解
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/29 08:53
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Mappings.class)
public @interface Mapping {

    /** 目标属性名*/
    String  target();

    /** 原属性名或SpEL表达式*/
    String source();


}
