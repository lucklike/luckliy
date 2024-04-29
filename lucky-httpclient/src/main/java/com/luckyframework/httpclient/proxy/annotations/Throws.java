package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.convert.ThrowsExceptionResponseConvert;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于抛出异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/29 9:48
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination(ResultConvert.class)
@ResultConvert(convert = @ObjectGenerate(ThrowsExceptionResponseConvert.class) )
public @interface Throws {

    /**
     * 异常描述注解
     */
    Ex[] value();

    /**
     * 转换元类型
     */
    @AliasFor(annotation = ResultConvert.class, attribute = "metaType")
    Class<?> metaType() default Object.class;

    /**
     * 是否导入响应实例{@link Response}
     */
    @AliasFor(annotation = ResultConvert.class, attribute = "importRespInstance")
    boolean importRespInstance() default true;

    /**
     * 是否导入响应体
     */
    @AliasFor(annotation = ResultConvert.class, attribute = "importBody")
    boolean importBody() default true;

    /**
     * 是否导入响应头
     */
    @AliasFor(annotation = ResultConvert.class, attribute = "importHeader")
    boolean importHeader() default true;
}
