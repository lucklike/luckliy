package com.luckyframework.scheduler.quartz.annotations;

import java.lang.annotation.*;

/**
 * 定时任务注解
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/3 00:04
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Schedules {
    Scheduled[] value();
}
