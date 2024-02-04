package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpecialOperationFunction;
import com.luckyframework.httpclient.proxy.dynamic.URLEncoderUtils;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * URL编码注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpecialOperation(URLEncoder.URLEncoderFunction.class)
public @interface URLEncoder {

    @AliasFor(annotation = SpecialOperation.class, attribute = "enable")
    boolean value() default true;

    String charset() default "UTF-8";
    class URLEncoderFunction implements SpecialOperationFunction {

        @Override
        public Object change(String originalName, Object originalValue, Annotation specialAnn) {
            return URLEncoderUtils.encode(originalValue, AnnotationUtils.getValue(specialAnn, "charset", String.class));
        }
    }
}
