package com.luckyframework.scheduler.quartz;

import org.quartz.DisallowConcurrentExecution;

/**
 * 禁止并发的任务
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/3 02:31
 */
@DisallowConcurrentExecution
public class LuckyAnnotationQuartzSerialJob extends LuckyAnnotationQuartzJob{
}
