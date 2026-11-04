package com.luckyframework.httpclient.generalapi.ai.tool;

import com.luckyframework.common.ConfigurationMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 将标注了{@link Tool}的方法翻译为一条工具声明：名称、描述、参数JSON Schema。
 * <pre>
 *     输出是 {@link ConfigurationMap} 表达的Map树，不建Schema DTO层次；
 *     properties 按参数（字段）声明顺序排列，enum 按常量声明顺序排列。
 *     生成结果按 Method 缓存，注册一次即复用——反射不允许进入回环循环。
 *
 *     全部失败都在注册期抛出 {@link ToolRegistryException}：
 *     描述缺失、参数名不可得、类型无法映射、嵌套超限或自引用。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，声明生成的实现细节；业务通过 {@link ToolRegistry#register(Object)} 登记注解工具即可
 */
final class ToolSchemaGenerator {

    /**
     * POJO嵌套展开的深度上限
     */
    static final int MAX_DEPTH = 4;

    /**
     * 编译未保留参数名时反射得到的合成名形态
     */
    private static final Pattern SYNTHETIC_NAME = Pattern.compile("arg\\d+");

    private static final Map<Class<?>, String> SIMPLE_TYPE_NAMES = new LinkedHashMap<>();
    private static final Set<Class<?>> TIME_TYPES = new HashSet<>(Arrays.asList(
            Date.class, java.sql.Date.class, java.sql.Time.class, java.sql.Timestamp.class, Calendar.class,
            LocalDate.class, LocalDateTime.class, LocalTime.class, OffsetDateTime.class, OffsetTime.class,
            ZonedDateTime.class, Instant.class, Year.class, YearMonth.class, MonthDay.class));

    private static final Map<Method, ConfigurationMap> SCHEMA_CACHE = new ConcurrentHashMap<>();

    static {
        SIMPLE_TYPE_NAMES.put(String.class, "string");
        SIMPLE_TYPE_NAMES.put(CharSequence.class, "string");
        SIMPLE_TYPE_NAMES.put(char.class, "string");
        SIMPLE_TYPE_NAMES.put(Character.class, "string");
        SIMPLE_TYPE_NAMES.put(boolean.class, "boolean");
        SIMPLE_TYPE_NAMES.put(Boolean.class, "boolean");
        SIMPLE_TYPE_NAMES.put(byte.class, "integer");
        SIMPLE_TYPE_NAMES.put(Byte.class, "integer");
        SIMPLE_TYPE_NAMES.put(short.class, "integer");
        SIMPLE_TYPE_NAMES.put(Short.class, "integer");
        SIMPLE_TYPE_NAMES.put(int.class, "integer");
        SIMPLE_TYPE_NAMES.put(Integer.class, "integer");
        SIMPLE_TYPE_NAMES.put(long.class, "integer");
        SIMPLE_TYPE_NAMES.put(Long.class, "integer");
        SIMPLE_TYPE_NAMES.put(BigInteger.class, "integer");
        SIMPLE_TYPE_NAMES.put(float.class, "number");
        SIMPLE_TYPE_NAMES.put(Float.class, "number");
        SIMPLE_TYPE_NAMES.put(double.class, "number");
        SIMPLE_TYPE_NAMES.put(Double.class, "number");
        SIMPLE_TYPE_NAMES.put(BigDecimal.class, "number");
        for (Class<?> timeType : TIME_TYPES) {
            SIMPLE_TYPE_NAMES.put(timeType, "string");
        }
    }

    private ToolSchemaGenerator() {
    }

    /**
     * 生成完整工具声明，工具名默认取方法名、描述缺失时抛异常
     */
    public static ToolSpec generateSpec(Method method) {
        Tool tool = method.getAnnotation(Tool.class);
        if (tool == null) {
            throw new ToolRegistryException("Method [" + describe(method) + "] is not annotated with @Tool.");
        }
        String name = tool.name().trim().isEmpty() ? method.getName() : tool.name().trim();
        String desc = tool.desc().trim();
        if (desc.isEmpty()) {
            throw new ToolRegistryException("Tool description is missing, please declare desc on the @Tool of method ["
                    + describe(method) + "].");
        }
        return new ToolSpec(name, desc, generateParameters(method));
    }

    /**
     * 生成方法参数的JSON Schema，同一Method只反射一次
     */
    public static ConfigurationMap generateParameters(Method method) {
        ConfigurationMap cached = SCHEMA_CACHE.get(method);
        if (cached != null) {
            return cached;
        }
        ConfigurationMap schema = new ConfigurationMap(buildMethodSchema(method));
        SCHEMA_CACHE.put(method, schema);
        return schema;
    }

    private static Map<String, Object> buildMethodSchema(Method method) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Attributes attributes = read(parameter);
            if (attributes.ignore) {
                continue;
            }
            String name = resolvedParameterName(parameter, method, i);
            Class<?> rawClass = rawClassOf(parameter.getParameterizedType());
            Map<String, Object> property = buildProperty(parameter.getParameterizedType(), attributes,
                    new IdentityHashMap<>(), new ArrayList<>(), 1);
            properties.put(name, property);
            // Optional只表示「可以不填」，类型本身按包裹的T展开
            if (attributes.required && rawClass != Optional.class) {
                required.add(name);
            }
        }
        return objectSchema(properties, required);
    }

    /**
     * 由Java类型与注解属性得出单个属性的Schema
     */
    private static Map<String, Object> buildProperty(Type type, Attributes attributes,
                                                     IdentityHashMap<Class<?>, Boolean> visiting,
                                                     List<Class<?>> path, int depth) {
        Map<String, Object> property = new LinkedHashMap<>();
        Class<?> rawClass = rawClassOf(type);

        if (!attributes.type.isEmpty()) {
            property.put("type", attributes.type);
            appendDescriptionAndEnum(property, attributes);
            return property;
        }
        if (rawClass == Optional.class) {
            return buildProperty(actualTypeArgument(type, 0), attributes, visiting, path, depth);
        }
        if (rawClass.isEnum()) {
            property.put("type", "string");
            if (attributes.enumerate.length > 0) {
                property.put("enum", Arrays.asList(attributes.enumerate));
            } else {
                List<String> constants = new ArrayList<>();
                for (Object constant : rawClass.getEnumConstants()) {
                    constants.add(((Enum<?>) constant).name());
                }
                property.put("enum", constants);
            }
            appendDescription(property, attributes);
            return property;
        }
        String simpleName = SIMPLE_TYPE_NAMES.get(rawClass);
        if (simpleName != null) {
            property.put("type", simpleName);
            appendDescriptionAndEnum(property, attributes);
            return property;
        }
        if (rawClass.isArray()) {
            property.put("type", "array");
            property.put("items", buildProperty(rawClass.getComponentType(), new Attributes(), visiting, path, depth));
            appendDescription(property, attributes);
            return property;
        }
        if (Collection.class.isAssignableFrom(rawClass)) {
            property.put("type", "array");
            property.put("items", buildProperty(actualTypeArgument(type, 0), new Attributes(), visiting, path, depth));
            appendDescription(property, attributes);
            return property;
        }
        if (isExpandablePojo(rawClass)) {
            return expandPojo(rawClass, attributes, visiting, path, depth);
        }
        throw new ToolRegistryException("The type [" + rawClass.getName() + "] cannot be mapped to a JSON Schema, "
                + "it is declared in [" + pathDescription(path) + "].");
    }

    /**
     * POJO展开为object：字段进properties、必填按层归属进入本层的required。
     * 引用栈在进入类型时登记、离开时撤销，用于识别自引用与相互引用。
     */
    private static Map<String, Object> expandPojo(Class<?> clazz, Attributes attributes,
                                                  IdentityHashMap<Class<?>, Boolean> visiting,
                                                  List<Class<?>> path, int depth) {
        if (visiting.containsKey(clazz)) {
            List<Class<?>> chain = new ArrayList<>(path);
            chain.add(clazz);
            throw new ToolRegistryException("Recursive type reference detected: " + pathDescription(chain)
                    + ", the tool schema cannot contain a self-reference.");
        }
        if (depth > MAX_DEPTH) {
            List<Class<?>> chain = new ArrayList<>(path);
            chain.add(clazz);
            throw new ToolRegistryException("The nesting depth of the tool schema exceeds the limit " + MAX_DEPTH
                    + ": " + pathDescription(chain));
        }
        visiting.put(clazz, Boolean.TRUE);
        path.add(clazz);
        try {
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("type", "object");
            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                if (isNonValueField(field)) {
                    continue;
                }
                Attributes fieldAttributes = read(field);
                if (fieldAttributes.ignore) {
                    continue;
                }
                properties.put(field.getName(), buildProperty(field.getGenericType(), fieldAttributes,
                        visiting, path, depth + 1));
                if (fieldAttributes.required) {
                    required.add(field.getName());
                }
            }
            property.put("properties", properties);
            appendRequired(property, required);
            appendDescription(property, attributes);
            return property;
        } finally {
            visiting.remove(clazz);
            path.remove(path.size() - 1);
        }
    }

    private static boolean isNonValueField(Field field) {
        return Modifier.isStatic(field.getModifiers()) || field.isSynthetic();
    }

    private static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        appendRequired(schema, required);
        return schema;
    }

    private static void appendRequired(Map<String, Object> target, List<String> required) {
        if (!required.isEmpty()) {
            target.put("required", new ArrayList<>(required));
        }
    }

    private static void appendDescription(Map<String, Object> target, Attributes attributes) {
        if (!attributes.desc.isEmpty()) {
            target.put("description", attributes.desc);
        }
    }

    private static void appendDescriptionAndEnum(Map<String, Object> target, Attributes attributes) {
        appendDescription(target, attributes);
        if (attributes.enumerate.length > 0) {
            target.put("enum", Arrays.asList(attributes.enumerate));
        }
    }

    /**
     * 参数名优先取编译期保留的真实名，得到 {@code arg0} 这类合成名时视为不可得
     */
    static String checkParameterName(String reflectedName, Class<?> owner, String methodName, int index) {
        if (reflectedName == null || SYNTHETIC_NAME.matcher(reflectedName).matches()) {
            throw new ToolRegistryException("The parameter name of [" + owner.getName() + "#" + methodName
                    + "] at position " + index + " is unavailable (got [" + reflectedName + "]), "
                    + "compile with -parameters or declare @Param(name=...) explicitly.");
        }
        return reflectedName;
    }

    /**
     * 解析出参数在schema里的最终名称（含 {@link Param#name()} 覆盖），
     * 绑定执行器时必须用同一个键取值，两处不能各算各的
     */
    static String resolvedParameterName(Parameter parameter, Method method, int index) {
        Attributes attributes = read(parameter);
        return attributes.name.isEmpty()
                ? checkParameterName(parameter.getName(), method.getDeclaringClass(), method.getName(), index)
                : attributes.name;
    }

    /**
     * 读取参数（字段）上的{@link Param}，并把{@link ParamDesc}、{@link Required}这类组合注解一并合并进来。
     * 直接标注的 {@link Param} 优先级最高。
     */
    private static Attributes read(AnnotatedElement element) {
        Attributes merged = new Attributes();
        for (Annotation annotation : element.getDeclaredAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (type == ParamDesc.class) {
                merged.desc = ((ParamDesc) annotation).value().trim();
                continue;
            }
            Param meta = type.getAnnotation(Param.class);
            if (meta != null) {
                merge(merged, meta);
            }
        }
        Param direct = element.getAnnotation(Param.class);
        if (direct != null) {
            merge(merged, direct);
        }
        return merged;
    }

    private static void merge(Attributes target, Param param) {
        if (!param.name().trim().isEmpty()) {
            target.name = param.name().trim();
        }
        if (!param.desc().trim().isEmpty()) {
            target.desc = param.desc().trim();
        }
        if (!param.type().trim().isEmpty()) {
            target.type = param.type().trim();
        }
        if (param.enumerate().length > 0) {
            target.enumerate = param.enumerate();
        }
        if (param.ignore()) {
            target.ignore = true;
        }
        if (param.required()) {
            target.required = true;
        }
    }

    private static Class<?> rawClassOf(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return rawClassOf(((ParameterizedType) type).getRawType());
        }
        throw new ToolRegistryException("The type [" + type.getTypeName() + "] cannot be mapped to a JSON Schema, "
                + "generic type variables and wildcards are not supported.");
    }

    private static Type actualTypeArgument(Type type, int index) {
        if (type instanceof ParameterizedType) {
            Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
            if (index < arguments.length) {
                return arguments[index];
            }
        }
        // 裸集合与裸Optional没有元素类型信息，交给Object处理必然映射失败，这里直接给出可诊断的错误
        throw new ToolRegistryException("The type [" + type.getTypeName()
                + "] has no element type declared, please use a generic type such as List<String>.");
    }

    private static boolean isExpandablePojo(Class<?> rawClass) {
        if (rawClass.isInterface() || Modifier.isAbstract(rawClass.getModifiers()) || rawClass.isEnum()
                || rawClass.isArray() || rawClass.isPrimitive() || Map.class.isAssignableFrom(rawClass)) {
            return false;
        }
        // JDK自身的类型（含InputStream这类IO句柄）一律不做字段展开，直接落到「无法映射」分支
        return !rawClass.getName().startsWith("java.") && !rawClass.getName().startsWith("javax.");
    }

    private static String pathDescription(List<Class<?>> path) {
        if (path.isEmpty()) {
            return "the root schema";
        }
        StringBuilder builder = new StringBuilder();
        for (Class<?> clazz : path) {
            if (builder.length() > 0) {
                builder.append(" -> ");
            }
            builder.append(clazz.getSimpleName());
        }
        return builder.toString();
    }

    private static String describe(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }

    /**
     * 一个参数（字段）合并后的注解取值
     */
    private static class Attributes {
        private String name = "";
        private String desc = "";
        private String type = "";
        private String[] enumerate = new String[0];
        private boolean ignore;
        private boolean required;
    }
}
