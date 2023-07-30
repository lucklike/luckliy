package com.luckyframework.definition;

import com.luckyframework.bean.factory.PluginManager;
import com.luckyframework.exception.BeanDefinitionRegisterException;
import com.luckyframework.scanner.ScannerUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean定义信息的注册容器
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/6 上午1:17
 */
public abstract class AbstractBeanDefinitionRegistry implements BeanDefinitionRegistry, PluginManager {

    private final static AnnotationMetadata[] NULL_METADATA = new AnnotationMetadata[0];

    /**
     * 组件bean定义
     */
    protected final Map<String, BeanDefinition> componentBeanDefinitionMap = new ConcurrentHashMap<>(225);
    /**
     * 插件bean定义
     */
    protected final Map<String, AnnotationMetadata> pluginBeanDefinitionMap = new ConcurrentHashMap<>(225);
    /**
     * 缓存注解与插件的映射关系
     */
    protected final Map<String, AnnotationMetadata[]> cacheAnnotationNamePluginMap = new ConcurrentHashMap<>();


    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionRegisterException {
        if (beanName == null) {
            throw new BeanDefinitionRegisterException("bean name is null.");
        }
        if (beanDefinition == null) {
            throw new BeanDefinitionRegisterException("beanDefinition is null.");
        }
        if (containsBeanDefinition(beanName)) {
            throw new BeanDefinitionRegisterException("The name is '" + beanName + "' the bean definition already exists");
        }
        componentBeanDefinitionMap.put(beanName, beanDefinition);
    }

    //------------------------------------------------------------------
    //              BeanDefinitionRegistry Methods
    //------------------------------------------------------------------

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        BeanDefinition definition = componentBeanDefinitionMap.get(beanName);
        Assert.notNull(definition, "There is no bean definition information named '" + beanName + "'");
        return definition;
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return componentBeanDefinitionMap.containsKey(beanName);
    }

    @Override
    public void removeBeanDefinition(String beanName) {
        componentBeanDefinitionMap.remove(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return componentBeanDefinitionMap.size();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return componentBeanDefinitionMap.keySet().toArray(new String[0]);
    }

    @Override
    public List<BeanDefinition> getBeanDefinitions() {
        return new ArrayList<>(componentBeanDefinitionMap.values());
    }


    //------------------------------------------------------------------
    //                  PluginManager Methods
    //------------------------------------------------------------------


    @Override
    public AnnotationMetadata[] getPlugins() {
        return pluginBeanDefinitionMap.values().toArray(NULL_METADATA);
    }

    @Override
    public AnnotationMetadata[] getPluginsFroAnnotation(String annotationClassName) {
        AnnotationMetadata[] annotationMetadataArray = cacheAnnotationNamePluginMap.get(annotationClassName);
        if (annotationMetadataArray == null) {
            Set<AnnotationMetadata> set = new HashSet<>();
            for (AnnotationMetadata metadata : pluginBeanDefinitionMap.values()) {
                if (ScannerUtils.annotationIsExist(metadata, annotationClassName)) {
                    set.add(metadata);
                }
            }
            annotationMetadataArray = set.toArray(NULL_METADATA);
            cacheAnnotationNamePluginMap.put(annotationClassName, annotationMetadataArray);
        }
        return annotationMetadataArray;
    }

    @Override
    public boolean containsPlugin(String pluginName) {
        return pluginBeanDefinitionMap.containsKey(pluginName);
    }

    @Override
    public void registerPlugin(String pluginName, AnnotationMetadata plugin) {
        if (pluginName == null) {
            throw new BeanDefinitionRegisterException("plugin name is null.");
        }
        if (plugin == null) {
            throw new BeanDefinitionRegisterException("plugin definition is null.");
        }
        if (containsPlugin(pluginName)) {
            throw new BeanDefinitionRegisterException("The name is'" + pluginName + "'The plugin definition already exists");
        }
        pluginBeanDefinitionMap.put(pluginName, plugin);
    }

    @Override
    public void removePlugin(String pluginName) {
        this.pluginBeanDefinitionMap.remove(pluginName);
    }
}
