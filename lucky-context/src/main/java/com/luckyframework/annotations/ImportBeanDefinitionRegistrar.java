package com.luckyframework.annotations;


import com.luckyframework.definition.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/29 0029 9:44
 */
public interface ImportBeanDefinitionRegistrar {

    default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    }

}
