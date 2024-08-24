package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.convert.ResponseConvert;
import com.luckyframework.httpclient.proxy.sse.SseResultConvert;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 响应结果转换器注解
 *
 * @see SseResultConvert
 * @see RespConvert
 * @see ConvertProhibition
 * @see DownloadToLocal
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ConvertMetaType
public @interface ResultConvert {

    /**
     * 响应结果转换器生成器，用于生成{@link ResponseConvert}对象的生成器
     */
    ObjectGenerate convert();

    /**
     * 转换元类型
     */
    @AliasFor(annotation = ConvertMetaType.class, attribute = "value")
    Class<?> metaType() default Object.class;

}
