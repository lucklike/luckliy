package com.luckyframework.annotations;

import org.springframework.core.annotation.AliasFor;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 插件,被该注解标注的Class将被视为插件，插件不会被实例化
 * 所有插件将以{@link AnnotationMetadata}的形式保存在容器中
 *
 * @author fk7075
 * @version 1.0
 * @date 2020/11/18 8:56
 */
@Target({ElementType.TYPE,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ScannerElement
public @interface Plugin {

    @AliasFor(annotation = ScannerElement.class,attribute = "value")
    String value() default "";

}
