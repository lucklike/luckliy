package com.luckyframework.proxy.conversion;

import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.FunctionalFactoryBean;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.common.TempPair;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.definition.BeanDefinitionBuilder;
import com.luckyframework.definition.GenericBeanDefinition;
import com.luckyframework.expression.BeanFactoryEvaluationContextFactory;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.scanner.ScannerUtils;
import com.luckyframework.spel.SpELRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/30 02:09
 */
public class ConversionBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    public static final Logger logger = LoggerFactory.getLogger(ConversionBeanFactoryPostProcessor.class);

    @Override
    public void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory) {
        AnnotationMetadata[] conversionPlugins = listableBeanFactory.getPluginsFroAnnotation(Conversion.class);
        for (AnnotationMetadata conversionPlugin : conversionPlugins) {
            final Class<?> pluginClass = ClassUtils.forName(conversionPlugin.getClassName(), ClassUtils.getDefaultClassLoader());
            String conversionBeanName = ScannerUtils.getScannerElementName(conversionPlugin);
            FunctionalFactoryBean factoryBean = () -> TempPair.of(ConversionUtils.getConversionServiceProxy(new SpELRuntime(new BeanFactoryEvaluationContextFactory(listableBeanFactory)), pluginClass), ResolvableType.forClass(pluginClass));
            GenericBeanDefinition conversionBeanDefinition = BeanDefinitionBuilder.builder(factoryBean, conversionPlugin);
            listableBeanFactory.registerBeanDefinition(conversionBeanName, conversionBeanDefinition);
        }

    }
}
