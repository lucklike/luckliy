package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.unpack.Factory;
import com.luckyframework.httpclient.proxy.unpack.FactoryObjectContentValueUnpack;
import com.luckyframework.reflect.Combination;
import com.luckyframework.spel.LazyValue;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Future参数拆包注解
 *
 * <pre>
 *     支持的包装类型有：
 *     {@link Factory}
 *     {@link LazyValue}
 *     {@link Future}
 *     {@link Supplier}
 *     {@link Callable}
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 * @see FactoryObjectContentValueUnpack
 * @see HttpRequest
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination(ValueUnpack.class)
@ValueUnpack(valueUnpack = @ObjectGenerate(FactoryObjectContentValueUnpack.class))
public @interface FactoryUnpack {

}
