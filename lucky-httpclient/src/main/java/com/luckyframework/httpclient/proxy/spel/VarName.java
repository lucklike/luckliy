package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.StringUtils;
import com.luckyframework.reflect.AnnotationUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * 普通变量
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/14 04:58
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface VarName {

    String value() default "";

    class FieldNameUtils {
        public static String getVarName(Field field) {
            VarName varNameAnn = AnnotationUtils.findMergedAnnotation(field, VarName.class);
            if (varNameAnn == null || !StringUtils.hasText(varNameAnn.value())) {
                return field.getName();
            }
            return varNameAnn.value();
        }
    }
}
