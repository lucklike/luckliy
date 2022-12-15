package com.luckyframework.scheduler.quartz;

import com.luckyframework.annotations.Bean;
import com.luckyframework.annotations.Configuration;
import com.luckyframework.annotations.Qualifier;
import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.environment.LuckyStandardEnvironment;
import com.luckyframework.scheduler.quartz.exceptions.QuartzConfigurationException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.core.env.Environment;

/***
 * Quartz相关的自动配置类
 */
@Configuration
public class QuartzAutoConfiguration {

    private final static String QUARTZ_PROPERTIES = "lucky.quartz.properties";

    @Bean
    public SchedulerFactory schedulerFactory(Environment environment) throws SchedulerException {
        LuckyStandardEnvironment luckyEnv = (LuckyStandardEnvironment) environment;
        if(luckyEnv.containsProperty(QUARTZ_PROPERTIES)){
            Object property = luckyEnv.getPropertyForObject(QUARTZ_PROPERTIES);
            if(property instanceof ConfigurationMap){
                ConfigurationMap configMap = (ConfigurationMap) property;
               return new StdSchedulerFactory(configMap.toProperties());
            }
            throw new QuartzConfigurationException("quartz properties configuration error! '"+QUARTZ_PROPERTIES+"'='"+property+"'");
        }else{
            return new StdSchedulerFactory();
        }
    }

    @Bean(name = QuartzJobExecuteManager.defaultSchedulerName,destroyMethod = "shutdown")
    public Scheduler createScheduler(@Qualifier("schedulerFactory") SchedulerFactory schedulerFactory) throws SchedulerException {
        return schedulerFactory.getScheduler();
    }

}
