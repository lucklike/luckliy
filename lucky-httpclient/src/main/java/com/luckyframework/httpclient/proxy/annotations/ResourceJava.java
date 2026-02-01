package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.setter.JavaFlatBeanParameterSetter;
import com.luckyframework.httpclient.proxy.setter.JsonFlatBeanParameterSetter;
import com.luckyframework.httpclient.proxy.statics.ResourceJavaObjectResolver;
import com.luckyframework.httpclient.proxy.statics.ResourceJsonObjectResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 从环境变量中提取JAVA对象请求体的解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/19 18:30
 * @see JavaParam
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@StaticParam(
        setter = @ObjectGenerate(JavaFlatBeanParameterSetter.class),
        resolver = @ObjectGenerate(ResourceJavaObjectResolver.class)
)
@Combination(StaticParam.class)
public @interface ResourceJava {

    /**
     * 文件的路径
     * <pre>
     *  支持的文件类型有：
     *     1.properties文件
     *     2.yml文件
     *     3.yaml文件
     *     4.json文件
     *     5.xml文件（<![CDATA[<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd>]]>）
     * </pre>
     */
    String value();

    /**
     * 文件的编码方式
     */
    String charset() default "UTF-8";

}
