package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.dynamic.StandardBinaryBodyDynamicParamResolver;
import com.luckyframework.httpclient.proxy.setter.BodyParameterSetter;
import com.luckyframework.reflect.Combination;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;

import static com.luckyframework.httpclient.proxy.dynamic.StandardBinaryBodyDynamicParamResolver.DEFAULT_MIME_TYPE;

/**
 * 二进制请求体参数注解
 * <pre>
 *   支持的参数类型如下：
 *      {@link byte[]}
 *      {@link Byte[]}
 *      {@link ByteBuffer}
 *      {@link Reader}
 *      {@link InputStream}
 *      {@link File}
 *      {@link InputStreamSource}
 *    如果参数不是以上类型，则会尝试使用{@code  ConversionUtils.conversion(object, Resource.class)}
 *    方法将参数转化为{@link Resource}类型
 *
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/11 16:48
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DynamicParam(
        setter = @ObjectGenerate(BodyParameterSetter.class),
        resolver = @ObjectGenerate(StandardBinaryBodyDynamicParamResolver.class)
)
@Combination(DynamicParam.class)
public @interface BinaryBody {

    /**
     * mimeType
     */
    String mimeType() default DEFAULT_MIME_TYPE;

    /**
     * charset
     */
    String charset() default "";

}
