package com.luckyframework.common;

/**
 * 之前的只要有一个为false时就执行
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/14 06:05
 */
public class OnlyOneFalseIsRunning implements ExecutionStrategy{
    @Override
    public boolean isExecution(RunnableActuator.CurrentEnvResult currentEnvResult) {
        for (RunnableActuator.Result result : currentEnvResult.getResultIterable()) {
            if (!result.getBoolean()) {
                return true;
            }
        }
        return false;
    }
}
