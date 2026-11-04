package com.luckyframework.httpclient.generalapi.ai.tool;

import com.luckyframework.conversion.ConversionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 按工具声明的参数JSON Schema绑定模型给出的参数：
 * 先按声明逐个取值并做单值类型转换（复用 {@link ConversionUtils}），再检查本层必填项，
 * 两类失败都以 {@link ToolArgumentsException} 表达，由回环引擎收敛成失败的工具结果。
 * <pre>
 *     Schema的层级与参数的层级一一对应：{@code properties} 里声明了什么就只取什么，
 *     嵌套对象的必填项落在该对象自己的 {@code required} 上，参数层的必填项落在根 {@code required} 上。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，参数绑定的实现细节，不属于对外承诺的公开接口
 */
public final class ToolArguments {

    private ToolArguments() {
    }

    /**
     * 按声明Schema取值并转换，同时校验本层必填项
     *
     * @param toolName 工具名，只用于组织错误说明
     * @param schema   参数JSON Schema
     * @param raw      从参数文本解析出来的原始参数集合
     * @return 按声明顺序取值的参数集合
     * @throws ToolArgumentsException 类型无法按声明转换，或缺少必填项
     */
    public static Map<String, Object> bind(String toolName, Map<String, Object> schema, Map<String, Object> raw) {
        Map<String, Object> source = raw == null ? Collections.<String, Object>emptyMap() : raw;
        Map<String, Object> properties = castMap(schema == null ? null : schema.get("properties"));

        Map<String, Object> bound = new LinkedHashMap<>();
        if (properties.isEmpty()) {
            // Schema没有声明任何属性时（远端MCP工具可能只给出空的object schema）原样透传，不做字段过滤
            bound.putAll(source);
        } else {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String name = entry.getKey();
                if (!source.containsKey(name)) {
                    continue;
                }
                bound.put(name, convert(toolName, name, castMap(entry.getValue()), source.get(name)));
            }
        }

        for (String name : castStringList(schema == null ? null : schema.get("required"))) {
            if (!bound.containsKey(name) || bound.get(name) == null) {
                throw new ToolArgumentsException("工具 [" + toolName + "] 缺少必填参数 [" + name + "]，"
                        + "请按工具声明补齐后重新调用。");
            }
        }
        return bound;
    }

    /**
     * 按声明类型转换单个参数值
     */
    private static Object convert(String toolName, String path, Map<String, Object> node, Object value) {
        if (value == null || node.isEmpty()) {
            return value;
        }
        Object typeNode = node.get("type");
        if (typeNode == null) {
            return value;
        }
        String type = String.valueOf(typeNode).toLowerCase(Locale.ROOT);
        if (isStructured(value) && !"object".equals(type) && !"array".equals(type)) {
            throw conversionFailure(toolName, path, type, value);
        }
        try {
            if ("string".equals(type)) {
                return ConversionUtils.conversion(value, String.class);
            }
            if ("integer".equals(type)) {
                return ConversionUtils.conversion(value, Long.class);
            }
            if ("number".equals(type)) {
                return ConversionUtils.conversion(value, Double.class);
            }
            if ("boolean".equals(type)) {
                return ConversionUtils.conversion(value, Boolean.class);
            }
            if ("array".equals(type)) {
                return convertArray(toolName, path, node, value);
            }
            if ("object".equals(type)) {
                if (!(value instanceof Map)) {
                    throw conversionFailure(toolName, path, type, value);
                }
                return bind(toolName, node, castMap(value));
            }
        } catch (ToolArgumentsException e) {
            throw e;
        } catch (RuntimeException e) {
            throw conversionFailure(toolName, path, type, value, e);
        }
        return value;
    }

    /**
     * 数组参数：逐个元素按 {@code items} 声明转换
     */
    private static Object convertArray(String toolName, String path, Map<String, Object> node, Object value) {
        Collection<?> elements = value instanceof Collection ? (Collection<?>) value
                : Collections.singletonList(value);
        Map<String, Object> items = castMap(node.get("items"));
        List<Object> converted = new ArrayList<>(elements.size());
        for (Object element : elements) {
            converted.add(convert(toolName, path + "[]", items, element));
        }
        return converted;
    }

    private static boolean isStructured(Object value) {
        return value instanceof Map || value instanceof Collection || value.getClass().isArray();
    }

    private static ToolArgumentsException conversionFailure(String toolName, String path, String type, Object value) {
        return new ToolArgumentsException("工具 [" + toolName + "] 的参数 [" + path + "] 无法按声明的类型 "
                + type + " 转换，实际值：" + value);
    }

    private static ToolArgumentsException conversionFailure(String toolName, String path, String type,
                                                            Object value, RuntimeException cause) {
        return new ToolArgumentsException("工具 [" + toolName + "] 的参数 [" + path + "] 无法按声明的类型 "
                + type + " 转换，实际值：" + value, cause);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object node) {
        return node instanceof Map ? (Map<String, Object>) node : new LinkedHashMap<String, Object>();
    }

    private static List<String> castStringList(Object node) {
        List<String> names = new ArrayList<>();
        if (node instanceof Collection) {
            for (Object element : (Collection<?>) node) {
                names.add(String.valueOf(element));
            }
        }
        return names;
    }
}
