package com.luckyframework.annotations;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 定义一个Component组件
 * @author fk7075
 * @version 1.0.0
 * @date 2020/11/14 1:14 下午
 */
@Target({ElementType.TYPE,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ScannerElement
public @interface Component {
	
	/**
	 * 为该Component组件指定一个唯一ID，默认会使用[首字母小写类名]作为组件的唯一ID
	 * @return 组件的ID
	 */
	@AliasFor(annotation = ScannerElement.class,attribute = "value")
	String value() default "";

}