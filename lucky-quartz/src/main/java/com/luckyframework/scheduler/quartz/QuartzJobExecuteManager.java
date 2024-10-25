package com.luckyframework.scheduler.quartz;

import com.luckyframework.annotations.DisableProxy;
import com.luckyframework.bean.aware.ApplicationContextAware;
import com.luckyframework.bean.factory.ConstructorFactoryBean;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.bean.factory.InitializingBean;
import com.luckyframework.common.StringUtils;
import com.luckyframework.context.ApplicationContext;
import com.luckyframework.environment.LuckyStandardEnvironment;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.scheduler.quartz.annotations.Scheduled;
import com.luckyframework.scheduler.quartz.annotations.Schedules;
import com.luckyframework.scheduler.quartz.exceptions.QuartzConfigurationException;
import com.luckyframework.scheduler.quartz.exceptions.QuartzJobExecuteComponentAssembleException;
import com.luckyframework.scheduler.quartz.exceptions.ScheduledConfigurationException;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quartz 任务执行管理器
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/2 23:26
 */
@DisableProxy
public class QuartzJobExecuteManager implements ApplicationContextAware, InitializingBean {

    /** 定时任务相关的注解*/
    public final static Class<? extends Annotation>[] SCHEDULER_ANNOTATIONS = new Class[]{
            Scheduled.class, Schedules.class
    };

    /** 默认使用的调度器名称*/
    public final static String defaultSchedulerName = "quartzScheduler";
    /** 任务ID与任务逻辑所组成的Map*/
    private final Map<String,LuckyTask>  quartzTaskMap = new ConcurrentHashMap<>(64);
    /** Quartz调度器Bean名称与其执行组件所组成的Map*/
    private final Map<String,List<QuartzJobExecuteComponent>> schedulerJobMap = new ConcurrentHashMap<>(64);
    /** 应用程序上下文*/
    private ApplicationContext applicationContext;
    /** 环境变量*/
    private Environment environment;


    private final int CRON = 1;
    private final int FIXED_DELAY = 2;
    private final int FIXED_RATE = 3;

    /***
     * 根据任务ID获取一个任务
     * @param taskId 任务ID
     * @return 任务逻辑
     */
    @NonNull
    public LuckyTask getTask(String taskId){
        LuckyTask task = quartzTaskMap.get(taskId);
        Assert.notNull(task,"No LuckyTask corresponding to '"+taskId+"' was found");
        return task;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.environment = applicationContext.getEnvironment();

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化任务
        initializeScheduled();
        // 执行任务
        executeScheduled();
    }

    /**
     * 初始化所有的任务信息
     */
    private void initializeScheduled() throws SchedulerException {
        initializeQuartzScheduled();
        initializeAnnotationScheduled();
    }

    // 初始化所有QuartzBean
    private void initializeQuartzScheduled() throws SchedulerException {
        Scheduler scheduler = applicationContext.getBean(defaultSchedulerName,Scheduler.class);
        String[] triggerBeanNames = applicationContext.getBeanNamesForType(Trigger.class);
        String[] jobDetailBeanNames = applicationContext.getBeanNamesForType(JobDetail.class);

        jobDetailBeanCheck(jobDetailBeanNames);
        triggerBeanCheck(triggerBeanNames);

        for (String jobDetailBeanName : jobDetailBeanNames) {
            JobDetail jobDetail = applicationContext.getBean(jobDetailBeanName, JobDetail.class);
            if(scheduler.checkExists(jobDetail.getKey())){
                scheduler.addJob(jobDetail,true,true);
            }else{
                scheduler.addJob(jobDetail,false);
            }
        }

        for (String triggerBeanName : triggerBeanNames) {
            Trigger trigger = applicationContext.getBean(triggerBeanName, Trigger.class);
            if(!scheduler.checkExists(trigger.getKey())){
                scheduler.scheduleJob(trigger);
            }
        }
    }

