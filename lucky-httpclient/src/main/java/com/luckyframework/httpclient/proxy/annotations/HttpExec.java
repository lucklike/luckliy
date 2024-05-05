package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.executor.HttpClientExecutor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.executor.OkHttp3Executor;
import com.luckyframework.httpclient.core.executor.OkHttpExecutor;
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
     * HTTP执行器类型
     */
    @AliasFor("exec")
    Class<? extends HttpExecutor> value() default HttpExecutor.class;


    /**
     * HTTP执行器类型
     */
    @AliasFor("value")
    Class<? extends HttpExecutor> exec() default HttpExecutor.class;

    /**
     * 使用Apache Http Client执行器
     */
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @HttpExec(HttpClientExecutor.class)
    @interface HttpClient {

    }

    /**
     * 使用OkHttp3执行器
     */
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @HttpExec(OkHttp3Executor.class)
    @interface Okhttp3 {

    }

    /**
     * 使用OkHttp执行器
     */
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @HttpExec(OkHttpExecutor.class)
    @interface Okhttp {

    }

    /**
     * 使用Jdk执行器
     */
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @HttpExec(JdkHttpExecutor.class)
    @interface Jdk {

    }



}
