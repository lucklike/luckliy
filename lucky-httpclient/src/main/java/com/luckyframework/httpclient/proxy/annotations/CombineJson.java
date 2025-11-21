package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.dynamic.JsonParamDynamicParamResolver;
import com.luckyframework.httpclient.proxy.setter.JsonPropertyParameterSetter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 可组合的JSON参数，可以配合{@link CombinablePropJson}、{@link CombinablePropJsonArray}系列注解来组合使用
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 *
 * @see CombinablePropJson
 * @see CombinablePropJsonArray
 * @see CombinableResJson
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DynamicParam(
        setter = @ObjectGenerate(JsonPropertyParameterSetter.class),
        resolver = @ObjectGenerate(clazz = JsonParamDynamicParamResolver.class)
)
public @interface CombineJson {

    /**
     * 参数名称
     */
    @AliasFor(annotation = DynamicParam.class, attribute = "name")
    String value() default "";

    /**
     * 是否将结果展开
     * 如果方法返回值为对象或者Map时需要将结果展开之后存储时可以将此属性设置为true
     */
    boolean unfold() default false;

}
