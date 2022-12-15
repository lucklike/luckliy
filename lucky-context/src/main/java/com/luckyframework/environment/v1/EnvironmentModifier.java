package com.luckyframework.environment.v1;

import com.luckyframework.annotations.DisableProxy;

import java.util.List;
import java.util.Map;

/**
 * 环境变量修改器，用于在运行时修改环境中变量的值，并同步更新单实例bean中注入的环境变量值
 * 基于观察者模式实现
 * @author fk7075
 * @version 1.0
 * @date 2021/11/14 1:38 下午
 */
@DisableProxy
public class EnvironmentModifier {

    private final Map<String, List<RuntimeModifier>> runtimeModifierMap;

    public EnvironmentModifier(Map<String, List<RuntimeModifier>> runtimeModifierMap) {
        this.runtimeModifierMap = runtimeModifierMap;
    }

    public void setEnvironmentValue(String key,Object value){
        String exKey = "${"+key+"}";
        if(runtimeModifierMap.containsKey(exKey)){
            List<RuntimeModifier> runtimeModifiers = runtimeModifierMap.get(exKey);
            for (RuntimeModifier modifier : runtimeModifiers) {
                modifier.setEnvironmentValue(key, value);
            }
        }
    }
}
