package com.luckyframework.annotations;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 注册一个Component组件
 * @author fk-7075
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

	@AliasFor("name")
	String value() default "";

	@AliasFor("value")
	String name() default "";

	boolean autowireCandidate() default true;

	String[] initMethod() default {};

	String[] destroyMethod() default {};

}
