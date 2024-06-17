package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.encoder.InflaterContentEncodingConvertor;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Deflate压缩
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/5 02:07
 * @see InflaterContentEncodingConvertor
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination(StaticHeader.class)
@StaticHeader("[SET]Accept-Encoding: deflate")
public @interface DeflateCompress {
}
