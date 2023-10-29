package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.common.EncryptionUtils;
import com.luckyframework.httpclient.proxy.SpecialOperationFunction;
import com.luckyframework.httpclient.proxy.impl.dynamic.URLEncoderUtils;
import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.core.annotation.AliasFor;
import org.springframework.util.Base64Utils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.Charset;

/**
 * Base64编码注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpecialOperation(Base64Encoder.Base64EncoderFunction.class)
public @interface Base64Encoder {

    @AliasFor(annotation = SpecialOperation.class, attribute = "enable")
    boolean value() default true;

    String charset() default "UTF-8";

    class Base64EncoderFunction implements SpecialOperationFunction {

        @Override
        public Object change(String originalName, Object originalValue, Annotation specialAnn) {
            return EncryptionUtils.base64Encode(String.valueOf(originalValue), Charset.forName(AnnotationUtils.getValue(specialAnn, "charset", String.class)));
        }
    }
}
