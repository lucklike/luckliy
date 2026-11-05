package com.luckyframework.httpclient.generalapi.ai.tool;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.reflect.MethodUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * 把标注了{@link Tool}的方法绑定为执行器：注册期一次性算出每个参数的名字与类型，
 * 执行时只按名字取值、按类型转换，反射查找不再进入回环循环。
 * <pre>
 *     模型给出的参数是 {@code Map<String,Object>}，缺少的选填项按各类型的安全默认值传入
 *     （基本类型取零值、引用类型取null、Optional取空），
 *     必填缺失与非法参数由回环引擎在执行器之外先行判定。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，注册表的实现细节，不属于对外承诺的公开接口
 */
final class ToolMethodBinder {

    private ToolMethodBinder() {
    }

    /**
     * 生成与方法一一对应的执行器
     *
     * @param toolHolder 方法所属的对象实例
     * @param method     标注了{@link Tool}的方法
     * @return 按声明参数取值并反射调用的执行器
     */
    static ToolExecutor bind(Object toolHolder, Method method) {
        method.setAccessible(true);
        Parameter[] parameters = method.getParameters();
        Binding[] bindings = new Binding[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String name = ToolSchemaGenerator.resolvedParameterName(parameter, method, i);
            Type declaredType = parameter.getParameterizedType();
            boolean optional = parameter.getType() == Optional.class;
            bindings[i] = new Binding(name, declaredType, parameter.getType(), optional);
        }
        return arguments -> {
            Map<String, Object> source = arguments == null ? Collections.<String, Object>emptyMap() : arguments;
            Object[] values = new Object[bindings.length];
            for (int i = 0; i < bindings.length; i++) {
                values[i] = bindings[i].resolve(source);
            }
            return MethodUtils.invoke(toolHolder, method, values);
        };
    }

    /**
     * 单个参数的绑定信息
     */
    private static class Binding {

        private final String name;
        private final Type declaredType;
        private final Class<?> rawType;
        private final boolean optional;

        Binding(String name, Type declaredType, Class<?> rawType, boolean optional) {
            this.name = name;
            this.declaredType = declaredType;
            this.rawType = rawType;
            this.optional = optional;
        }

        private Object resolve(Map<String, Object> source) {
            Object raw = source.get(name);
            if (optional) {
                return Optional.ofNullable(convert(raw, elementType()));
            }
            Object converted = convert(raw, declaredType);
            return converted == null ? defaultValue(rawType) : converted;
        }

        /**
         * Optional&lt;T&gt;上的T
         */
        private Type elementType() {
            if (declaredType instanceof ParameterizedType) {
                return ((ParameterizedType) declaredType).getActualTypeArguments()[0];
            }
            return Object.class;
        }

        private Object convert(Object raw, Type type) {
            if (raw == null) {
                return null;
            }
            Class<?> target = type instanceof Class ? (Class<?>) type
                    : (type instanceof ParameterizedType ? (Class<?>) ((ParameterizedType) type).getRawType() : Object.class);
            if (target.isEnum() && !(raw instanceof Enum)) {
                return ConversionUtils.conversion(raw, target);
            }
            if (target.isInstance(raw)) {
                return raw;
            }
            return ConversionUtils.conversion(raw, type);
        }

        /**
         * 缺省值：基本类型取零值，其余取null，避免反射赋值时的空指针噪声
         */
        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) {
                return Boolean.FALSE;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == double.class) {
                return 0D;
            }
            if (type == float.class) {
                return 0F;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            return (char) 0;
        }
    }
}
