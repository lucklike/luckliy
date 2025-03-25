package com.luckyframework.httpclient.generalapi.openai;

import com.luckyframework.common.ConfigurationMap;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tool注册器，用于搜集和管理所有的Tool信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/25 14:50
 */
public class ToolRegister {

    /**
     * 所有的函数名-函数描述信息的映射关系
     */
    private final Map<String, ConfigurationMap> functionMap = new LinkedHashMap<>();


    /**
     * 获取所有函数名与函数描述信息的映射
     *
     * @return 所有函数名与函数描述信息的映射
     */
    public Map<String, ConfigurationMap> getFunctionMap() {
        return this.functionMap;
    }

    /**
     * 获取所有函数描述信息
     *
     * @return 所有函数描述信息
     */
    public Collection<ConfigurationMap> getFunctions() {
        return this.functionMap.values();
    }

    /**
     * 获取所有的函数名
     *
     * @return 所有的函数名
     */
    public Set<String> getFunctionNames() {
        return this.functionMap.keySet();
    }

    /**
     * 根据函数名来获取函数描述信息
     *
     * @param functionName 函数名
     * @return 函数描述信息
     */
    public ConfigurationMap getFunction(String functionName) {
        return this.functionMap.get(functionName);
    }

    /**
     * 添加函数对象中所有的工具方法
     *
     * @param functionObject 函数所在对象
     */
    public void addFunctionObject(@NonNull Object functionObject) {
        Assert.notNull(functionObject, "functionObject must not be null");
    }

}
