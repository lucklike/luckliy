package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.reflect.AnnotationUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * 静态方法别名
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/14 04:58
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface StaticMethodAlias {

    String value();


    class MethodNameUtils {
        public static String getMethodName(Method method) {
            StaticMethodAlias methodAliasAnn = AnnotationUtils.findMergedAnnotation(method, StaticMethodAlias.class);
            return methodAliasAnn == null ? method.getName() : methodAliasAnn.value();
        }
    }
}
