package com.luckyframework.common;

/**
 * 最后一个条件为false时才执行
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/14 06:05
 */
public class LastIsFalseRunning implements ExecutionStrategy{
    @Override
    public boolean isExecution(RunnableActuator.CurrentEnvResult currentEnvResult) {
        RunnableActuator.Result lastResult = currentEnvResult.getLastResult();
        return lastResult != null && !lastResult.getBoolean();
    }
}
