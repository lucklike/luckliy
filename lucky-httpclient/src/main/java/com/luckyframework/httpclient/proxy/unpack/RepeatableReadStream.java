package com.luckyframework.httpclient.proxy.unpack;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ValueUnpack;
import com.luckyframework.httpclient.proxy.destroy.DestroyMeta;
import com.luckyframework.reflect.Combination;

import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 输入流转可重复读取输入流的转换器注解
 *
 * <pre>
 *     支持的包装类型有：
 *     {@link InputStream}
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 * @see RepeatableReadStreamConvertUnpack
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination({ValueUnpack.class, DestroyMeta.class})
@DestroyMeta(destroyHandle = @ObjectGenerate(RepeatableReadStreamDestroyHandle.class))
@ValueUnpack(valueUnpack = @ObjectGenerate(RepeatableReadStreamConvertUnpack.class))
public @interface RepeatableReadStream {

    /**
     * 转换的流类型
     * BYTE_ARRAY: 以byte数组为存储介质 （默认）
     * LOCAL_FILE: 以本地文件为存储介质
     */
    StreamType value() default StreamType.BYTE_ARRAY;

    /**
     * 此配置仅在value为LOCAL_FILE时生效，指定存储介质文件的路径
     */
    String storeDir() default "";

}
