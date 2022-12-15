package com.luckyframework.bean.aware;


import org.springframework.core.env.Environment;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/6/29 上午12:18
 */
public interface EnvironmentAware extends Aware{

    void setEnvironment(Environment environment);
}
