package com.luckyframework.environment;

/**
 * 环境变量工厂
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/15 上午2:45
 */
public class EnvironmentFactory {

    private static LuckyConfigurationEnvironment environment;

    public static LuckyStandardEnvironment defaultEnvironment(){
        if(environment == null){
            environment = new LuckyConfigurationEnvironment();
        }
        return environment;
    }

}
