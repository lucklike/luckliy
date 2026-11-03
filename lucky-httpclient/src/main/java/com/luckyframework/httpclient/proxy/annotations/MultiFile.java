package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.proxy.dynamic.MultiFileDynamicParamResolver;
import com.luckyframework.httpclient.proxy.setter.StandardHttpFileParameterSetter;
import com.luckyframework.io.MultipartFile;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

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
import java.util.Collection;

/**
 * multipart/form-data参数注解，可以被该注解标注的类型有：
 * <pre>
 *   A.自带文件名称属性的参数类型，这些类型默认使用自带的文件名；
 *     若同时设置了{@link #fileName}，则以{@link #fileName}为准
 *     1.{@link File}、{@link File[]}、{@link Collection&lt;File&gt;}
 *     2.{@link Resource}、{@link Resource[]}、{@link Collection&lt;Resource&gt;}
 *     3.{@link MultipartFile}、{@link MultipartFile[]}、{@link Collection&lt;MultipartFile&gt;}
 *     4.{@link HttpFile}、{@link HttpFile[]}、{@link Collection&lt;HttpFile&gt;}
 *     5.{@link String}类型的资源定位符号，参照{@link ResourceLoader#getResource(String)}
 *          例如：
 *              http://www.baidu.com/img/bd_logo1.png
 *              file:D:/test/test.txt
 *              classpath:file/*.pdf
 *          http/https协议的资源会包装成{@link com.luckyframework.io.HttpResource}，
 *          以便按响应头补全文件名后缀
 *
 *   B.不自带文件名属性的参数类型，可用{@link #fileName}设置文件名，未设置时自动生成随机文件名
 *     a.使用@MultiFile的fileName属性进行设置
 *          1.{@link byte[]}以及他的数组
 *          2.{@link Byte[]}以及他的数组和集合类型
 *          3.{@link InputStream}以及他的数组和集合类型
 *          4.{@link ByteBuffer}、{@link Reader}以及它们的数组和集合类型
 *     b.使用Map传参，利用key来设置文件名（key同样支持SpEL表达式）
 *          1.Map&lt;String, Byte[]>、Map&lt;String, Byte[][]>、Map&lt;String, Collection<Byte[]>>
 *          2.Map&lt;String, InputStream>、Map&lt;String, InputStream[]>、Map&lt;String, Collection<InputStream>>
 *  注:
 *  占位符<b>{_index_}会被替换为数组、集合、Iterator的下标，单个值（非容器类型）的下标固定为0。
 *  例如：
 *      {@code
 *          @Post("/upload")
 *          void upload(@MultiFile(value = "file", fileName = "test{_index_}.txt") InputStream[] in)
 *
 *          upload([in0, in1, in2]);
 *          in0 -> test0.txt
 *          in1 -> test1.txt
 *          in2 -> test2.txt
 *
 *      }
 *
 *  数组、集合、Iterator类型的参数即便为空也会正常发起请求，此时不产生任何文件项。
 * </pre>
 * @see MultiFileDynamicParamResolver
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DynamicParam(
        setter = @ObjectGenerate(StandardHttpFileParameterSetter.class),
        resolver = @ObjectGenerate(MultiFileDynamicParamResolver.class)
)
public @interface MultiFile {

    /**
     * 参数名称
     */
    @AliasFor(annotation = DynamicParam.class, attribute = "name")
    String value() default "";

    /**
     * 参数名称
     */
    @AliasFor(annotation = DynamicParam.class, attribute = "name")
    String name() default "";

    /**
     * 文件名称
     */
    String fileName() default "";

    /**
     * 文件的Content-Type，支持SpEL表达式
     * <pre>
     *     为空时按以下顺序确定：
     *     1.内容自带的Content-Type（如{@link MultipartFile}、{@link HttpFile}）；
     *     2.根据文件名推断；
     *     3.回退为{@code application/octet-stream}。
     *     不为空时统一覆盖本次所有文件的Content-Type。
     * </pre>
     */
    String contentType() default "";

}
