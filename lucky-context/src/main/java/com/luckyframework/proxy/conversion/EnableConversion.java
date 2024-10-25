package com.luckyframework.proxy.conversion;

import com.luckyframework.annotations.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动生成转换工具代理生成器功能的开关
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/30 02:29
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ConversionBeanFactoryPostProcessor.class, ConversionConfigCheckApplicationListener.class})
public @interface EnableConversion {
}
