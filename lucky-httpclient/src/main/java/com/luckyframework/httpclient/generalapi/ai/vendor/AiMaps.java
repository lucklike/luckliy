package com.luckyframework.httpclient.generalapi.ai.vendor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 报文节点的取值工具：厂商响应是外部可控输入，方言层一律显式取字段，
 * 因此需要一批「类型不符即视为缺失」的安全导航方法。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，不属于对外承诺的公开接口
 */
public final class AiMaps {

    private AiMaps() {
    }

    /**
     * 取节点值，非Map时返回空Map（表达「该节点缺失」而非抛异常）
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Object node, String key) {
        if (!(node instanceof Map)) {
            return Collections.emptyMap();
        }
        Object value = ((Map<String, Object>) node).get(key);
        return value instanceof Map ? (Map<String, Object>) value : Collections.<String, Object>emptyMap();
    }

    /**
     * 取数组节点，非List时返回空List
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getList(Object node, String key) {
        if (!(node instanceof Map)) {
            return Collections.emptyList();
        }
        Object value = ((Map<String, Object>) node).get(key);
        return value instanceof List ? (List<Object>) value : Collections.emptyList();
    }

    /**
     * 取字符串节点，非字符串时返回null
     */
    public static String getString(Map<String, Object> node, String key) {
        Object value = node.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 取整数节点，取值不可转为整数时返回null
     */
    public static Integer getInteger(Map<String, Object> node, String key) {
        Object value = node.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 节点是否存在给定键
     */
    public static boolean contains(Map<String, Object> node, String key) {
        return node != null && node.containsKey(key);
    }

    /**
     * 把节点集合转为Map集合，跳过非Map元素
     */
    public static List<Map<String, Object>> toMapList(List<Object> nodes) {
        List<Map<String, Object>> maps = new ArrayList<>(nodes.size());
        for (Object node : nodes) {
            if (node instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cast = (Map<String, Object>) node;
                maps.add(cast);
            }
        }
        return maps;
    }
}
