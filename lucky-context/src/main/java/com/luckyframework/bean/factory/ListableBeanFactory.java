package com.luckyframework.bean.factory;

import com.luckyframework.definition.AnnotatedTypeMetadataUtils;
import com.luckyframework.exception.BeansException;
import com.luckyframework.exception.NoSuchBeanDefinitionException;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 可列出的Bean工厂
 * @author fk
 * @version 1.0
 * @date 2021/3/23 0023 9:20
 */
public interface ListableBeanFactory extends BeanFactory {

    boolean beanIsCache(String beanName);

    void addSingletonBean(String beanName,Object singletonObject);

    /**
     * 是否包含该名称的bean定义
     * @param beanName bean定义的名称
     * @return Y/N ->T/F
     */
    boolean containsBeanDefinition(String beanName);

    /**
     * 获取所有bean定义的数量
     * @return bean定义的总数
     */
    int getBeanDefinitionCount();

    /**
     * 获取所有bean定义的名称
     * @return 所有bean定义的名称
     */
    String[] getBeanDefinitionNames();

    /**
     * 获取所有与指定类型相匹配的bean的名称
     * @param type 指定的类型
     * @return 所有与指定类型相匹配的bean的名称
     */
    default String[] getBeanNamesForType(@Nullable Class<?> type){
        return getBeanNamesForType(type,true);
    }

    /**
     * 获取所有与指定类型相匹配的bean的名称
     * @param type 指定的类型
     * @param includeNonSingletons 是否包含非单例的bean
     * @return 所有与指定类型相匹配的bean的名称
     */
    default String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons){
        return getBeanNamesForType(ResolvableType.forRawClass(type),includeNonSingletons);
    }

    /**
     * 获取所有与指定类型相匹配的bean的名称
     * @param type 指定的类型
     * @return 所有与指定类型相匹配的bean的名称
     */
    default String[] getBeanNamesForType(@Nullable ResolvableType type){
        return getBeanNamesForType(type,true);
    }

    /**
     * 根据bean的类型获取bean的名称，如果存在多个则返回被{@link com.luckyframework.annotations.Primary @Primary}注解标注的那个，
     * 一个都没找到返回null，找到多个被{@link com.luckyframework.annotations.Primary @Primary}注解标注的bean时抛出异常
     * @param resolvableType bean的类型
     * @throws BeansException 找到多个被{@link com.luckyframework.annotations.Primary @Primary}注解标注的bean时抛出异常
     * @return bean的名称
     */
    String getBeanNameForType(ResolvableType resolvableType) throws BeansException;

    default String getBeanNameForType(Class<?> type){
        return getBeanNameForType(ResolvableType.forRawClass(type));
    }

    /**
     * 获取所有与指定类型相匹配的bean的名称
     * @param type 指定的类型
     * @param includeNonSingletons 是否包含非单例的bean
     * @return 所有与指定类型相匹配的bean的名称
     */
    String[] getBeanNamesForType(@Nullable ResolvableType type, boolean includeNonSingletons);

    /**
     * 获取所有被指定注解标注的实例
     * @param annotationType 指定注解的类型
     * @return 所有被指定注解标注的实例的名称
     */
    String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);

    /**
     * 获取指定类型下所有的Bean的名称和实例所组成的Map，包括所有的非单实例Bean
     * @param type 指定类型的Class
     * @param <T>  类型
     * @return 所有的Bean的名称和实例所组成的Map
     * @throws BeansException
     */
    default <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException{
        return getBeansOfType(type,true);
    }

    /**
     * 获取指定类型下所有的Bean的名称和实例所组成的Map
     * @param type 指定类型的Class
     * @param includeNonSingletons 是否包括非单实例Bean
     * @param <T>  类型
     * @return 所有的Bean的名称和实例所组成的Map
     * @throws BeansException
     */
    <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons)
            throws BeansException;

    /**
     * 获取被指定注解标注的所有的Bean 的名称与实例所组成的Map
     * @param annotationType 注解类型的Class
     * @return 所有的Bean 的名称与实例所组成的Map
     * @throws BeansException
     */
    Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException;

    /**
     * 返回指定Bean上的某个注解的实例
     * @param beanName Bean的名称
     * @param annotationType 注解Class
     * @param <A> 注解的类型
     * @return
     * @throws NoSuchBeanDefinitionException
     */
    @Nullable
    <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
            throws NoSuchBeanDefinitionException;

    /**
     * 获取指定类型的所欲根据排序注解排序后的所有Bean名称的集合（包括所有的非单实例Bean）
     * @param type 指定Bean类型的Class
     * @param <T>  Bean的类型
     * @return 排序后的所有Bean的名称
     */
    default <T> List<T> getSortedBeansOfType(@Nullable Class<T> type){
        return getSortedBeansOfType(type,true);
    }


    /**
     * 获取指定类型的所欲根据排序注解排序后的所有Bean名称的集合
     * @param type 指定Bean类型的Class
     * @param includeNonSingletons 是否包含非单实例的Bean
     * @param <T>  Bean的类型
     * @return 排序后的所有Bean的名称
     */
    default <T> List<T> getSortedBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons){
        Map<String, T> beansMap = getBeansOfType(type,includeNonSingletons);
        List<T> list = new ArrayList<>(beansMap.values());
        AnnotationAwareOrderComparator.sort(list);
        return list;
    }

    /**
     * 获取某个Bean的 {@link AnnotatedTypeMetadata}
     * @param beanName Bean的名称
     * @return {@link AnnotatedTypeMetadata}
     */
    default AnnotatedTypeMetadata getBeanAnnotatedTypeMetadata(String beanName){
        return AnnotatedTypeMetadataUtils.getBeanAnnotatedTypeMetadata(beanName);
    }

}
