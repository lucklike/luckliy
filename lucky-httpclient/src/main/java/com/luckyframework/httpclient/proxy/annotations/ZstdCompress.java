package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.impl.ZstdContentEncodingConvertor;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Zstd压缩，使用此功能需要引入以下依赖：
 * <pre>
 * {@code
 *  <dependency>
 *       <groupId>com.github.luben</groupId>
 *       <artifactId>zstd-jni</artifactId>
 *       <version>${version}</version>
 *   </dependency>
 * }
 * </pre>
 *
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/5 02:07
 * @see ZstdContentEncodingConvertor
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination(StaticHeader.class)
@StaticHeader("Accept-Encoding=zstd")
public @interface ZstdCompress {
}
