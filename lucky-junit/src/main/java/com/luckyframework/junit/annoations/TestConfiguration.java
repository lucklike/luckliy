package com.luckyframework.junit.annoations;

import java.lang.annotation.*;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/9/18 2:54 下午
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestConfiguration {

    Class<?>[] rootClasses() default {};

    String[] basePackages() default {};
}
