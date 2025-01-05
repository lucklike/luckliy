package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.meta.HttpFile;
import com.luckyframework.httpclient.proxy.setter.MultipartDataFormParameterSetter;
import com.luckyframework.httpclient.proxy.statics.MultipartFormStaticParamResolver;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.reflect.Combination;
import org.springframework.core.io.Resource;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

/**
 * 静态MultipartForm表单参数配置注解
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
     * 文本参数
     */
    String[] txt() default {};

    /**
     * 资源表达式
     * <pre>
     * 支持的类型有：
     *      1.{@link File}、{@link File[]}、{@link Collection&lt;File&gt;}
     *      2.{@link Resource}、{@link Resource[]}、{@link Collection&lt;Resource&gt;}
     *      3.{@link MultipartFile}、{@link MultipartFile[]}、{@link Collection&lt;MultipartFile&gt;}
     *      4.{@link HttpFile}、{@link HttpFile[]}、{@link Collection&lt;HttpFile&gt;}
     * </pre>
     */
    String[] file() default {};

    /**
     * 二进制参数
     */
    Binary[] binary() default {};

    /**
     * 属性名与属性值之间的分隔符
     */
    String separator() default "=";

    /**
     * 条件表达式，只有该表达式为true时，才会进行参数的设置
     */
    String condition() default "";

}
