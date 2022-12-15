package com.luckyframework.bean.factory;

import java.util.List;

/**
 * BeanPostProcessor的注册器
 * @author fk
 * @version 1.0
 * @date 2021/3/16 0016 16:04
 */
public interface BeanPostProcessorRegistry {

    /**
     * 注册一个BeanPostProcessor
     */
    void registerBeanPostProcessor(BeanPostProcessor processor);

    /**
     * 获取所有注册的BeanPostProcessor
     */
    List<BeanPostProcessor> getBeanPostProcessors();
}
