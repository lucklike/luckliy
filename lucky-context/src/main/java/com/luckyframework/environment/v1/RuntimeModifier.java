package com.luckyframework.environment.v1;

/**
 * 环境变量运行时修改器接口
 * @author fk7075
 * @version 1.0
 * @date 2021/11/14 9:33 上午
 */
@FunctionalInterface
public interface RuntimeModifier {

    void setEnvironmentValue(String key,Object value) throws RuntimeModifierException;
}
