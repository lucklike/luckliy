package com.luckyframework.scheduler.quartz.annotations;

import java.lang.annotation.*;

/**
 * 定时任务注解
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/2 23:19
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Schedules.class)
public @interface Scheduled {

    //----------------------------------------------------------------------------
    //              注意：cron、fixedDelay、fixedRate 只能同时存在一个
    //----------------------------------------------------------------------------

    /** 指定执行该任务的调度器*/
    String scheduler() default "";

    /** 任务名称*/
    String name() default "";

    /** 任务组名称*/
    String group() default "LUCKY_ANNOTATION_SCHEDULED";

    /** cron表达式*/
    String cron() default "";

    /** 指定cron表达式的时区*/
    String zone() default "";

    /** 以固定的时间间隔执行任务，单位为毫秒（要等待上次任务完成后）*/
    long fixedDelay() default -1L;

    /** 以固定的时间间隔执行任务，使用字符串表示*/
    String fixedDelayString() default "";

    /** 在调用之间以固定的周期（以毫秒为单位）执行带注释的方法。（不需要等待上次任务完成）*/
    long fixedRate() default -1L;

    /** 在调用之间以固定的周期（以毫秒为单位）执行带注释的方法。（不需要等待上次任务完成）*/
    String fixedRateString() default "";

    /** 第一次执行fixedRate()或fixedDelay()任务之前延迟的毫秒数*/
    long initialDelay() default -1L;

    /** 第一次执行fixedRate()或fixedDelay()任务之前延迟的毫秒数*/
    String initialDelayString() default "";

    /** 执行次数*/
    int executeCount() default -1;

    /** 执行次数*/
    String executeCountString() default "";

}
