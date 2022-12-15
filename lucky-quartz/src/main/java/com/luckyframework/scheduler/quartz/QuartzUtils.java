package com.luckyframework.scheduler.quartz;

import com.luckyframework.scheduler.quartz.exceptions.CronExpressionException;
import org.quartz.*;
import org.springframework.util.StringUtils;

import java.util.TimeZone;

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Quartz工具类，用于快速创建Quartz中的一些重要组件
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/2 23:34
 */
public abstract class QuartzUtils {


    /**
     * 创建一个需要持久保存而且禁止并行的{@link JobDetail}
     * @param groupName             组名
     * @param jobName               Job名
     * @return  {@link JobDetail}
     */
    public static JobDetail createStoreDurablyDisallowConcurrentJobDetail(String groupName,String jobName){
        return createJobDetail(groupName, jobName,true,true);
    }

    /**
     * 创建一个不需要持久保存而且禁止并行的{@link JobDetail}
     * @param groupName             组名
     * @param jobName               Job名
     * @return  {@link JobDetail}
     */
    public static JobDetail createNonStoreDurablyDisallowConcurrentJobDetail(String groupName,String jobName){
        return createJobDetail(groupName, jobName,false,true);
    }


    /**
     * 创建一个需要持久保存的的{@link JobDetail}
     * @param groupName             组名
     * @param jobName               Job名
     * @return  {@link JobDetail}
     */
    public static JobDetail createStoreDurablyJobDetail(String groupName,String jobName){
        return createJobDetail(groupName, jobName,true,false);
    }

    /**
     * 创建一个不需要持久保存的的{@link JobDetail}
     * @param groupName             组名
     * @param jobName               Job名
     * @return  {@link JobDetail}
     */
    public static JobDetail createNonStoreDurablyJobDetail(String groupName,String jobName){
        return createJobDetail(groupName, jobName,false,false);
    }

    /**
     * 创建一个{@link JobDetail}
     * @param groupName             组名
     * @param jobName               Job名
     * @param jobDurability         是否持久保存
     * @param isDisallowConcurrent  是否禁止冰并行
     * @return  {@link JobDetail}
     */
    public static JobDetail createJobDetail(String groupName, String jobName, boolean jobDurability, boolean isDisallowConcurrent){
        Class<? extends Job> jobClass = isDisallowConcurrent ? LuckyAnnotationQuartzSerialJob.class : LuckyAnnotationQuartzJob.class;
        return JobBuilder
                .newJob(jobClass)
                .withIdentity(jobName,groupName)
                .storeDurably(jobDurability)
                .build();
    }


    /**
     * 返回一个简单的触发器
     * @param groupName         组名
     * @param triggerName       触发器名
     * @param intervalInMillis  执行间隔
     * @param executeCount      执行次数
     * @param initialDelay      第一次执行时的延时时间
     * @return 简单的触发器
     */
    public static Trigger createFixedDelayTrigger(String groupName, String triggerName,long intervalInMillis,int executeCount,long initialDelay){
        TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger().withIdentity(triggerName,groupName);
        if(initialDelay != -1){
            builder.startAt(futureDate((int) initialDelay, DateBuilder.IntervalUnit.MILLISECOND));
        }
        SimpleScheduleBuilder scheduleBuilder = simpleSchedule().withIntervalInMilliseconds(intervalInMillis);
        if(executeCount != -1){
            scheduleBuilder.withRepeatCount(executeCount);
        }else{
            scheduleBuilder.repeatForever();
        }
        return builder.withSchedule(scheduleBuilder).build();
    }

    /**
     * 获取一个基于Cron表达式的触发器
     * @param groupName         组名
     * @param triggerName       触发器名
     * @param cronExpression    Cron表达式
     * @param zoneId            时区ID
     * @return 基于Cron表达式的触发器
     */
    public static Trigger createCronTrigger(String groupName, String triggerName, String cronExpression, String zoneId){
        TimeZone timeZone = StringUtils.hasText(zoneId) ? TimeZone.getTimeZone(zoneId) : null;
        return createCronTrigger(groupName, triggerName, cronExpression, timeZone);
    }

    /**
     * 获取一个基于Cron表达式的触发器
     * @param groupName         组名
     * @param triggerName       触发器名
     * @param cronExpression    Cron表达式
     * @param timezone          时区
     * @return 基于Cron表达式的触发器
     */
    public static Trigger createCronTrigger(String groupName, String triggerName, String cronExpression, TimeZone timezone){
        if(!CronExpression.isValidExpression(cronExpression)){
            throw new CronExpressionException("Invalid cron expressions: '"+cronExpression+"'");
        }
        TriggerBuilder<Trigger> builder = createTriggerBuilder(groupName, triggerName);

        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
        if (timezone != null){
            cronScheduleBuilder.inTimeZone(timezone);
        }
        return builder.startNow().withSchedule(cronScheduleBuilder).build();
    }

    /**
     * 获取一个触发器的构造器
     * @param groupName     组名
     * @param triggerName   触发器名
     * @return 触发器的构造器
     */
    private static TriggerBuilder<Trigger> createTriggerBuilder(String groupName,String triggerName){
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
        return triggerBuilder.withIdentity(triggerName,groupName);
    }

}
