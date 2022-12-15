package com.luckyframework.environment;

import com.luckyframework.context.AbstractApplicationContext;
import org.springframework.core.env.Environment;

/**
 * 环境变量扩展接口
 * 该接口为SPI接口，需要将实例的全类名配置在{@core META_INF/lucky.factories}中，
 * 容器会在【环境变量初始化结束后】,【容器初始化之前】执行该接口实例中的回调方法来完成扩展功能
 * @see AbstractApplicationContext#environmentExtension()
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/17 11:15
 */
@FunctionalInterface
public interface EnvironmentExtension {

    void extend(Environment environment);

}
