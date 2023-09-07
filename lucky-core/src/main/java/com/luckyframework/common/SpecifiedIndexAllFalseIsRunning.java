package com.luckyframework.common;

/**
 * 指定的索引全为false时执行
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/14 06:05
 */
public class SpecifiedIndexAllFalseIsRunning implements ExecutionStrategy{

    private final int[] index;

    public SpecifiedIndexAllFalseIsRunning(int... index) {
        this.index = index;
    }

    @Override
    public boolean isExecution(RunnableActuator.CurrentEnvResult currentEnvResult) {
        for (int i : index) {
            if (currentEnvResult.getResult(i).getBoolean()) {
                return false;
            }
        }
        return true;
    }
}
