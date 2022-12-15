package com.luckyframework.bean.factory;

/**
 * Bean初始化后执行afterPropertiesSet方法
 * @author fk
 * @version 1.0
 * @date 2021/3/26 0026 11:16
 */
public interface InitializingBean {

    void afterPropertiesSet() throws Exception;
}
