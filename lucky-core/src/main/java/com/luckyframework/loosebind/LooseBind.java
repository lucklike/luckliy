package com.luckyframework.loosebind;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.KeyCaseSensitivityMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 松散绑定
 *
 * @author fukang
 * @version 1.0.0
 * @date 2022/12/22 12:41
 */
@SuppressWarnings("all")
public class LooseBind {

    public static final char[] DEFAULT_TO_HUMP = {'-', '_'};

    /**
     * 忽略无效字段
     */
    private final boolean ignoreInvalidField;
    /**
     * 忽略未知字段
     */
    private final boolean ignoreUnknownField;
    /**
     * 是否启用字段优先的匹配方式
     */
    private final boolean isFieldPriorityMatch;
    /**
     * 松散绑定中指定的分隔符
     */
    private final char[] toHump;
    /**
     * 属性别名配置
     */
    private final List<FieldAlias> fieldAliasList = new ArrayList<>();
    /**
     * 注入因子过滤器
     */
    private InjectionFactorFilter factorBuilderFilter = InjectionFactorFilter.ALWAYS_TRUE_INSTANCE;

    public LooseBind(boolean ignoreInvalidField, boolean ignoreUnknownField, boolean isFieldPriorityMatch, char[] toHump) {
        this.ignoreInvalidField = ignoreInvalidField;
        this.ignoreUnknownField = ignoreUnknownField;
        this.isFieldPriorityMatch = isFieldPriorityMatch;
        this.toHump = toHump;
    }

    public LooseBind() {
        this(false, true, false, DEFAULT_TO_HUMP);
    }

    public void addFieldAlias(FieldAlias fieldAlias) {
        this.fieldAliasList.add(fieldAlias);
    }

    public void setFactorBuilderFilter(InjectionFactorFilter factorBuilderFilter) {
        this.factorBuilderFilter = factorBuilderFilter;
    }

    /**
     * 给Bean注入环境变量中属性
     *
     * @param bean             Bean实例
     * @param configProperties 配置项
     */
    public void binding(@NonNull Object bean, Map<String, Object> configProperties) throws FieldUnknownException, FieldInvalidException {
        if (ContainerUtils.isEmptyMap(configProperties)) {
            return;
        }
        Class<?> beanClass = bean.getClass();
        Map<String, Field> nameFieldMap = ClassUtils.getNameFieldMap(beanClass);
        Map<String, Method> nameSetterMethodMap = ClassUtils.getAllSetterMethodMap(beanClass);

        for (Map.Entry<String, Object> confEntry : configProperties.entrySet()) {
            String configKey = confEntry.getKey();
            Object configValue = confEntry.getValue();
            InjectionFactorBuilder injectionFactorBuilder;
            // 优先匹配原则 --> 属性匹配优先
            if (isFieldPriorityMatch) {
                injectionFactorBuilder = findFieldInjectionFactorBuilder(configKey, configValue, nameFieldMap);
                injectionFactorBuilder = injectionFactorBuilder == null ? findMethodInjectionFactorBuilder(configKey, configValue, nameSetterMethodMap) : injectionFactorBuilder;
            }
            // 优先匹配原则 --> 方法匹配优先
            else {
                injectionFactorBuilder = findMethodInjectionFactorBuilder(configKey, configValue, nameSetterMethodMap);
                injectionFactorBuilder = injectionFactorBuilder == null ? findFieldInjectionFactorBuilder(configKey, configValue, nameFieldMap) : injectionFactorBuilder;
            }


            if (injectionFactorBuilder == null && !ignoreUnknownField) {
                throw new FieldUnknownException(configKey);
            }

            if (injectionFactorBuilder != null && factorBuilderFilter.pass(injectionFactorBuilder)) {
                try {
                    injectionFactorBuilder.setBeanInstance(bean).builder().injection();
                } catch (Exception e) {
                    if (!ignoreInvalidField) {
                        throw new FieldInvalidException(configKey, injectionFactorBuilder.getFactor(), e);
                    }
                }
            }
        }
    }

    /**
     * 将某个对象松散绑定到某个类型上，并返回该类型的实例
     *
     * @param beanType    松散绑定目标类型
     * @param configValue 数据源对象
     * @return 松散绑定之后的对象
     * @throws Exception 绑定过程中可能出现的异常
     */
    public <T> T binding(@NonNull SerializationTypeToken<T> beanType, Object configValue) throws Exception {
        return (T) binding(ResolvableType.forType(beanType.getType()), configValue);
    }

