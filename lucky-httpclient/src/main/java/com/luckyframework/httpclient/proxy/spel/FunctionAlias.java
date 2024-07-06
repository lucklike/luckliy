package com.luckyframework.httpclient.proxy.spel;

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
public @interface FunctionAlias {

    String value();


    class MethodNameUtils {
        public static String getMethodName(Method method) {
            FunctionAlias methodAliasAnn = AnnotationUtils.findMergedAnnotation(method, FunctionAlias.class);
            return methodAliasAnn == null ? method.getName() : methodAliasAnn.value();
        }
    }
}
