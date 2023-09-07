package com.luckyframework.common;

/**
 * 执行策略
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/14 03:22
 */
@FunctionalInterface
public interface ExecutionStrategy {

    boolean isExecution(RunnableActuator.CurrentEnvResult currentEnvResult);
}
