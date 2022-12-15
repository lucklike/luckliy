package com.luckyframework.environment.v1;

/**
 * 默认的运行时环境变量修改器
 * @author fk7075
 * @version 1.0
 * @date 2021/11/14 1:22 下午
 */
public class DefaultRuntimeModifier implements RuntimeModifier {

    public DefaultRuntimeModifier(Environment environment) {
        this.environment = environment;
    }

    private final Environment environment;

    @Override
    public void setEnvironmentValue(String key, Object value) throws RuntimeModifierException {
        environment.setProperty(key, value);
    }
}
