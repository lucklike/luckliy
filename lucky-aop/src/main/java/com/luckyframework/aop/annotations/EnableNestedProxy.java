package com.luckyframework.aop.annotations;

import com.luckyframework.annotations.Import;
import com.luckyframework.aop.SettingSupportNestedBeanFactoryPostProcessor;

import java.lang.annotation.*;

/**
 * 启用嵌套代理支持
 * @author fk7075
 * @version 1.0
 * @date 2021/9/7 8:43 上午
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SettingSupportNestedBeanFactoryPostProcessor.class)
public @interface EnableNestedProxy {

}
