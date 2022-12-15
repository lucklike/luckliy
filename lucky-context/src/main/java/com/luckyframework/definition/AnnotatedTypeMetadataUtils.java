package com.luckyframework.definition;

import com.luckyframework.bean.factory.ConstructorFactoryBean;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.bean.factory.MethodFactoryBean;
import com.luckyframework.bean.factory.StaticMethodFactoryBean;
import com.luckyframework.context.ApplicationContextUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.lang.NonNull;

/**
 *
 * @author FK-7075
 * @version 1.0.0
 * @time 2022/5/2 21:39
 */
public class AnnotatedTypeMetadataUtils {

    private final static Logger logger = LoggerFactory.getLogger(AnnotatedTypeMetadataUtils.class);

    public static AnnotatedTypeMetadata getBeanAnnotatedTypeMetadata(@NonNull BeanDefinition definition){
        FactoryBean factoryBean = definition.getFactoryBean();
        if(factoryBean instanceof ConstructorFactoryBean){
            return AnnotationMetadata.introspect(((ConstructorFactoryBean) factoryBean).getBeanClass());
        }
        if(factoryBean instanceof MethodFactoryBean){
            return new StandardMethodMetadata(((MethodFactoryBean) factoryBean).findMethod());
        }
        if(factoryBean instanceof StaticMethodFactoryBean){
            return new StandardMethodMetadata(((StaticMethodFactoryBean)factoryBean).findStaticMethod());
        }
        return AnnotationMetadata.introspect(factoryBean.getResolvableType().getRawClass());
    }

    public static AnnotatedTypeMetadata getBeanAnnotatedTypeMetadata(@NonNull String beanName){
        if(ApplicationContextUtils.containsBean(beanName)){
            return getBeanAnnotatedTypeMetadata(ApplicationContextUtils.getBeanDefinition(beanName));
        }
        throw new LuckyRuntimeException("The bean '"+beanName+"' does not exist and cannot get the annotation element").printException(logger);
    }

    public static AnnotatedTypeMetadata getAnnotatedTypeMetadata(@NonNull Object instance){
        return getAnnotatedTypeMetadata(instance.getClass());
    }

    public static AnnotatedTypeMetadata getAnnotatedTypeMetadata(@NonNull Class<?> instanceClass){
        return AnnotationMetadata.introspect(instanceClass);
    }
}
