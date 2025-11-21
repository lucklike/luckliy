package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.setter.MapParameterSetter;
import com.luckyframework.httpclient.proxy.statics.ResourceLocationJsonObjectResolver;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

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
        setter = @ObjectGenerate(MapParameterSetter.class),
        resolver = @ObjectGenerate(ResourceLocationJsonObjectResolver.class)
)
@Location(value = "")
@Combination({StaticParam.class, Location.class})
public @interface CombinableResJson {

    /**
     * 文件的路径
     */
    @AliasFor(annotation = Location.class, attribute = "value")
    String value();

    /**
     * 文件的路径
     */
    @AliasFor(annotation = Location.class, attribute = "array")
    String array() default "";

    /**
     * 文件的编码方式
     */
    String charset() default "UTF-8";

}
