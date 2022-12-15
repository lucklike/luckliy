package com.luckyframework.scheduler.quartz.annotations;

import com.luckyframework.annotations.Import;
import com.luckyframework.scheduler.quartz.QuartzAutoConfiguration;
import com.luckyframework.scheduler.quartz.QuartzJobExecuteManager;

import java.lang.annotation.*;

/**
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/3 03:14
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({QuartzAutoConfiguration.class, QuartzJobExecuteManager.class})
public @interface EnableScheduling {
}
