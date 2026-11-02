package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.io.MultipartFile;
import org.springframework.core.io.Resource;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;

/**
 * 静态MultipartForm表单的文件表单项配置注解。
 * <pre>
 *     配合{@link MultipartFormData}的{@code file}属性使用，在接口方法上静态声明一个
 *     multipart/form-data文件表单项：{@link #file()}表达式解析出资源内容，{@link #name()}是表单字段名。
 *     例如：
 *     {@code
 *     @MultipartFormData(file = @File(name = "avatar", file = "#{avatar}", contentType = "image/png"))
 *     boolean upload();
 *     }
 *
 *     {@link #file()}的解析结果必须是资源类型参数，支持的形态有
 *     {@link java.io.File}、{@link Resource}、{@link MultipartFile}、{@link HttpFile}
 *     以及它们的数组和集合。
 *
 *     注意：{@link #filename()}留空时使用内容自带的文件名，{@link #contentType()}留空时
 *     按内容自带的Content-Type或文件名推断。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/11/2
 * @see MultipartFormData
 * @see Binary
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface File {

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
     * 资源内容表达式，支持SpEL表达式
     * <pre>
     *     表达式的结果必须是资源类型参数（能否使用由{@link com.luckyframework.httpclient.core.executor.HttpExecutor#isResourceParam(Object)}判定），
     *     支持{@link java.io.File}、{@link Resource}、{@link MultipartFile}、{@link HttpFile}
     *     以及它们的数组和集合。
     * </pre>
     *
     * @return 内容表达式
     */
    String file();

    /**
     * 上报给服务端的文件名，支持SpEL表达式
     * <pre>
     *     留空时使用内容自带的文件名；
     *     不为空时覆盖内容自带的名称。
     * </pre>
     *
     * @return 文件名表达式
     */
    String filename() default "";

    /**
     * 上报给服务端的文件Content-Type，支持SpEL表达式
     * <pre>
     *     留空时按以下顺序确定：
     *     1.内容自带的Content-Type（如{@link MultipartFile}、{@link HttpFile}）；
     *     2.根据文件名推断；
     *     3.回退为{@code application/octet-stream}。
     * </pre>
     *
     * @return Content-Type表达式
     */
    String contentType() default "";
}
