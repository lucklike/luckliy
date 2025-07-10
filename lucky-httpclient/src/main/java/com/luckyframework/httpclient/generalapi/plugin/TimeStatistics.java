package com.luckyframework.httpclient.generalapi.plugin;

import com.luckyframework.httpclient.proxy.plugin.Plugin;
import org.springframework.core.annotation.AliasFor;

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
@Plugin(pluginClass = TimeStatisticsPlugin.class)
public @interface TimeStatistics {

    /**
     * 是否开启统计功能
     */
    @AliasFor("enable")
    boolean value() default true;

    /**
     * 是否开启统计功能
     */
    @AliasFor("value")
    boolean enable() default true;

    /**
     * 触发警告标志的最小耗时
     */
    long warn() default -1L;

    /**
     * 触发错误标志的最小耗时
     */
    long slow() default -1L;

    /**
     * 耗时统计处理器
     */
    Class<? extends TimeStatisticsHandle> handle() default DefaultTimeStatisticsHandle.class;

}
