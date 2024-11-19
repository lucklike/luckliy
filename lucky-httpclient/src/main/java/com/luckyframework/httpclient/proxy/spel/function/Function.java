package com.luckyframework.httpclient.proxy.spel.function;

import com.luckyframework.reflect.AnnotationUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * 为公共静态方法声明一个别名
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/14 04:58
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Function {

    String value();


    class MethodNameUtils {
        public static String getMethodName(Method method) {
            Function methodAliasAnn = AnnotationUtils.findMergedAnnotation(method, Function.class);
            return methodAliasAnn == null ? method.getName() : methodAliasAnn.value();
        }
    }
}