    /**
     * 将某个对象松散绑定到某个类型上，并返回该类型的实例
     *
     * @param beanType    松散绑定目标类型
     * @param configValue 数据源对象
     * @return 松散绑定之后的对象
     * @throws Exception 绑定过程中可能出现的异常
     */
    public <T> T binding(@NonNull Class<T> beanType, Object configValue) throws Exception {
        return (T) binding(ResolvableType.forClass(beanType), configValue);
    }

    /**
     * 将某个对象松散绑定到某个类型上，并返回该类型的实例
     *
     * @param beanType    松散绑定目标类型
     * @param configValue 数据源对象
     * @return 松散绑定之后的对象
     * @throws Exception 绑定过程中可能出现的异常
     */
    public Object binding(@NonNull ResolvableType beanType, Object configValue) throws Exception {
        if (configValue == null) {
            return null;
        }
        Class<?> clazz = Objects.requireNonNull(beanType.getRawClass());
        // 基本类型
        if (isNotLooseBind(clazz)) {
            return bindingBaseType(configValue, beanType);
        }

        // 集合类型
        if (Collection.class.isAssignableFrom(clazz)) {
            return bindingCollection(configValue, beanType);
        }

        // 数组
        if (clazz.isArray()) {
            return bindingArray(configValue, beanType);
        }

        // Map
        if (Map.class.isAssignableFrom(clazz)) {
            return bindingMap(configValue, beanType);
        }

        // Pojo
        Object fieldObject = createObject(beanType.getRawClass());
        binding(fieldObject, ConversionUtils.conversion(configValue, new SerializationTypeToken<Map<String, Object>>() {
        }));
        return fieldObject;
    }

    // 松散绑定基本类型
    private Object bindingBaseType(Object configValue, @NonNull ResolvableType type) {
        return ConversionUtils.conversion(configValue, type);
    }

    // 松散绑定集合类型
    private Object bindingCollection(Object configValue, @NonNull ResolvableType type) throws Exception {
        Class<?> clazz = Objects.requireNonNull(type.getRawClass());
        Class<?> elementType = ContainerUtils.getElementType(type);

        Collection collection = (Collection) createObject(clazz);
        if (ContainerUtils.isIterable(configValue)) {
            for (Object elementValue : ContainerUtils.getIterable(configValue)) {
                collection.add(binding(type.getGeneric(0), elementValue));
            }
        } else {
            List<Object> listObject = ConversionUtils.conversion(configValue, new SerializationTypeToken<List<Object>>() {
            });
            for (Object elementValue : listObject) {
                collection.add(binding(type.getGeneric(0), elementValue));
            }
        }
        return collection;
    }

    // 松散绑定数组类型
    private Object bindingArray(Object configValue, @NonNull ResolvableType type) throws Exception {
        Class<?> clazz = Objects.requireNonNull(type.getRawClass());
        Class<?> elementType = ContainerUtils.getElementType(type);

        if (ContainerUtils.isIterable(configValue)) {
            Object array = Array.newInstance(elementType, ContainerUtils.getIteratorLength(configValue));
            int index = 0;
            for (Object elementValue : ContainerUtils.getIterable(configValue)) {
                Array.set(array, index++, binding(type.getComponentType(), elementValue));
            }
            return array;
        } else {
            Object[] arrayObject = ConversionUtils.conversion(configValue, Object[].class);
            Object array = Array.newInstance(elementType, arrayObject.length);
            int index = 0;
            for (Object elementValue : arrayObject) {
                Array.set(array, index++, binding(type.getComponentType(), elementValue));
            }
            return array;
        }

    }

    // 松散绑定Map类型
    private Object bindingMap(Object configValue, @NonNull ResolvableType type) throws Exception {
        Class<?> clazz = Objects.requireNonNull(type.getRawClass());
        Class<?> elementType = ContainerUtils.getElementType(type);

        Map<Object, Object> mapValue = ConversionUtils.conversion(configValue, new SerializationTypeToken<Map<Object, Object>>() {
        });
        Map map = (Map) createObject(clazz);

        for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
            Object key = entry.getKey();
            Object object = entry.getValue();

            map.put(binding(type.getGeneric(0), key), binding(type.getGeneric(1), object));
        }

