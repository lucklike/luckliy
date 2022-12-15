package com.luckyframework.processor;

import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.definition.BaseBeanDefinition;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.definition.GenericBeanDefinition;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.StandardMethodMetadata;

/**
 * 处理{@link FactoryBean}类型的Bean的后置处理器，
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/3 18:10
 */
public class FactoryBeanBeanFactoryPostProcessor implements BeanFactoryPostProcessor {


    @Override
    public void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory) {
        String[] factoryNameNames = listableBeanFactory.getBeanNamesForType(FactoryBean.class);
        for (String factoryNameName : factoryNameNames) {
            BeanDefinition factoryBeanDefinition = listableBeanFactory.getBeanDefinition(factoryNameName);
            com.luckyframework.bean.factory.FactoryBean  realBeanFactoryBean = new com.luckyframework.bean.factory.FactoryBean () {
                private final String factoryBeanName = getFactoryBeanName(factoryNameName);

                @Override
                public Object createBean() {
                    return listableBeanFactory.getBean(factoryBeanName,FactoryBean.class).getBean();
                }

                @Override
                public ResolvableType getResolvableType() {
                    ResolvableType factoryBeanType = listableBeanFactory.getResolvableType(factoryBeanName);
                    if(factoryBeanType.hasGenerics()){
                        return factoryBeanType.getGeneric(0);
                    }
                    factoryBeanType = ResolvableType.forClass(FactoryBean.class, factoryBeanType.getRawClass());
                    return factoryBeanType.getGeneric(0);
                }
            };
            listableBeanFactory.removeBeanDefinition(factoryNameName);
            listableBeanFactory.registerBeanDefinition(getFactoryBeanName(factoryNameName), factoryBeanDefinition);
            BaseBeanDefinition realBeanDefinition = new BaseBeanDefinition();
            realBeanDefinition.setFactoryBean(realBeanFactoryBean);
            GenericBeanDefinition.setBeanDefinitionField(getRealBeanAnnotatedTypeMetadata(factoryBeanDefinition.getResolvableType().resolve()), realBeanDefinition);
            listableBeanFactory.registerBeanDefinition(factoryNameName, realBeanDefinition);
        }
    }

    private String getFactoryBeanName(String sourceName) {
        return "&" + sourceName;
    }

    private AnnotatedTypeMetadata getRealBeanAnnotatedTypeMetadata(Class<?> factoryBeanClass){
        return new StandardMethodMetadata(MethodUtils.getDeclaredMethod(factoryBeanClass, "getBean"));
    }

}
