package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.executor.HttpClient5Executor;
import com.luckyframework.httpclient.core.executor.HttpClientExecutor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.executor.OkHttpExecutor;
import com.luckyframework.httpclient.core.meta.Version;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于指定Http执行器的注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface HttpExec {

    /**
     * HTTP执行器生成器
     */
    ObjectGenerate execGenerate() default @ObjectGenerate(HttpExecutor.class);

    /**
     * HTTP执行器Class
     */
    Class<? extends HttpExecutor> execClass() default HttpExecutor.class;

    /**
     * 获取HTTP执行器的SpEL表达式
     * <pre>
     *     1.如果表达式结果类型为{@link HttpExecutor}时直接使用该执行器
     *     2.如果表达式结果类型为{@link String}时，使用{@link HttpClientProxyObjectFactory#getAlternativeHttpExecutor(String)}来获取执行器
     * </pre>
     */
    String exec() default "";

    /**
     * 指定用于生成HTTP执行器的函数
     * <pre>
     *     函数的返回值类型必须为{@link HttpExecutor}
     * </pre>
     */
    String execFunc() default "";


    /**
     * 使用Apache Http Client5 执行器
     */
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @HttpVersion
    @HttpExec(execGenerate = @ObjectGenerate(HttpClient5Executor.class))
    @Combination({HttpVersion.class, HttpExec.class})
    @interface http_client5 {
        /**
         * 设置 HTTP 的版本
         */
        @AliasFor(annotation = HttpVersion.class, attribute = "value")
        Version value() default Version.NON;
    }

    /**
     * 使用Apache Http Client执行器
     */
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @HttpExec(execGenerate = @ObjectGenerate(HttpClientExecutor.class))
    @interface http_client {

    }

    /**
     * 使用OkHttp执行器
     */
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @HttpVersion
    @HttpExec(execGenerate = @ObjectGenerate(OkHttpExecutor.class))
    @Combination({HttpVersion.class, HttpExec.class})
    @interface okhttp {
        /**
         * 设置 HTTP 的版本
         */
        @AliasFor(annotation = HttpVersion.class, attribute = "value")
        Version value() default Version.NON;
    }

    /**
     * 使用Jdk执行器
     */
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @HttpExec(execGenerate = @ObjectGenerate(JdkHttpExecutor.class))
    @interface jdk {

    }


}
