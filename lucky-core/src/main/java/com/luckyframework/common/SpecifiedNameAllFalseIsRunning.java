package com.luckyframework.common;

/**
 * 指定的名称全为false时执行
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/14 06:05
 */
public class SpecifiedNameAllFalseIsRunning implements ExecutionStrategy{

    private final String[] names;

    public SpecifiedNameAllFalseIsRunning(String... index) {
        this.names = index;
    }

    @Override
    public boolean isExecution(RunnableActuator.CurrentEnvResult currentEnvResult) {
        for (String name : names) {
            if (currentEnvResult.getResult(name).getBoolean()) {
                return false;
            }
        }
        return true;
    }
}
