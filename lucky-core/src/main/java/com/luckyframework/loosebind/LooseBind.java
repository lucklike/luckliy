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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

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
    public void binding(Object bean, Map<String, Object> configProperties) throws FieldUnknownException, FieldInvalidException {
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

    public static void checkConfigProperties(List<FieldAlias> requiredFieldAliases, Map<String, Object> configProperties, char[] toHump) throws NotFoundRequiredFieldException {
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

    private static boolean containsKey(KeyCaseSensitivityMap<Object> kcsMap, Set<String> keys, char[] toHump) {
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
            // a.使用configKey直接去配置项中去进行匹配
            for (char to : toHump) {

                // a.尝试转成驼峰格式后进行匹配
                String toKey1 = StringUtils.otherFormatsToCamel(configKey, to);
                if (injectionMap.containsKey(toKey1)) {
                    injection = injectionMap.get(toKey1);
                    break;
                }

                // a.尝试将驼峰转成其他之后进行匹配
                String toKey2 = StringUtils.humpToOtherFormats(configKey, String.valueOf(to));
                if (injectionMap.containsKey(toKey2)) {
                    injection = injectionMap.get(toKey2);
                    break;
                }
            }

            // b.尝试使用configKey去与别名配置进行匹配
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
        private Object factor;
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

        public InjectionFactorBuilder setFactor(Object factor) {
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
        private final Object factor;
        /**
         * 值
         */
        private final Object factoryValue;
        /**
         * 转化的目标类型
         */
        private final ResolvableType factoryType;

        InjectionFactor(LooseBind looseBind, Object beanInstance, Object factor, Object factoryValue) {
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
            return getLooseBindPojo(factoryValue, factoryType);
        }

        // 动态绑定对象类型
        private Object getLooseBindPojo(Object value, ResolvableType type) throws Exception {
            Class<?> clazz = Objects.requireNonNull(type.getRawClass());
            // 基本类型
            if (isNotLooseBind(clazz)){
                return getLooseBindBaseType(value, type);
            }

            // 集合类型
            if (Collection.class.isAssignableFrom(clazz)) {
                return getLooseBindCollection(value, type);
            }

            // 数组
            if (clazz.isArray()) {
                return getLooseBindArray(value, type);
            }

            // Map
            if (Map.class.isAssignableFrom(clazz)) {
                return getLooseBindMap(value, type);
            }

            // Pojo
            Object fieldObject = ClassUtils.newObject(type.getRawClass());
            looseBind.binding(fieldObject, ConversionUtils.conversion(value, new SerializationTypeToken<Map<String, Object>>() {
            }));
            return fieldObject;
        }


        // 动态绑定基本类型
        private Object getLooseBindBaseType(Object value, ResolvableType type) {
            return ConversionUtils.conversion(value, type);
        }


        private Object getLooseBindCollection(Object value, ResolvableType type) throws Exception {
            Class<?> clazz = Objects.requireNonNull(type.getRawClass());
            Class<?> elementType = ContainerUtils.getElementType(type);

            Supplier<Object> collectionSupplier;
            if (List.class.isAssignableFrom(clazz)) {
                collectionSupplier = ArrayList::new;
            } else if (Set.class.isAssignableFrom(clazz)) {
                collectionSupplier = LinkedHashSet::new;
            } else {
                throw new IllegalArgumentException("Unknown injection factor type !");
            }

            List<Object> listObject = ConversionUtils.conversion(value, new SerializationTypeToken<List<Object>>() {});
            Collection collection = (Collection) ClassUtils.createObject(clazz, collectionSupplier);
            for (Object elementValue : ContainerUtils.getIterable(listObject)) {
                Object object = ClassUtils.newObject(elementType);
                collection.add(getLooseBindPojo(elementValue, type.getGeneric(0)));
            }
            return collection;
        }

        public Object getLooseBindArray(Object value, ResolvableType type) throws Exception {
            Class<?> clazz = Objects.requireNonNull(type.getRawClass());
            Class<?> elementType = ContainerUtils.getElementType(type);

            Object[] arrayObject = ConversionUtils.conversion(value, Object[].class);
            Object array = Array.newInstance(elementType, ContainerUtils.getIteratorLength(value));
            int index = 0;
            for (Object elementValue : ContainerUtils.getIterable(arrayObject)) {
                Object object = ClassUtils.newObject(elementType);
                Array.set(array, index++, getLooseBindPojo(object, type.getComponentType()));
            }
            return array;
        }

        private Object getLooseBindMap(Object value, ResolvableType type) throws Exception{
            Class<?> clazz = Objects.requireNonNull(type.getRawClass());
            Class<?> elementType = ContainerUtils.getElementType(type);

            Map<Object, Object> mapValue = ConversionUtils.conversion(value, new SerializationTypeToken<Map<Object, Object>>() {});
            Map map = (Map) ClassUtils.createObject(clazz, LinkedHashMap::new);

            for (Map.Entry<Object, Object> entry : mapValue.entrySet()) {
                Object key = entry.getKey();
                Object object = entry.getValue();

                map.put(getLooseBindPojo(key, type.getGeneric(0)), getLooseBindPojo(object, type.getGeneric(1)));
            }

            return map;
        }

        private boolean isNotLooseBind(Class<?> type) {
            return Object.class == type || ClassUtils.isSimpleBaseType(type) || Class.class.isAssignableFrom(type) || type.isEnum();
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
