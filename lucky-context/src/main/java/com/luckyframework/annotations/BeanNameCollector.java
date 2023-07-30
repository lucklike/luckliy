package com.luckyframework.annotations;

import java.lang.annotation.*;

/**
 * 类型注入注解
 * @author fk-7075
 */
@Target({ ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD,
		ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BeanNameCollector {

	/**
	 * Declares whether the annotated dependency is required.
	 * Defaults to {@code true}.
	 */
	boolean required() default true;

	String[] exclude() default {};

	Class<?> value();

}
