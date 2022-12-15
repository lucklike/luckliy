package com.luckyframework.proxy.cache.annotations;

import com.luckyframework.cache.CacheManager;
import com.luckyframework.proxy.cache.KeyGenerator;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 缓存注解
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/13 11:30
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@CacheMetadata
@Repeatable(Cacheables.class)
public @interface Cacheable {


    /** 缓存空间*/
    @AliasFor("cacheNames")
    String[] value() default {};

    /** 缓存空间*/
    @AliasFor("value")
    String[] cacheNames() default {};

    /** 缓存Key的生成规则，支持SpEL表达式*/
    String key() default "";

    /** 缓存Key的生成规则,这里需要配置{@link KeyGenerator}类型实例的Bean的名称*/
    String keyGenerator() default "";

    /** 当缓存空间不存在时，会初始化该类型的缓存空间*/
    String cacheType() default "";

    /** 缓存管理器配置，需要配置{@link CacheManager} 类型实例的Bean的名称*/
    String cacheManager() default "";

    /** 只有当该条件成立时，方法的执行结果才会被存入缓存中*/
    String condition() default "";

    /** 只有当该条件成立时，方法的执行结果将不会被存入缓存中，与{@link #condition()}不同的
     * 时unless可以使用方法执行结果进行判断(#result)*/
    String unless() default "";

    /** 缓存查找过程是否串行化*/
    boolean sync() default false;

    String[] spelImport() default {};
}