        return map;
    }

    // 是否为不支持松散绑定的类型
    private boolean isNotLooseBind(Class<?> type) {
        return Object.class == type || ClassUtils.isSimpleBaseType(type) || Class.class.isAssignableFrom(type) || type.isEnum();
    }

    // 使用Class创建对象
    private Object createObject(Class<?> clazz) {
        if (List.class.isAssignableFrom(clazz)) {
            return ClassUtils.createObject(clazz, ArrayList::new);
        }
        if (Set.class.isAssignableFrom(clazz)) {
            return ClassUtils.createObject(clazz, LinkedHashSet::new);
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return ClassUtils.createObject(clazz, LinkedHashMap::new);
        }
        return ClassUtils.newObject(clazz);
    }

    private void checkConfigProperties(List<FieldAlias> requiredFieldAliases, Map<String, Object> configProperties, char[] toHump) throws NotFoundRequiredFieldException {
        if (!requiredFieldAliases.isEmpty()) {
            KeyCaseSensitivityMap<Object> kcsMap = new KeyCaseSensitivityMap<>(configProperties);
            for (FieldAlias requiredFieldAlias : requiredFieldAliases) {
                String requiredFieldName = requiredFieldAlias.getName();
                Set<String> aliasSet = requiredFieldAlias.getAliases();
                aliasSet.add(requiredFieldName);
                if (!containsKey(kcsMap, aliasSet, toHump)) {
                    throw new NotFoundRequiredFieldException(aliasSet);
                }
            }
        }
    }

    private boolean containsKey(KeyCaseSensitivityMap<Object> kcsMap, Set<String> keys, char[] toHump) {
        for (String key : keys) {
            if (kcsMap.containsKey(key)) {
                return true;
            }
            for (char to : toHump) {

                String toKey2 = StringUtils.humpToOtherFormats(key, String.valueOf(to));
                if (kcsMap.containsKey(toKey2)) {
                    return true;
                }

                String toKey1 = StringUtils.otherFormatsToCamel(key, to);
                if (kcsMap.containsKey(toKey1)) {
                    return true;
                }

            }
        }
        return false;
    }

    /**
     * 获取一个基于方法的属性注入因子的构造器
     *
     * @param configKey       配置Key
     * @param configValue     配置Value
     * @param setterMethodMap Setter方法名与方法组成的Map
     * @return 基于方法的注入因子构造器
     */
    private InjectionFactorBuilder findMethodInjectionFactorBuilder(String configKey, Object configValue, Map<String, Method> setterMethodMap) {
        Map<String, Method> temp = new LinkedHashMap<>();
        setterMethodMap.forEach((k, v) -> temp.put(k.substring(3), v));
        Method method = getInjection(configKey, temp);
        return method == null ? null : new InjectionFactorBuilder().setLooseBind(this).setFactor(method).setFactoryValue(configValue);
    }

    /**
     * 获取一个基于属性的属性注入因子的构造器
     *
     * @param configKey    配置Key
     * @param configValue  配置Value
     * @param nameFieldMap 属性名与属性组成的Map
     * @return 基于属性的注入因子构造器
     */
    private InjectionFactorBuilder findFieldInjectionFactorBuilder(String configKey, Object configValue, Map<String, Field> nameFieldMap) {
        Field field = getInjection(configKey, nameFieldMap);
        return field == null ? null : new InjectionFactorBuilder().setLooseBind(this).setFactor(field).setFactoryValue(configValue);
    }

    /**
     * 获取一个注入因子
     *
     * @param configKey    配置Key
     * @param nameFieldMap 属性名与属性组成的Map
     * @param <T>          注入因子类型
     * @return 注入因子
     */
    private <T> T getInjection(String configKey, Map<String, T> nameFieldMap) {
        Map<String, T> injectionMap = new KeyCaseSensitivityMap<>(nameFieldMap);
        T injection = injectionMap.get(configKey);
        if (injection == null) {

            // a.去除掉分隔符之后再进行匹配
            for (char c : toHump) {
                configKey = configKey.replace(String.valueOf(c), "");
            }
            injection = injectionMap.get(configKey);

            // b.使用别名配置进行匹配
            if (injection == null) {
                injection = getInjectionByAlias(injectionMap, configKey);
            }
        }
        return injection;
    }

    // 通过别名配置来获取注入因子
    private <T> T getInjectionByAlias(Map<String, T> nameFieldMap, String configKey) {
        for (FieldAlias fieldAlias : fieldAliasList) {
            String fieldName = fieldAlias.getName();
            Set<String> aliases = fieldAlias.getAliases();

            boolean isEqualConfigKey = isEqualKey(configKey, fieldName);
            if (!isEqualConfigKey) {
                for (String alias : aliases) {
                    if (isEqualKey(configKey, alias)) {
                        isEqualConfigKey = true;
                        break;
                    }
                }
            }

            if (isEqualConfigKey && nameFieldMap.containsKey(fieldName)) {
                return nameFieldMap.get(fieldName);
            }
        }
        return null;
    }

    private boolean isEqualKey(String key, String testKey) {
        if (key.equalsIgnoreCase(testKey)) return true;
        for (char to : toHump) {
            String toKey = StringUtils.otherFormatsToCamel(key, to);
            String toTestKey = StringUtils.otherFormatsToCamel(testKey, to);
            if (toKey.equalsIgnoreCase(toTestKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 注入因子过滤器接口
     */
    public interface InjectionFactorFilter {

        InjectionFactorFilter ALWAYS_TRUE_INSTANCE = new AlwaysTrueInjectionFactorFilter();

        boolean pass(InjectionFactorBuilder factorBuilder);
    }

    /**
     * 结果永远为true的注入因子过滤器实现类
     */
    static class AlwaysTrueInjectionFactorFilter implements InjectionFactorFilter {
        @Override
        public boolean pass(InjectionFactorBuilder factorBuilder) {
            return true;
        }
    }

    /**
     * 注入因子构造器，用于生产一个{@link InjectionFactor}
     */
    @SuppressWarnings("all")
    public static class InjectionFactorBuilder {
        private LooseBind looseBind;
        private Object beanInstance;
        private AccessibleObject factor;
        private Object factoryValue;

        public Object getBeanInstance() {
            return beanInstance;
        }

        public Object getFactor() {
            return factor;
        }

        public Object getFactoryValue() {
            return factoryValue;
        }

        public InjectionFactorBuilder setLooseBind(LooseBind looseBind) {
            this.looseBind = looseBind;
            return this;
        }

        public InjectionFactorBuilder setBeanInstance(Object beanInstance) {
            this.beanInstance = beanInstance;
            return this;
        }

        public InjectionFactorBuilder setFactor(AccessibleObject factor) {
            this.factor = factor;
            return this;
        }

        public InjectionFactorBuilder setFactoryValue(Object factoryValue) {
            this.factoryValue = factoryValue;
            return this;
        }

        public InjectionFactor builder() {
            return new InjectionFactor(looseBind, beanInstance, factor, factoryValue);
        }

    }

    /**
     * 注入因子
     * 负责将值赋值给某个对象的某个属性或者执行其Setter方法
     */
    @SuppressWarnings("all")
    static class InjectionFactor {

        private final LooseBind looseBind;

        /**
         * Bean实例
         */
        private final Object beanInstance;
        /**
         * 注入因子[Field/Method]
         */
        private final AccessibleObject factor;
        /**
         * 值
         */
        private final Object factoryValue;
        /**
         * 转化的目标类型
         */
        private final ResolvableType factoryType;

        InjectionFactor(LooseBind looseBind, Object beanInstance, AccessibleObject factor, Object factoryValue) {
            this.looseBind = looseBind;
            this.beanInstance = beanInstance;
            this.factor = factor;
            this.factoryValue = factoryValue;
            if (factor instanceof Field) {
                factoryType = ResolvableType.forField((Field) factor);
            } else if (factor instanceof Method) {
                factoryType = ResolvableType.forType(((Method) factor).getGenericParameterTypes()[0]);
            } else {
                throw new IllegalArgumentException("Unknown injection factor type !");
            }
        }

        /**
         * 执行属性注入操作
         */
        public void injection() throws Exception {
            if (factor instanceof Field) {
                setFieldValue(beanInstance, (Field) factor, getInjectionValue());
            } else {
                invokeSetterMethod(beanInstance, (Method) factor, getInjectionValue());
            }
        }

        /**
         * 获取注入值
         *
         * @return 注入值
         */
        private Object getInjectionValue() throws Exception {
            return looseBind.binding(factoryType, factoryValue);
        }

        private void setFieldValue(Object object, Field field, Object fieldValue) {
            if (Modifier.isStatic(field.getModifiers())) {
                FieldUtils.setValue(object.getClass(), field, fieldValue);
            } else {
                FieldUtils.setValue(object, field, fieldValue);
            }
        }

        private void invokeSetterMethod(Object object, Method method, Object fieldValue) {
            if (Modifier.isStatic(method.getModifiers())) {
                MethodUtils.invoke(object.getClass(), method, fieldValue);
            } else {
                MethodUtils.invoke(object, method, fieldValue);
            }
        }
    }
}
