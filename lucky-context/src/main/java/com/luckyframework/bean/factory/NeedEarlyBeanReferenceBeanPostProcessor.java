package com.luckyframework.bean.factory;

/**
 * 需要提前暴露早期Bean的后置处理器
 */
public interface NeedEarlyBeanReferenceBeanPostProcessor extends BeanPostProcessor{

    default Object getEarlyBeanReference(String beanName,Object beanInstance){
        return beanInstance;
    }
}
