package com.luckyframework.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定构造器，多个构造器时可以使用这个注解指定使用哪一个
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/4 下午5:10
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Constructor {
}
