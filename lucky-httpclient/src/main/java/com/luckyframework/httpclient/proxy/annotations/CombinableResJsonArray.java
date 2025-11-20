package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.setter.JsonArrayBodyFactoryParameterSetter;
import com.luckyframework.httpclient.proxy.statics.ResourceJsonArrayObjectResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 从环境变量中提取JSON对象请求体的解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/19 18:30
 * @see CombineJson
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@StaticParam(
        setter = @ObjectGenerate(JsonArrayBodyFactoryParameterSetter.class),
        resolver = @ObjectGenerate(ResourceJsonArrayObjectResolver.class)
)
@Combination(StaticParam.class)
public @interface CombinableResJsonArray {

    /**
     * 指定环境变量key
     */
    String value();

    /**
     * 数组前缀
     */
    String prefix() default "$";

    /**
     * 文件的编码方式
     */
    String charset() default "UTF-8";

}
