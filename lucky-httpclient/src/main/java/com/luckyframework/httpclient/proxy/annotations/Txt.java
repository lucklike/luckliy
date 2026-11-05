package com.luckyframework.httpclient.proxy.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 静态MultipartForm表单的文本表单项配置注解。
 * <pre>
 *     配合{@link MultipartFormData}的{@code txt}属性使用，在接口方法上静态声明一个
 *     multipart/form-data文本表单项：{@link #value()}表达式解析出内容，{@link #name()}是表单字段名。
 *     例如：
 *     {@code
 *     @MultipartFormData(txt = {
 *         @Txt(name = "username", value = "Jack"),
 *         @Txt(name = "age", value = "#{age}")
 *     })
 *     boolean upload();
 *     }
 *
 *     文本表单项的Content-Type固定为{@code text/plain;charset=UTF-8}。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/11/2
 * @see MultipartFormData
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Txt {

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
     *     表达式的结果必须是字符串，即服务端收到的这个文本项的参数名。
     * </pre>
     *
     * @return 表单字段名表达式
     */
    String name();

    /**
     * 文本内容表达式，支持SpEL表达式
     * <pre>
     *     表达式的结果会经{@link String#valueOf(Object)}转成字符串后写入表单。
     * </pre>
     *
     * @return 内容表达式
     */
    String value();
}
