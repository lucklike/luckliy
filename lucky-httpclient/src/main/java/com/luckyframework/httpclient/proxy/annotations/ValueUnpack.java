package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.unpack.ContextValueUnpack;
import com.luckyframework.httpclient.proxy.unpack.RepeatableReadStream;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参数拆包注解，将某些包装类型进行拆包操作
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 * @see FactoryUnpack
 * @see RepeatableReadStream
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(ValueUnpacks.class)
public @interface ValueUnpack {

    /**
     * 用于创建{@link ContextValueUnpack}参数拆包器的生成器
     */
    ObjectGenerate valueUnpack() default @ObjectGenerate(ContextValueUnpack.class);

    /**
     * 参数拆包器类型{@link ContextValueUnpack}
     */
    Class<? extends ContextValueUnpack> unpackClass() default ContextValueUnpack.class;

}
