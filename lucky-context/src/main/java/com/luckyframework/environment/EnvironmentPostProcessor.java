package com.luckyframework.environment;

import org.springframework.core.env.Environment;

/**
 * 环境变量的后置处理器
 * @author fk7075
 * @version 1.0
 * @date 2021/9/17 6:36 下午
 */
public interface EnvironmentPostProcessor {

    void postProcessorEnvironment(Environment environment);
}
