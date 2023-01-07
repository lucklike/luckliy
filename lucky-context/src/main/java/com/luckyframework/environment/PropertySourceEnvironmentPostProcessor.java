package com.luckyframework.environment;

import com.luckyframework.bean.aware.ApplicationContextAware;
import com.luckyframework.context.AbstractApplicationContext;
import com.luckyframework.context.ApplicationContext;
import com.luckyframework.scanner.ScanElementClassifier;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2022/12/20 23:54
 */
@Order(Integer.MAX_VALUE - 10)
public class PropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, ApplicationContextAware {

    private ScanElementClassifier scannerClassifier;

    @Override
    public void postProcessorEnvironment(Environment env) {
        LuckyStandardEnvironment environment = (LuckyStandardEnvironment) env;
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.remove(ConfigurationPropertySourceUtils.PROPERTY_SOURCE_ANNOTATION_SOURCE_NAME);
        CompositePropertySource cps = ConfigurationPropertySourceUtils.getPropertySourceAnnotationSource(scannerClassifier.getComponents());
        if(!cps.isEmpty()){
            environment.getPropertySources().addLast(cps);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.scannerClassifier = ((AbstractApplicationContext) applicationContext).getScannerClassifier();
    }
}
