package com.luckyframework.bean.factory;

import com.luckyframework.scanner.ScannerUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.lang.annotation.Annotation;

/**
 * 插件管理器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/4 下午11:41
 */
public interface PluginManager {

    /**
     * 注册一个插件
     */
    default void registerPlugin(String pluginName,Resource classResource) throws IOException {
        registerPlugin(pluginName, ScannerUtils.getAnnotationMetadata(classResource));
    }

    /**
     * 注册一个插件
     */
    default void registerPlugin(String pluginName, Class<?> pluginClass){
        registerPlugin(pluginName, AnnotationMetadata.introspect(pluginClass));
    }

    /**
     * 注册一个插件
     */
    void registerPlugin(String pluginName, AnnotationMetadata plugin);

    void removePlugin(String pluginName);

    /** 判断是否包含指定名称的插件*/
    boolean containsPlugin(String pluginName);

    /**
     * 获取所有的插件
     */
    AnnotationMetadata[] getPlugins();


    /**
     * 获取被某个插件注解标注的所有插件
     * @param annotationClassName 注解类的全类名
     */
    AnnotationMetadata[] getPluginsFroAnnotation(@NonNull String annotationClassName);

    /**
     * 获取被某个插件注解标注的所有插件
     * @param annotationClass 注解类的Class
     */
    default AnnotationMetadata[] getPluginsFroAnnotation(Class<? extends Annotation> annotationClass){
        return getPluginsFroAnnotation(annotationClass.getName());
    }

}
