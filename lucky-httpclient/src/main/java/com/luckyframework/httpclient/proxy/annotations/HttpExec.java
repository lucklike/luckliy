package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.executor.HttpClientExecutor;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.core.executor.JdkHttpExecutor;
import com.luckyframework.httpclient.core.executor.OkHttp3Executor;
import com.luckyframework.httpclient.core.executor.OkHttpExecutor;

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
    ObjectGenerate exec() default @ObjectGenerate(HttpExecutor.class);

    /**
     * 使用Apache Http Client执行器
     */
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @HttpExec(exec = @ObjectGenerate(HttpClientExecutor.class))
    @interface http_client {

    }

    /**
     * 使用OkHttp3执行器
     */
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @HttpExec(exec = @ObjectGenerate(OkHttp3Executor.class))
    @interface okhttp3 {

    }

    /**
     * 使用OkHttp执行器
     */
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @HttpExec(exec = @ObjectGenerate(OkHttpExecutor.class))
    @interface okhttp {

    }

    /**
     * 使用Jdk执行器
     */
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @HttpExec(exec = @ObjectGenerate(JdkHttpExecutor.class))
    @interface jdk {

    }


}
