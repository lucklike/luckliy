package com.luckyframework.httpclient.proxy.spel.var;

import com.luckyframework.common.StringUtils;
import com.luckyframework.reflect.AnnotationUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * 变量注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/14 04:58
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Variate {

    /**
     * 变量名
     */
    String value() default "";

    /**
     * 是否将变量展开
     */
    boolean unfold() default false;

    /**
     * 是否为字面量
     */
    boolean literal() default false;

    /**
     * 变量所用域
     */
    VarScope scope() default VarScope.DEFAULT;

    /**
     * 变量类型
     */
    VarType type() default VarType.NORMAL;


    /**
     * 名称获取工具
     */
    class FieldNameUtils {
        public static String getVarName(Field field) {
            Variate variateAnn = AnnotationUtils.findMergedAnnotation(field, Variate.class);
            if (variateAnn == null || !StringUtils.hasText(variateAnn.value())) {
                return field.getName();
            }
            return variateAnn.value();
        }
    }
}
