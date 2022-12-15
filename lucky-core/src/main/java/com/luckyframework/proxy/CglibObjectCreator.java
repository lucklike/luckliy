package com.luckyframework.proxy;

import org.springframework.cglib.proxy.Enhancer;

/**
 * Cglib对象创建器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/5/15 06:05
 */
public interface CglibObjectCreator {

    Object createProxyObject(Enhancer enhancer);

}
