package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.io.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 静态二进制表单项配置注解。
 * <pre>
 *     配合{@link MultipartFormData}的{@code binary}属性使用，在接口方法上静态声明一个
 *     multipart/form-data文件表单项：{@link #file()}表达式解析出内容，{@link #name()}是表单字段名，
 *     {@link #filename()}是上报给服务端的文件名，与动态入参（{@code @MultiFile}参数注解）互为补充。
 *     例如：
 *     {@code
 *     @MultipartFormData(binary = @Binary(name = "file", file = "#{bytes}", filename = "data.bin"))
 *     boolean upload();
 *     }
 *
 *     {@link #file()}的解析结果必须是二进制类型参数，由
 *     {@link HttpExecutor#isBinaryParam(Object)}统一判定，支持的形态有：
 *     1.{@code byte[]}、{@code Byte[]}、{@code ByteBuffer}、{@code Reader}——转成字节后包装；
 *     2.{@link InputStream}、{@link org.springframework.core.io.InputStreamSource}系
 *       （含{@link Resource}、{@link MultipartFile}）——按流包装；
 *     3.{@link File}与{@link HttpFile}——走资源构造器，保留流式读取；
 *     其他类型（如String、数字）会在解析阶段抛出{@link IllegalArgumentException}。
 *
 *     注意：{@code byte[]}/{@code InputStream}这类纯二进制内容本身不自带文件名，
 *     必须通过{@link #filename()}给出上报给服务端的文件名，否则服务端无法把它识别为文件项。
 * </pre>
 *
 * @author fukang
 * @version 1.1.0
 * @date 2024/12/9 01:37
 * @see MultipartFormData
 * @see HttpFile
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Binary {

    /**
     * 条件表达式，支持SpEL
     * <pre>
     *     表达式的结果必须是布尔值，只有结果为true时才会注入本表单项；
     *     留空表示无条件注入。
     * </pre>
     *
     * @return 条件表达式
     */
    String condition() default "";

    /**
     * 表单字段名，支持SpEL表达式
     * <pre>
     *     表达式的结果必须是字符串，即服务端收到的这个文件项的参数名。
     * </pre>
     *
     * @return 表单字段名表达式
     */
    String name();

    /**
     * 二进制内容表达式，支持SpEL表达式
     * <pre>
     *     表达式的结果必须是二进制类型参数（能否使用由{@link HttpExecutor#isBinaryParam(Object)}判定），
     *     支持{@code byte[]}、{@code Byte[]}、{@code ByteBuffer}、{@code Reader}、{@code InputStream}、
     *     {@code InputStreamSource}（含{@link Resource}、{@link MultipartFile}）、{@link File}、{@link HttpFile}；
     *     命中后统一包装成携带{@link #filename()}文件名的{@link HttpFile}写入表单。
     * </pre>
     *
     * @return 内容表达式
     */
    String file();

    /**
     * 上报给服务端的文件名，支持SpEL表达式
     * <pre>
     *     包装成{@link HttpFile}时覆盖内容自带的名称；
     *     {@code byte[]}/{@code InputStream}等不自带文件名的内容必须在此给出。
     * </pre>
     *
     * @return 文件名表达式
     */
    String filename();

}
