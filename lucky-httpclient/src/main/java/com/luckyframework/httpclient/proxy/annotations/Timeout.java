package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.setter.TimeoutSetter;
import com.luckyframework.httpclient.proxy.statics.TimeoutStaticParamResolver;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 超时时间参数配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination({StaticParam.class})
@StaticParam(
        setter = @ObjectGenerate(TimeoutSetter.class),
        resolver = @ObjectGenerate(TimeoutStaticParamResolver.class)
)
public @interface Timeout {

    //------------------------------------------------------------------
    //                      通用超时时间
    //------------------------------------------------------------------


    /**
     * 连接超时时间
     */
    int connectTimeout() default -1;

    /**
     * 连接超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String connectTimeoutExp() default "";

    /**
     * 读取超时时间
     */
    int readTimeout() default -1;

    /**
     * 读取超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String readTimeoutExp() default "";

    //------------------------------------------------------------------
    //                        OkHttp特有
    //------------------------------------------------------------------

    /**
     * <h3>使用OkHttp执行器时才有效</h3>
     * 写超时时间
     */
    int writeTimeout() default -1;

    /**
     * <h3>使用OkHttp执行器时才有效</h3>
     * 写超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String writeTimeoutExp() default "";

    /**
     * <h3>使用OkHttp执行器时才有效</h3>
     * 整体超时时间
     */
    int callTimeout() default -1;

    /**
     * <h3>使用OkHttp执行器时才有效</h3>
     * 整体超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String callTimeoutExp() default "";

    //------------------------------------------------------------------
    //                        HttpClient特有
    //------------------------------------------------------------------

    /**
     * <h3>使用HttpClient执行器时才有效</h3>
     * 连接获取超时时间
     */
    int connectionRequestTimeout() default -1;

    /**
     * <h3>使用HttpClient执行器时才有效</h3>
     * 连接获取超时时间的SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String connectionRequestTimeoutExp() default "";

}
