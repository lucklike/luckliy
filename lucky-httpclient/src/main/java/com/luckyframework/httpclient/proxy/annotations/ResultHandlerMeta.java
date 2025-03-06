package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.handle.ResultHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ResultHandlerMeta {

    /**
     * 进行结果处理的类
     */
    Class<? extends ResultHandler> handlerClass() default ResultHandler.class;

    /**
     * 声明用于注册结果处理器的生成器
     */
    ObjectGenerate handler() default @ObjectGenerate(ResultHandler.class);
}