    //  初始化所有注解定时任务
    private void initializeAnnotationScheduled(){
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanDefinitionNames) {
            FactoryBean factoryBean = applicationContext.getBeanDefinition(beanName).getFactoryBean();
            if(factoryBean instanceof ConstructorFactoryBean){
                ConstructorFactoryBean constructorFactoryBean = (ConstructorFactoryBean) factoryBean;
                Class<?> beanClass = constructorFactoryBean.getBeanClass();
                List<Method> scheduledAnnotationMethodList = ClassUtils.getMethodByAnnotationArrayOR(beanClass, SCHEDULER_ANNOTATIONS);
                scheduledAnnotationMethodCheck(scheduledAnnotationMethodList);
                scheduledAnnotationMethodList.forEach(m->parserScheduledMethod(beanName,m));
            }
        }
    }


    // 检验容器中是否存在多个ID相同的LuckyTask
    private void scheduledAnnotationMethodCheck(List<Method> scheduledAnnotationMethodList){
        Set<String> idSet = new HashSet<>();
        for (Method scheduledMethod : scheduledAnnotationMethodList) {
            Scheduled[] schedules = scheduledMethod.getAnnotationsByType(Scheduled.class);
            int index = 0;
            for (Scheduled schedule : schedules) {
                String taskId = scheduledMethod.getDeclaringClass().getName()+"#"+scheduledMethod.getName()+"@INDEX:["+(index++)+"]";
                String name = getEnvValue(StringUtils.getString(schedule.name(),taskId),String.class);
                String group = getEnvValue(schedule.group(),String.class);
                String id = new JobKey(name,group).toString();
                if(idSet.contains(id)){
                    throw new ScheduledConfigurationException("There are multiple tasks with the same ID["+id+"] in the lucky container.");
                }else{
                    idSet.add(id);
                }
            }
        }

    }

    // 检验容器中是否存在多个ID相同的JobDetail
    private void jobDetailBeanCheck(String[] jobDetailBeanNames){
        Set<String> jobDetailIdSet = new HashSet<>();
        for (String beanName : jobDetailBeanNames) {
            JobKey key = applicationContext.getBean(beanName, JobDetail.class).getKey();
            String jobKey = key.toString();
            if(jobDetailIdSet.contains(jobKey)){
                throw new QuartzConfigurationException("There are multiple JobDetail with the same ID["+jobKey+"] in the Lucky container.");
            }else{
                jobDetailIdSet.add(jobKey);
            }
        }
    }


    // 检验容器中是否存在多个ID相同的Trigger
    private void triggerBeanCheck(String[] triggerBeanNames){
        Set<String> triggerIdSet = new HashSet<>();
        for (String beanName : triggerBeanNames) {
            TriggerKey key = applicationContext.getBean(beanName, Trigger.class).getKey();
            String jobKey = key.toString();
            if(triggerIdSet.contains(jobKey)){
                throw new QuartzConfigurationException("There are multiple Trigger with the same ID["+jobKey+"] in the Lucky container.");
            }else{
                triggerIdSet.add(jobKey);
            }
        }
    }



    /**
     * 执行所有的定时任务
     * @throws SchedulerException 任务执行出错时会抛出的异常
     */
    private void executeScheduled() throws SchedulerException {
        Scheduler defaultScheduled = applicationContext.getBean(defaultSchedulerName, Scheduler.class);
        if(!defaultScheduled.isStarted()){
            defaultScheduled.start();
        }
        for (Map.Entry<String, List<QuartzJobExecuteComponent>> entry : schedulerJobMap.entrySet()) {
            List<QuartzJobExecuteComponent> quartzJobExecuteComponents = entry.getValue();
            for (QuartzJobExecuteComponent quartzJobExecuteComponent : quartzJobExecuteComponents) {
                quartzJobExecuteComponent.start();
            }
        }
    }

    /**
     * 将一个Scheduled方法解析为多个可执行的{@link LuckyTask}，并将其注册到容器中
     * @param beanName          方法所在实例的Bean名称
     * @param scheduledMethod   方法实例
     */
    private void parserScheduledMethod(String beanName,Method scheduledMethod){
        Scheduled[] schedules = scheduledMethod.getAnnotationsByType(Scheduled.class);
        int index = 0;
        for (Scheduled scheduled : schedules) {
            parserScheduledAnnotation(beanName,scheduledMethod, scheduled,index++);
        }
    }

    /**
     * 将方法中的一个具体的{@link Scheduled @Scheduled}解析为一个可执行的{@link LuckyTask},并将其注册到容器中
     * @param beanName          方法所在实例的Bean名称
     * @param scheduledMethod   方法实例
     * @param scheduled         Scheduled注解实例
     * @param index             注解的索引
     */
    private void parserScheduledAnnotation(String beanName,Method scheduledMethod,Scheduled scheduled,int index){
        String taskId = scheduledMethod.getDeclaringClass().getName()+"#"+scheduledMethod.getName()+"@INDEX:["+index+"]";
        int type = scheduledTypeCheck(scheduledMethod,scheduled);

        String schedulerName = StringUtils.getString(scheduled.scheduler(),defaultSchedulerName);
        Scheduler scheduler = applicationContext.getBean(schedulerName,Scheduler.class);

        String name = getEnvValue(StringUtils.getString(scheduled.name(),taskId),String.class);
        String group = getEnvValue(scheduled.group(),String.class);

        JobDetail jobDetail;
        Trigger trigger;

        // 指定使用cron表达式的触发器
        if(CRON == type){

            String cron = getEnvValue(scheduled.cron(),String.class);
            String zone = getEnvValue(scheduled.zone(),String.class);
            jobDetail = QuartzUtils.createNonStoreDurablyJobDetail(group,name);
            trigger = QuartzUtils.createCronTrigger(group,name,cron,zone);

        }

        // 指定使用固定频率的触发器，要等待上次任务完成后才会执行下次任务
        else if(FIXED_DELAY == type){

            String fixedDelayString = scheduled.fixedDelayString();
            String initialDelayString = scheduled.initialDelayString();
            String executeCountString = scheduled.executeCountString();
            long intervalInMillis = org.springframework.util.StringUtils.hasText(fixedDelayString)
                    ? getEnvValue(fixedDelayString,long.class) : scheduled.fixedDelay();
            long initialDelay = org.springframework.util.StringUtils.hasText(initialDelayString)
                    ? getEnvValue(initialDelayString,long.class) : scheduled.initialDelay();
            int executeCount = org.springframework.util.StringUtils.hasText(executeCountString)
                    ? getEnvValue(executeCountString,int.class) : scheduled.executeCount();

            trigger = QuartzUtils.createFixedDelayTrigger(group,name,intervalInMillis,executeCount,initialDelay);
            jobDetail = QuartzUtils.createNonStoreDurablyDisallowConcurrentJobDetail(group,name);

        }

        // 指定使用固定速率的触发器，不需要等待上次任务完成后才会执行下次任务
        else {

            String fixedRateString = scheduled.fixedRateString();
            long intervalInMillis = org.springframework.util.StringUtils.hasText(fixedRateString)
                    ? getEnvValue(fixedRateString,long.class) : scheduled.fixedRate();
            String initialDelayString = scheduled.initialDelayString();
            String executeCountString = scheduled.executeCountString();
            long initialDelay = org.springframework.util.StringUtils.hasText(initialDelayString)
                    ? getEnvValue(initialDelayString,long.class) : scheduled.initialDelay();
            int executeCount = org.springframework.util.StringUtils.hasText(executeCountString)
                    ? getEnvValue(executeCountString,int.class) : scheduled.executeCount();
            trigger = QuartzUtils.createFixedDelayTrigger(group,name,intervalInMillis,executeCount,initialDelay);
            jobDetail = QuartzUtils.createNonStoreDurablyJobDetail(group,name);
        }
        QuartzJobExecuteComponent jobExecuteComponent = new QuartzJobExecuteComponent(taskId, jobDetail, trigger);
        jobExecuteComponent.assemble(scheduler);
        addQuartzJobExecuteComponent(schedulerName,jobExecuteComponent);
        quartzTaskMap.put(taskId,()->MethodUtils.invoke(applicationContext.getBean(beanName),scheduledMethod));
    }

    /**
     * {@link Scheduled @Scheduled}注解类型校验
     * 注：Cron、fixedDelay和isFixedRate配置只能同时存在一个，否则会引发{@link ScheduledConfigurationException}
     * Cron表达式类型返回 1
     * fixedDelay类型返回2
     * isFixedRate类型返回3
     * @param scheduledMethod 定时任务方法
     * @param scheduled  {@link Scheduled @Scheduled}注解实例
     * @return 触发器类型
     */
    private int scheduledTypeCheck(Method scheduledMethod,Scheduled scheduled){
        if(scheduledMethod.getParameterCount() != 0){
            throw new ScheduledConfigurationException("The timed task method must have no parameters. method is '"+scheduledMethod+"'");
        }
        String cron = scheduled.cron();
        boolean isCron = org.springframework.util.StringUtils.hasText(cron);

        long fixedDelay = scheduled.fixedDelay();
        String fixedDelayString = scheduled.fixedDelayString();
        boolean isFixedDelay = (fixedDelay != -1L && !org.springframework.util.StringUtils.hasText(fixedDelayString))
                || (fixedDelay == -1L && org.springframework.util.StringUtils.hasText(fixedDelayString));

        long fixedRate = scheduled.fixedRate();
        String fixedRateString = scheduled.fixedRateString();
        boolean isFixedRate = (fixedRate != -1L && !org.springframework.util.StringUtils.hasText(fixedRateString))
                || (fixedRate == -1L && org.springframework.util.StringUtils.hasText(fixedRateString));

        if(isCron && !isFixedDelay && !isFixedRate){
            return CRON;
        }
        if(!isCron && isFixedDelay && !isFixedRate){
            return FIXED_DELAY;
        }
        if(!isCron && !isFixedDelay && isFixedRate){
            return FIXED_RATE;
        }
        throw new ScheduledConfigurationException(scheduled);

    }

    /**
     * 注册一个Quartz任务组件
     * @param schedulerId   调度器ID
     * @param jobComponent  任务组件
     */
    private void addQuartzJobExecuteComponent(String schedulerId,QuartzJobExecuteComponent jobComponent){
        List<QuartzJobExecuteComponent> quartzJobExecuteComponents = schedulerJobMap.get(schedulerId);
        if(quartzJobExecuteComponents == null){
            quartzJobExecuteComponents = new LinkedList<>();
            quartzJobExecuteComponents.add(jobComponent);
            schedulerJobMap.put(schedulerId,quartzJobExecuteComponents);
        }else{
            quartzJobExecuteComponents.add(jobComponent);
        }
    }


    /**
     * 从环境变量中获取值
     * @param configEx 表达式
     * @param tClass   目标类型的Class
     * @param <T>      目标类型
     * @return         指定类型的值
     */
    private <T> T getEnvValue(String configEx, Class<T> tClass){
        return ((LuckyStandardEnvironment)environment).resolvePlaceholdersForType(configEx, tClass);
    }


    /**
     * 封装Quartz任务执行时必要的组件，例如{@link JobDetail}、{@link Trigger}和{@link Scheduler}
     */
    static class QuartzJobExecuteComponent{

        private final String luckyTaskId;
        private final JobDetail jobDetail;
        private final Trigger trigger;
        private Scheduler scheduler;
        private boolean isAssemble = false;

        QuartzJobExecuteComponent(String luckyTaskId, JobDetail jobDetail, Trigger trigger) {
            this.luckyTaskId = luckyTaskId;
            this.jobDetail = jobDetail;
            this.trigger = trigger;
        }

        /**
         * 将Quartz组件组装起来等待调用
         * @param scheduler Quartz调度器
         */
        public void assemble(Scheduler scheduler){
            if(isAssemble){
                throw new QuartzJobExecuteComponentAssembleException("The Quartz job component has been assembled and cannot be assembled again");
            }else{
                try{
                    isAssemble = true;
                    this.scheduler = scheduler;
                    if(scheduler.checkExists(jobDetail.getKey())){
                        scheduler.addJob(jobDetail,true,true);
                    }else{
                        trigger.getJobDataMap().put(LuckyAnnotationQuartzJob.LUCKY_DEFAULT_DATA_KEY,luckyTaskId);
                        this.scheduler.scheduleJob(jobDetail,trigger);
                    }
                }catch (Exception e){
                    throw new QuartzJobExecuteComponentAssembleException(e);
                }
            }
        }

        /**
         * 启动Quartz定时任务
         * @throws SchedulerException
         */
        public void start() throws SchedulerException {
            Assert.notNull(scheduler,"Scheduler is null");
            if(!scheduler.isStarted()){
                scheduler.start();;
            }
        }

    }
}
