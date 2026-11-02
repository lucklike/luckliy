package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.setter.MultipartDataFormParameterSetter;
import com.luckyframework.httpclient.proxy.statics.MultipartFormStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 静态MultipartForm表单参数配置注解。
 * <pre>
 *     用于在接口方法上静态声明{@code multipart/form-data}表单参数，支持三种表单项：
 *     1.文本表单项：{@link #txt()}，由{@link Txt}声明；
 *     2.文件表单项：{@link #file()}，由{@link File}声明，资源内容支持
 *       {@link java.io.File}、{@link org.springframework.core.io.Resource}、
 *       {@link com.luckyframework.io.MultipartFile}、{@link com.luckyframework.httpclient.core.meta.HttpFile}
 *       以及它们的数组和集合；
 *     3.二进制表单项：{@link #binary()}，由{@link Binary}声明。
 *
 *     例如：
 *     {@code
 *     @MultipartFormData(
 *         txt = @Txt(name = "username", value = "#{username}"),
 *         file = @File(name = "avatar", file = "#{avatar}", contentType = "image/png"),
 *         binary = @Binary(name = "data", file = "#{bytes}", filename = "data.bin")
 *     )
 *     String upload();
 *     }
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination({StaticParam.class})
@StaticParam(
        setter = @ObjectGenerate(MultipartDataFormParameterSetter.class),
        resolver = @ObjectGenerate(MultipartFormStaticParamResolver.class)
)
@Repeatable(MultipartFormDatas.class)
public @interface MultipartFormData {

    /**
     * 文本表单项
     */
    Txt[] txt() default {};

    /**
     * 文件表单项
     */
    File[] file() default {};

    /**
     * 二进制表单项
     */
    Binary[] binary() default {};

    /**
     * 条件表达式，只有该表达式为true时，才会进行参数的设置
     */
    String condition() default "";

}
