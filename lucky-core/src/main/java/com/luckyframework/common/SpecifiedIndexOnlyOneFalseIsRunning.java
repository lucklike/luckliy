package com.luckyframework.common;

/**
 * 指定的索引只要有一个为false时执行
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/14 06:05
 */
public class SpecifiedIndexOnlyOneFalseIsRunning implements ExecutionStrategy{

    private final int[] index;

    public SpecifiedIndexOnlyOneFalseIsRunning(int... index) {
        this.index = index;
    }

    @Override
    public boolean isExecution(RunnableActuator.CurrentEnvResult currentEnvResult) {
        for (int i : index) {
            if (!currentEnvResult.getResult(i).getBoolean()) {
                return true;
            }
        }
        return true;
    }
}
