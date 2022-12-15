package com.luckyframework.context;

import com.luckyframework.bean.factory.AgentBeanFactory;
import com.luckyframework.bean.factory.BeanPostProcessorRegistry;
import com.luckyframework.bean.factory.ListableBeanFactory;
import com.luckyframework.bean.factory.PluginManager;
import com.luckyframework.context.event.ApplicationEventPublisher;
import com.luckyframework.context.message.MessageSource;
import com.luckyframework.definition.BeanDefinitionRegistry;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.scanner.Scanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

/**
 * 应用程序上下文
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/12 上午1:40
 */
public interface ApplicationContext extends ListableBeanFactory, PluginManager, AgentBeanFactory,
        ResourceLoader, BeanDefinitionRegistry, BeanPostProcessorRegistry, ApplicationEventPublisher, MessageSource {

    default Resource[] getResources(String locationPattern) throws IOException{
        return Scanner.PM.getResources(locationPattern);
    }

    Environment getEnvironment();

    @Override
    default Resource getResource(String location) {
        return Scanner.PM.getResource(location);
    }

    @Override
    default ClassLoader getClassLoader() {
        return ClassUtils.getDefaultClassLoader();
    }

    /**
     * 获取按照优先级排序后的所有单例bean的名称
     * @return 按照优先级排序后的所有单例bean的名称
     */
    String[] prioritizedSingletonBeans();

}
