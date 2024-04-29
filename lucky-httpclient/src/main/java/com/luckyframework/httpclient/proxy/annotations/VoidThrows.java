package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.convert.VoidThrowsExceptionResponseConvert;
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
@ResultConvert(convert = @ObjectGenerate(VoidThrowsExceptionResponseConvert.class) )
public @interface VoidThrows {

    /**
     * 异常描述注解
     */
    Ex[] value();

    /**
     * 是否导入响应实例{@link VoidResponse}
     */
    @AliasFor(annotation = VoidResultConvert.class, attribute = "importVoidRespInstance")
    boolean importVoidRespInstance() default true;

    /**
     * 是否导入响应体
     */
    @AliasFor(annotation = VoidResultConvert.class, attribute = "importBody")
    boolean importBody() default true;

    /**
     * 是否导入响应头
     */
    @AliasFor(annotation = VoidResultConvert.class, attribute = "importHeader")
    boolean importHeader() default true;
}
