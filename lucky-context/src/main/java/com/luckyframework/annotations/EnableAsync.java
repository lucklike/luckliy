package com.luckyframework.annotations;

import com.luckyframework.proxy.async.AsyncBeanFactoryPostProcessor;

import java.lang.annotation.*;

/**
 * 启用{@link Async @Async}异步方法执行功能
 * @author fk7075
 * @version 1.0
 * @date 2021/9/7 8:43 上午
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AsyncBeanFactoryPostProcessor.class)
public @interface EnableAsync {

}
