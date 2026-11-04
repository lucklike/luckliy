package com.luckyframework.httpclient.generalapi.ai.tool;

import com.luckyframework.common.ConfigurationMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具声明：名称、描述、参数JSON Schema。
 * 注解生成、编程式登记、远端MCP工具清单三种来源进入注册表后都是同一个数据结构。
 * 报文中的包装形状（{@code {"type":"function","function":{...}}}）属于统一模型的表达，
 * Anthropic/Gemini的结构差异由方言层直接取本对象字段组装，不消费该包装。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class ToolSpec {

    private final String name;
    private final String description;
    private final ConfigurationMap parameters;

    public ToolSpec(String name, String description, ConfigurationMap parameters) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be empty.");
        }
        this.name = name;
        this.description = description;
        this.parameters = parameters == null ? emptyParameters() : parameters;
    }

    /**
     * 构造一条工具声明
     */
    public static ToolSpec of(String name, String description, ConfigurationMap parameters) {
        return new ToolSpec(name, description, parameters);
    }

    /**
     * 空的参数Schema：{@code {"type":"object","properties":{}}}
     */
    public static ConfigurationMap emptyParameters() {
        Map<String, Object> properties = new LinkedHashMap<>();
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return new ConfigurationMap(schema);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 参数JSON Schema，形如 {@code {"type":"object","properties":{...},"required":[...]}}
     */
    public ConfigurationMap getParameters() {
        return parameters;
    }

    /**
     * 转为统一模型（OpenAI chat/completions兼容形状）里的工具声明：
     * 外层包一个 {@code type:function}，Schema放在 {@code function.parameters}
     */
    public Map<String, Object> toDeclaration() {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);

        Map<String, Object> declaration = new LinkedHashMap<>();
        declaration.put("type", "function");
        declaration.put("function", function);
        return declaration;
    }

    /**
     * 从统一模型形状的工具声明还原出ToolSpec
     */
    @SuppressWarnings("unchecked")
    public static ToolSpec fromDeclaration(Map<String, Object> declaration) {
        Object function = declaration.get("function");
        if (!(function instanceof Map)) {
            throw new IllegalArgumentException("Ill-formed tool declaration, the 'function' node is missing: " + declaration);
        }
        Map<String, Object> functionMap = (Map<String, Object>) function;
        Object parameters = functionMap.get("parameters");
        ConfigurationMap schema = parameters instanceof ConfigurationMap
                ? (ConfigurationMap) parameters
                : new ConfigurationMap((Map<String, Object>) parameters);
        return new ToolSpec((String) functionMap.get("name"), (String) functionMap.get("description"), schema);
    }

    @Override
    public String toString() {
        return "ToolSpec{name=" + name + ", description=" + description + ", parameters=" + parameters + "}";
    }
}
