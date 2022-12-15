package com.luckyframework.scheduler.quartz;

import com.luckyframework.context.ApplicationContextUtils;
import com.luckyframework.scanner.ScannerUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 支持Lucky生命周期的QuartzJob，其子类中可以使用Lucky中提供的属性注入方式来注入属性
 */
public abstract class QuartzJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String luckyJobName = ScannerUtils.getScannerElementName(getClass());
        Object luckyJob = ApplicationContextUtils.containsBean(luckyJobName)
                ? ApplicationContextUtils.getBean(luckyJobName)
                : ApplicationContextUtils.luckyBeanInjection(luckyJobName,this);
        ((QuartzJob)luckyJob).executeInternal(context);
    }

    /**
     * 任务的具体逻辑，留给子类扩展实现
     * @param context 作业执行上下文
     * @throws JobExecutionException
     */
    protected abstract void executeInternal(JobExecutionContext context)throws JobExecutionException;
}
