package com.luckyframework.expression;

import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.FunctionalFactoryBean;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.common.TempPair;
import com.luckyframework.definition.BaseBeanDefinition;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.spel.SpELRuntime;
import org.springframework.core.ResolvableType;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/31 18:47
 */
public class SpELRuntimeBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    public static final String SPEL_RUNTIME_BEAN = "luckyDefaultSpELRuntime";


    @Override
    public void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory) {
        BeanDefinition spELRuntimeDefinition = new BaseBeanDefinition();
        FunctionalFactoryBean factoryBean = () -> TempPair.of(new SpELRuntime(new BeanFactoryEvaluationContextFactory(listableBeanFactory)), ResolvableType.forRawClass(SpELRuntime.class));
        spELRuntimeDefinition.setFactoryBean(factoryBean);
        listableBeanFactory.registerBeanDefinition(SPEL_RUNTIME_BEAN,spELRuntimeDefinition);
    }
}
