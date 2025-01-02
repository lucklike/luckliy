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
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

/**
 * multipart/form-data参数注解，可以被该注解标注的类型有：
 * <pre>
 *   A.自带文件名称属性的参数类型，使用这些类型时设置{@link #fileName}是无效的
 *     1.{@link File}、{@link File[]}、{@link Collection&lt;File&gt;}
 *     2.{@link Resource}、{@link Resource[]}、{@link Collection&lt;Resource&gt;}
 *     3.{@link MultipartFile}、{@link MultipartFile[]}、{@link Collection&lt;MultipartFile&gt;}
 *     4.{@link HttpFile}、{@link HttpFile[]}、{@link Collection&lt;HttpFile&gt;}
 *     5.{@link String}类型的资源定位符号，参照{@link ResourceLoader#getResource(String)}
 *          例如：
 *              http://www.baidu.com/img/bd_logo1.png
 *              file:D:/test/test.txt
 *              classpath:file/*.pdf
 *
 *   B.不自带文件名属性的参数类型，使用这些类型时必须提供{@link #fileName}属性
 *     a.使用@MultiFile的fileName属性进行设置
 *          1.{@link byte[]}以及他的数组
 *          2.{@link Byte[]}以及他的数组和集合类型
 *          3.{@link InputStream}以及他的数组和集合类型
 *     b.使用Map传参，利用key来设置文件名
 *          1.Map&lt;String, Byte[]>、Map&lt;String, Byte[][]>、Map&lt;String, Collection<Byte[]>>
 *          2.Map&lt;String, InputStream>、Map&lt;String, InputStream[]>、Map&lt;String, Collection<InputStream>>
 *  注:
 *  在使用以上类型的数组或者集合时{@link #fileName}属性中可以使用占位符<b>{_index_}
 *  实际生成的文件名中会被替换为数组或者集合的下标。
 *  例如：
 *      {@code
 *          @Post("/upload")
 *          void upload(@MultiFile("fileName=test{_index_}.txt") InputStream[] in)
 *
 *          upload([in0, in1, in2]);
 *          in0 -> test0.txt
 *          in1 -> test1.txt
 *          in2 -> test2.txt
 *
 *      }
 *
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

}
