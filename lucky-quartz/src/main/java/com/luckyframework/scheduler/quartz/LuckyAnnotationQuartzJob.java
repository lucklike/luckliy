package com.luckyframework.scheduler.quartz;

import com.luckyframework.context.ApplicationContextUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 用于执行Lucky注解任务的Job，使用约定的Key从{@link JobDataMap}中获取对应JobDetail的任务ID
 * 使用此ID可以从Lucky的任务管理器{@link QuartzJobExecuteManager}中获得一个特定对应的定时任务逻辑{@link LuckyTask}
 *
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/2 23:23
 */
public class LuckyAnnotationQuartzJob implements Job {

    public final static String LUCKY_DEFAULT_DATA_KEY = "8A5A5D563525E3B9C623A16F1404D127";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Class<QuartzJobExecuteManager> managerClass = QuartzJobExecuteManager.class;
        QuartzJobExecuteManager jobExecuteManager = ApplicationContextUtils.getBean(managerClass.getName(), managerClass);
        JobDataMap dataMap = context.getMergedJobDataMap();
        String taskKey = dataMap.getString(LUCKY_DEFAULT_DATA_KEY);
        LuckyTask task = jobExecuteManager.getTask(taskKey);
        task.execute();
    }
}
