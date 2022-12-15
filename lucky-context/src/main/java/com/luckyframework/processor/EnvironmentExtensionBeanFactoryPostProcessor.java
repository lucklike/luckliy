package com.luckyframework.processor;

import com.luckyframework.annotations.ProxyMode;
import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.BeanReference;
import com.luckyframework.bean.factory.FunctionalFactoryBean;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.common.Regular;
import com.luckyframework.common.TempPair;
import com.luckyframework.definition.BaseBeanDefinition;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.definition.PropertyValue;
import com.luckyframework.environment.v1.EnvironmentModifier;
import com.luckyframework.environment.v1.RuntimeModifier;
import com.luckyframework.environment.v1.SingletonRuntimeModifier;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 环境变量扩展器，扫描所有bean定义，找出其中所有注入属性为isByValue的bean定义
 * 将这些
 * @author fk7075
 * @version 1.0
 * @date 2021/11/14 9:31 上午
 */
public class EnvironmentExtensionBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory) {
        final Map<String, List<RuntimeModifier>> runtimeModifierMap = new LinkedHashMap<>();
        Environment environment = listableBeanFactory.getEnvironment();
        for (String definitionName : listableBeanFactory.getBeanDefinitionNames()) {
            BeanDefinition definition = listableBeanFactory.getBeanDefinition(definitionName);
            if(definition.isSingleton()){
                PropertyValue[] propertyValues = definition.getPropertyValues();
                for (PropertyValue propertyValue : propertyValues) {
                    Object value = propertyValue.getValue();
                    if((value instanceof BeanReference)){
                        BeanReference br = (BeanReference) value;
                        if(br.isByValue()){
                            String valueName = br.getBeanName();
                            List<String> expression = Regular.getArrayByExpression(valueName, Regular.$_$);
                            for (String expName : expression) {
                                int x = expName.indexOf(":");
                                if(x != -1){
                                    expName = expName.substring(0,x)+"}";
                                }
                                List<RuntimeModifier> runtimeModifiers = runtimeModifierMap.get(expName);
                                RuntimeModifier modifier = new SingletonRuntimeModifier(listableBeanFactory, environment, definitionName, propertyValue);
                                if(runtimeModifiers == null){
                                    runtimeModifiers = new LinkedList<>();
                                    runtimeModifiers.add(modifier);
                                    runtimeModifierMap.put(expName,runtimeModifiers);
                                }else{
                                    runtimeModifiers.add(modifier);
                                }
                            }
                        }
                    }
                }
            }
        }
        EnvironmentModifier environmentModifier = new EnvironmentModifier(runtimeModifierMap);
        BeanDefinition modifierDefinition = new BaseBeanDefinition();
        FunctionalFactoryBean factoryBean = () -> TempPair.of(environmentModifier, ResolvableType.forRawClass(EnvironmentModifier.class));
        modifierDefinition.setFactoryBean(factoryBean);
        modifierDefinition.setProxyMode(ProxyMode.NO);
        listableBeanFactory.registerBeanDefinition(EnvironmentModifier.class.getName(), modifierDefinition);
    }
}
