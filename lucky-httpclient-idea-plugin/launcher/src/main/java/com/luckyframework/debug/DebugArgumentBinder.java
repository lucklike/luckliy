package com.luckyframework.debug;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 把协议中的类型标签参数还原为方法参数。
 *
 * 类型标签由插件侧按 PSI 目标类型生成；这里以
 * Method#getGenericParameterTypes() 为准做二次校验与转换。
 * 嵌套值没有 type 字段时交给 Jackson 按目标泛型转换，
 * 用于 List/Map/POJO 内部的元素。
 */
final class DebugArgumentBinder {

    private final ObjectMapper mapper = DebugJson.MAPPER;

    Object[] bind(JsonNode arguments, Type[] types) throws DebugException {
        if (arguments == null || !arguments.isArray()) {
            throw new DebugException(DebugProtocol.ERROR_ARGUMENT_CONVERT,
                    "arguments 必须是数组", null);
        }
        if (arguments.size() != types.length) {
            throw new DebugException(DebugProtocol.ERROR_ARGUMENT_CONVERT,
                    "参数数量不匹配：期望 " + types.length + "，实际 " + arguments.size(), null);
        }
        Object[] values = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            try {
                values[i] = bindOne(arguments.get(i), types[i]);
            } catch (DebugException e) {
                throw e;
            } catch (Exception e) {
                throw new DebugException(DebugProtocol.ERROR_ARGUMENT_CONVERT,
                        "第 " + (i + 1) + " 个参数转换失败：" + DebugJson.messageOf(e),
                        DebugJson.stackOf(e));
            }
        }
        return values;
    }

    private Object bindOne(JsonNode node, Type type) throws Exception {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.has("type")) {
            // 嵌套值（List/Map/POJO 内部）没有类型标签，按目标类型直接转换
            return mapper.convertValue(node, mapper.getTypeFactory().constructType(type));
        }
        String kind = DebugJson.text(node, "type", "null");
        if ("null".equals(kind)) {
            return null;
        }

        Class<?> raw = rawClass(type);
        if ("string".equals(kind)) {
            return DebugJson.text(node, "value", "");
        }
        if ("boolean".equals(kind)) {
            return booleanOf(node, raw);
        }
        if ("int".equals(kind)) {
            return intOf(node, raw);
        }
        if ("long".equals(kind)) {
            return longOf(node);
        }
        if ("double".equals(kind)) {
            return doubleOf(node, raw);
        }
        if ("char".equals(kind)) {
            return charOf(node);
        }
        if ("decimal".equals(kind)) {
            return new BigDecimal(DebugJson.text(node, "value", "0"));
        }
        if ("bigint".equals(kind)) {
            return new BigInteger(DebugJson.text(node, "value", "0"));
        }
        if ("enum".equals(kind)) {
            return enumOf(node, raw);
        }
        if ("file".equals(kind)) {
            String path = DebugJson.text(node, "value", "");
            return raw == Path.class ? Paths.get(path) : new File(path);
        }
        if ("base64".equals(kind)) {
            byte[] bytes = Base64.getDecoder().decode(DebugJson.text(node, "value", ""));
            if (raw == InputStream.class) {
                return new ByteArrayInputStream(bytes);
            }
            if (raw == Reader.class) {
                return new StringReader(new String(bytes, StandardCharsets.UTF_8));
            }
            return bytes;
        }
        if ("list".equals(kind)) {
            return listOf(node.path("value"), type);
        }
        if ("map".equals(kind)) {
            return mapOf(node.path("value"), type);
        }
        if ("object".equals(kind)) {
            String json = DebugJson.text(node, "json", "null");
            return mapper.readValue(json, mapper.getTypeFactory().constructType(type));
        }
        throw new DebugException(DebugProtocol.ERROR_ARGUMENT_CONVERT,
                "未知参数类型标签：" + kind, null);
    }

    private Object booleanOf(JsonNode node, Class<?> raw) {
        return Boolean.valueOf(node.path("value").asBoolean());
    }

    private Object intOf(JsonNode node, Class<?> raw) throws DebugException {
        long value = node.path("value").asLong();
        if (raw == byte.class || raw == Byte.class) {
            requireRange(value, Byte.MIN_VALUE, Byte.MAX_VALUE, raw);
            return Byte.valueOf((byte) value);
        }
        if (raw == short.class || raw == Short.class) {
            requireRange(value, Short.MIN_VALUE, Short.MAX_VALUE, raw);
            return Short.valueOf((short) value);
        }
        if (raw == long.class || raw == Long.class) {
            return Long.valueOf(value);
        }
        if (raw == BigInteger.class) {
            return BigInteger.valueOf(value);
        }
        requireRange(value, Integer.MIN_VALUE, Integer.MAX_VALUE, raw);
        return Integer.valueOf((int) value);
    }

    private void requireRange(long value, long min, long max, Class<?> raw) throws DebugException {
        if (value < min || value > max) {
            throw new DebugException(DebugProtocol.ERROR_ARGUMENT_CONVERT,
                    "数值 " + value + " 超出 " + raw.getTypeName() + " 的取值范围", null);
        }
    }

    private Object longOf(JsonNode node) throws DebugException {
        String text = DebugJson.text(node, "value", "0").trim();
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            throw new DebugException(DebugProtocol.ERROR_ARGUMENT_CONVERT,
                    "不是合法 long：" + text, null);
        }
    }

    private Object doubleOf(JsonNode node, Class<?> raw) throws DebugException {
        String text = DebugJson.text(node, "value", "0").trim();
        boolean single = raw == float.class || raw == Float.class;
        if ("NaN".equals(text)) {
            return single ? Float.valueOf(Float.NaN) : Double.valueOf(Double.NaN);
        }
        if ("Infinity".equals(text)) {
            return single ? Float.valueOf(Float.POSITIVE_INFINITY) : Double.valueOf(Double.POSITIVE_INFINITY);
        }
        if ("-Infinity".equals(text)) {
            return single ? Float.valueOf(Float.NEGATIVE_INFINITY) : Double.valueOf(Double.NEGATIVE_INFINITY);
        }
        try {
            double parsed = Double.parseDouble(text);
            return single ? Float.valueOf((float) parsed) : Double.valueOf(parsed);
        } catch (NumberFormatException e) {
            throw new DebugException(DebugProtocol.ERROR_ARGUMENT_CONVERT,
                    "不是合法浮点数：" + text, null);
        }
    }

    private Object charOf(JsonNode node) throws DebugException {
        String text = DebugJson.text(node, "value", "");
        if (text.length() != 1) {
            throw new DebugException(DebugProtocol.ERROR_ARGUMENT_CONVERT,
                    "char 参数只接受单个字符：" + text, null);
        }
        return Character.valueOf(text.charAt(0));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object enumOf(JsonNode node, Class<?> raw) throws DebugException {
        if (!raw.isEnum()) {
            throw new DebugException(DebugProtocol.ERROR_ARGUMENT_CONVERT,
                    "目标类型不是枚举：" + raw.getName(), null);
        }
        String name = DebugJson.text(node, "value", "");
        try {
            return Enum.valueOf((Class<? extends Enum>) raw, name);
        } catch (IllegalArgumentException e) {
            throw new DebugException(DebugProtocol.ERROR_ARGUMENT_CONVERT,
                    "枚举值不存在：" + raw.getName() + "." + name, null);
        }
    }

    private Object listOf(JsonNode values, Type type) throws Exception {
        Class<?> raw = rawClass(type);
        Type elementType = elementTypeOf(type);
        if (raw.isArray()) {
            Class<?> component = raw.getComponentType();
            int size = values != null && values.isArray() ? values.size() : 0;
            Object array = Array.newInstance(component, size);
            for (int i = 0; i < size; i++) {
                Array.set(array, i, bindOne(values.get(i), component));
            }
            return array;
        }

        Collection<Object> collection;
        if (Set.class.isAssignableFrom(raw)) {
            collection = new LinkedHashSet<Object>();
        } else if (raw.isInterface() || raw.isAssignableFrom(ArrayList.class)) {
            collection = new ArrayList<Object>();
        } else {
            collection = (Collection<Object>) raw.newInstance();
        }
        if (values != null && values.isArray()) {
            for (JsonNode value : values) {
                collection.add(bindOne(value, elementType));
            }
        }
        return collection;
    }

    private Object mapOf(JsonNode values, Type type) throws Exception {
        Class<?> raw = rawClass(type);
        Type keyType = String.class;
        Type valueType = Object.class;
        if (type instanceof ParameterizedType) {
            Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
            if (arguments.length == 2) {
                keyType = unwrap(arguments[0]);
                valueType = unwrap(arguments[1]);
            }
        }
        if (keyType != String.class && keyType != Object.class) {
            throw new DebugException(DebugProtocol.ERROR_ARGUMENT_CONVERT,
                    "P1 只支持 String 作为 Map 的键类型，实际为 " + keyType.getTypeName(), null);
        }

        Map<Object, Object> map;
        if (raw.isInterface() || raw.isAssignableFrom(LinkedHashMap.class)) {
            map = new LinkedHashMap<Object, Object>();
        } else {
            @SuppressWarnings("unchecked")
            Map<Object, Object> created = (Map<Object, Object>) raw.newInstance();
            map = created;
        }
        if (values != null && values.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = values.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                map.put(entry.getKey(), bindOne(entry.getValue(), valueType));
            }
        }
        return map;
    }

    private Type elementTypeOf(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
            if (arguments.length == 1) {
                return unwrap(arguments[0]);
            }
        }
        if (type instanceof Class && ((Class<?>) type).isArray()) {
            return ((Class<?>) type).getComponentType();
        }
        return Object.class;
    }

    private Type unwrap(Type type) {
        if (type instanceof WildcardType) {
            Type[] upper = ((WildcardType) type).getUpperBounds();
            return upper.length > 0 ? upper[0] : Object.class;
        }
        return type;
    }

    private Class<?> rawClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        if (type instanceof WildcardType) {
            Type[] upper = ((WildcardType) type).getUpperBounds();
            return upper.length > 0 ? rawClass(upper[0]) : Object.class;
        }
        return Object.class;
    }
}
