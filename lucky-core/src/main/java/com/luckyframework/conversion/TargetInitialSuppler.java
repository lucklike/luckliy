package com.luckyframework.conversion;

import java.lang.annotation.*;

/**
 * 配置target对象初始化方法的注解
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/2 08:46
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetInitialSuppler {

    /** target对象初始化的SpEL表达式*/
    String value();

}
