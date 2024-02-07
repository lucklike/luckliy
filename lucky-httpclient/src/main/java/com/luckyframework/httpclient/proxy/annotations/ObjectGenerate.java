package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.creator.ObjectFactory;
import com.luckyframework.httpclient.proxy.creator.Scope;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 对象生成器注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ObjectGenerate {

    /**
     * 同{@link #clazz()}
     */
    @AliasFor("clazz")
    Class<?> value() default Void.class;

    /**
     * 对象{@link Class}或对象工厂{@link ObjectFactory}
     */
    @AliasFor("value")
    Class<?> clazz() default Void.class;

    /**
     * 创建对象所需要的额外信息
     */
    String msg() default "";

    /**
     * 对象的作用域
     */
    Scope scope() default Scope.SINGLETON;

}
