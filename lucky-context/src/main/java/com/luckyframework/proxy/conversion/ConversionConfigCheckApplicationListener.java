package com.luckyframework.proxy.conversion;

import com.luckyframework.context.ApplicationContext;
import com.luckyframework.context.event.ApplicationListener;
import com.luckyframework.context.event.ContextRefreshedEvent;
import com.luckyframework.conversion.ConversionManager;
import com.luckyframework.conversion.UseConversion;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.reflect.Method;
import java.util.List;

/**
 * ApplicationContext创建完成事件监听器，用于检查Conversion配置是否正常
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/5 01:12
 */
public class ConversionConfigCheckApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ConversionConfigCheckApplicationListener.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        conversionConfigCheck((ApplicationContext) event.getSource());
    }

    private void conversionConfigCheck(ApplicationContext context){
        AnnotationMetadata[] pluginsFroAnnotation = context.getPluginsFroAnnotation(Conversion.class);
        for (AnnotationMetadata metadata : pluginsFroAnnotation) {
            final Class<?> pluginClass = ClassUtils.forName(metadata.getClassName(), ClassUtils.getDefaultClassLoader());
            checkConversionClass(pluginClass);
        }
    }

    private void checkConversionClass(Class<?> conversionClass){
        UseConversion use = AnnotationUtils.findMergedAnnotation(conversionClass, UseConversion.class);
        if(use != null){
            for (String name : use.names()) {
                if(!ConversionManager.contains(name)){
                    throw new ConversionConfigurationException("Use the converter failed because there is no converter with name '{}', location: {}", name, conversionClass).printException(logger);
                }
            }
        }

        List<Method> useMethod = ClassUtils.getMethodByStrengthenAnnotation(conversionClass, UseConversion.class);
        for (Method method : useMethod) {
            UseConversion methodUsed = AnnotationUtils.findMergedAnnotation(method, UseConversion.class);
            for (String name : methodUsed.names()) {
                if(!ConversionManager.contains(name)){
                    throw new ConversionConfigurationException("Use the converter failed because there is no converter with name '{}', location: {}", name, method).printException(logger);
                }
            }
        }
    }

}
