package com.luckyframework.conversion;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.loosebind.LooseBind;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import com.luckyframework.spel.SpELRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.NonNull;
import org.springframework.util.Base64Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 万能的数据转换工具，可以将一个对象转换为另一种指定类型的对象，支持泛型嵌套转换
 *
 * <p><b>标量类型转换</b>：</p>
 * <p>String -> 基本类型（含byte[]/Byte[]与String之间的Base64编解码）<br/>
 * String -> 枚举类型（常量名精确匹配/忽略大小写匹配）<br/>
 * String -> Resource、File、InputStream、OutputStream、URL、URI<br/>
 * Object -> Class（类名转Class对象）</p>
 *
 * <p><b>容器类型转换</b>：</p>
 * <p>数组 -> 数组、数组 -> 集合、集合 -> 数组、集合 -> 集合<br/>
 * Map&lt;A,C&gt; -> Map&lt;B,D&gt;、Collection&lt;T&gt; -> Collection&lt;R&gt;、T[] -> R[]<br/>
 * String[]、Collection&lt;String&gt; -> 基本类型[]、枚举[]、Resource[]等（元素递归转换）<br/>
 * Iterator -> 集合/数组</p>
 *
 * <p><b>对象类型转换</b>：</p>
 * <p>Map -> 实体（支持@TargetField映射）、实体 -> Map（含循环引用检测）<br/>
 * T -> R（基于{@link ConversionService}的扩展转换）</p>
 *
 * <p><b>异常约定</b>：</p>    
 * <p>转换失败时统一抛出{@link TypeConversionException}（运行时异常，携带失败原因）</p>
 *
 * <p><b>其他能力</b>：</p>
 * <p>looseBind系列松散绑定、{@link #addProtocolResolver}协议解析器扩展、基于接口代理的转换服务</p>
 */
@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public abstract class ConversionUtils {

    /**
     * 默认的前置转换函数：将ConfigurationMap中的NULL_ENTRY占位符转换为null
     */
    private static final Function<Object, Object> DEFAULT_FUNCTION = s -> s == ConfigurationMap.NULL_ENTRY ? null : s;

    /**
     * 默认的转换服务列表（为空表示不启用额外转换服务）
     */
    private static final List<ConversionService> DEFAULT_CONVERSION_LIST = Collections.emptyList();

    /**
     * 默认的松散绑定对象
     */
    private static final LooseBind DEFAULT_LOOSE_BIND = new LooseBind();

    /**
     * Spring的资源解析对象
     */
    private static final PathMatchingResourcePatternResolver PATH_RESOURCE_SCANNER = new PathMatchingResourcePatternResolver();

    /**
     * 转换器接口代理对象缓存，key为转换接口的Class
     */
    private static final Map<Class<?>, Object> interfaceProxyCache = new ConcurrentHashMap<>(64);

    /**
     * 日志
     */
    private static final Logger log = LoggerFactory.getLogger(ConversionUtils.class);

    /**
     * 特殊类型转换器（单个对象），目标类型 -> 转换逻辑
     */
    private static final Map<Class<?>, Function<Object, Object>> SPECIAL_TYPE_CONVERTERS = new LinkedHashMap<>();

    /**
     * 特殊类型转换器（数组/集合元素），目标元素类型 -> 转换逻辑
     */
    private static final Map<Class<?>, Function<Object, Object>> SPECIAL_TYPE_ARRAY_CONVERTERS = new LinkedHashMap<>();

    static {
        SPECIAL_TYPE_CONVERTERS.put(Resource.class, ConversionUtils::conversionToResource);
        SPECIAL_TYPE_CONVERTERS.put(File.class, ConversionUtils::conversionToFile);
        SPECIAL_TYPE_CONVERTERS.put(InputStream.class, ConversionUtils::conversionToInputStream);
        SPECIAL_TYPE_CONVERTERS.put(OutputStream.class, ConversionUtils::conversionToOutputStream);
        SPECIAL_TYPE_CONVERTERS.put(URL.class, ConversionUtils::conversionToURL);
        SPECIAL_TYPE_CONVERTERS.put(URI.class, ConversionUtils::conversionToURI);

        SPECIAL_TYPE_ARRAY_CONVERTERS.put(Resource.class, ConversionUtils::conversionToResources);
        SPECIAL_TYPE_ARRAY_CONVERTERS.put(File.class, ConversionUtils::conversionToFileArray);
        SPECIAL_TYPE_ARRAY_CONVERTERS.put(InputStream.class, ConversionUtils::conversionToInputStreamArray);
        SPECIAL_TYPE_ARRAY_CONVERTERS.put(OutputStream.class, ConversionUtils::conversionToOutputStreamArray);
        SPECIAL_TYPE_ARRAY_CONVERTERS.put(URL.class, ConversionUtils::conversionToURLArray);
        SPECIAL_TYPE_ARRAY_CONVERTERS.put(URI.class, ConversionUtils::conversionToURIArray);
    }

    /**
     * 获取目标类型对应的特殊类型转换器
     *
     * @param targetType 目标类型
     * @return 转换器，不存在时返回null
     */
    private static Function<Object, Object> getSpecialTypeConverter(Class<?> targetType) {
        for (Map.Entry<Class<?>, Function<Object, Object>> entry : SPECIAL_TYPE_CONVERTERS.entrySet()) {
            if (entry.getKey().isAssignableFrom(targetType)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 获取目标元素类型对应的特殊类型数组转换器
     *
     * @param targetEntryType 目标元素类型
     * @return 转换器，不存在时返回null
     */
    private static Function<Object, Object> getSpecialTypeArrayConverter(Class<?> targetEntryType) {
        for (Map.Entry<Class<?>, Function<Object, Object>> entry : SPECIAL_TYPE_ARRAY_CONVERTERS.entrySet()) {
            if (entry.getKey().isAssignableFrom(targetEntryType)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 添加协议解析器
     *
     * @param protocolResolver 协议解析器
     */
    public static void addProtocolResolver(ProtocolResolver protocolResolver) {
        ResourceLoader resourceLoader = PATH_RESOURCE_SCANNER.getResourceLoader();
        if (resourceLoader instanceof DefaultResourceLoader) {
            ((DefaultResourceLoader) resourceLoader).addProtocolResolver(protocolResolver);
        }
    }

    /**
     * 获取一个转换器接口的代理对象
     * <p>注意：创建代理时会将接口中所有{@code @ConversionMethod}声明的转换器以"简单名"（注解name或方法名）
     * 注册到全局{@link ConversionManager}；若两个转换接口存在同名方法，后创建者会因同名冲突抛出
     * IllegalArgumentException而创建失败（该行为与接口的创建顺序相关）。</p>
     *
     * @param spELRuntime              SpEL运行时环境
     * @param conversionInterfaceClass 转换服务接口的Class
     * @param <T>                      转换器类型
     * @return 该转换器接口的代理对象
     */
    public static <T> T getConversionServiceProxy(SpELRuntime spELRuntime, @NonNull Class<T> conversionInterfaceClass) {
        Object conversionService = interfaceProxyCache.get(conversionInterfaceClass);
        if (conversionService == null) {
            ConversionInvocationHandler invocationHandler = new ConversionInvocationHandler(spELRuntime, conversionInterfaceClass);
            conversionService = ProxyFactory.getJdkProxyObject(conversionInterfaceClass.getClassLoader(), new Class[]{conversionInterfaceClass}, invocationHandler);
            interfaceProxyCache.put(conversionInterfaceClass, conversionService);
        }
        return (T) conversionService;
    }

    /**
     * 获取一个转换器接口的代理对象
     *
     * @param conversionInterfaceClass 转换服务接口的Class
     * @param <T>                      转换器类型
     * @return 该转换器接口的代理对象
     */
    public static <T> T getConversionServiceProxy(@NonNull Class<T> conversionInterfaceClass) {
        return getConversionServiceProxy(new SpELRuntime(), conversionInterfaceClass);
    }

    /**
     * 松散绑定：将数据源对象绑定为指定类型的对象（使用默认的{@link LooseBind}配置）
     *
     * @param beanType    松散绑定目标类型（支持泛型Token）
     * @param configValue 数据源对象
     * @param <T>         松散绑定对象类型
     * @return 松散绑定对象
     * @throws TypeConversionException 绑定失败时抛出（原始异常作为cause）
     */
    public static <T> T looseBind(SerializationTypeToken<T> beanType, Object configValue) {
        return looseBind(DEFAULT_LOOSE_BIND, beanType, configValue);
    }

    /**
     * 松散绑定：将数据源对象绑定为指定类型的对象（使用默认的{@link LooseBind}配置）
     *
     * @param beanType    松散绑定目标类型
     * @param configValue 数据源对象
     * @param <T>         松散绑定对象类型
     * @return 松散绑定对象
     * @throws TypeConversionException 绑定失败时抛出（原始异常作为cause）
     */
    public static <T> T looseBind(Class<T> beanType, Object configValue) {
        return looseBind(DEFAULT_LOOSE_BIND, beanType, configValue);
    }

    /**
     * 松散绑定：将数据源对象绑定为指定类型的对象（使用默认的{@link LooseBind}配置）
     *
     * @param beanType    松散绑定目标类型（支持泛型嵌套）
     * @param configValue 数据源对象
     * @param <T>         松散绑定对象类型
     * @return 松散绑定对象
     * @throws TypeConversionException 绑定失败时抛出（原始异常作为cause）
     */
    public static <T> T looseBind(Type beanType, Object configValue) {
        return looseBind(DEFAULT_LOOSE_BIND, beanType, configValue);
    }

    /**
     * 松散绑定：将数据源对象绑定为指定类型的对象（使用默认的{@link LooseBind}配置）
     *
     * @param beanType    松散绑定目标类型
     * @param configValue 数据源对象
     * @param <T>         松散绑定对象类型
     * @return 松散绑定对象
     * @throws TypeConversionException 绑定失败时抛出（原始异常作为cause）
     */
    public static <T> T looseBind(ResolvableType beanType, Object configValue) {
        return looseBind(DEFAULT_LOOSE_BIND, beanType, configValue);
    }

    /**
     * 松散绑定：将数据源对象绑定为指定类型的对象（使用指定的{@link LooseBind}配置）
     *
     * @param looseBind   松散绑定对象
     * @param beanType    松散绑定目标类型（支持泛型Token）
     * @param configValue 数据源对象
     * @param <T>         松散绑定对象类型
     * @return 松散绑定对象
     * @throws TypeConversionException 绑定失败时抛出（原始异常作为cause）
     */
    public static <T> T looseBind(LooseBind looseBind, SerializationTypeToken<T> beanType, Object configValue) {
        return looseBind(looseBind, beanType.getType(), configValue);
    }

    /**
     * 松散绑定：将数据源对象绑定为指定类型的对象（使用指定的{@link LooseBind}配置）
     *
     * @param looseBind   松散绑定对象
     * @param beanType    松散绑定目标类型
     * @param configValue 数据源对象
     * @param <T>         松散绑定对象类型
     * @return 松散绑定对象
     * @throws TypeConversionException 绑定失败时抛出（原始异常作为cause）
     */
    public static <T> T looseBind(LooseBind looseBind, Class<T> beanType, Object configValue) {
        return looseBind(looseBind, ResolvableType.forRawClass(beanType), configValue);
    }

    /**
     * 松散绑定：将数据源对象绑定为指定类型的对象（使用指定的{@link LooseBind}配置）
     *
     * @param looseBind   松散绑定对象
     * @param beanType    松散绑定目标类型（支持泛型嵌套）
     * @param configValue 数据源对象
     * @param <T>         松散绑定对象类型
     * @return 松散绑定对象
     * @throws TypeConversionException 绑定失败时抛出（原始异常作为cause）
     */
    public static <T> T looseBind(LooseBind looseBind, Type beanType, Object configValue) {
        return looseBind(looseBind, ResolvableType.forType(beanType), configValue);
    }

    /**
     * 松散绑定：将数据源对象绑定为指定类型的对象（使用指定的{@link LooseBind}配置）
     *
     * @param looseBind   松散绑定对象
     * @param beanType    松散绑定目标类型
     * @param configValue 数据源对象
     * @param <T>         松散绑定对象类型
     * @return 松散绑定对象
     * @throws TypeConversionException 绑定失败时抛出（原始异常作为cause）
     */
    public static <T> T looseBind(LooseBind looseBind, ResolvableType beanType, Object configValue) {
        try {
            return (T) looseBind.binding(beanType, configValue);
        } catch (Exception e) {
            throw new TypeConversionException("Loose binding failure.", e);
        }
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param conversionType 转换后的类型
     * @param conversionList 类型转换器集合
     * @param <T>            泛型类型
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, SerializationTypeToken<T> conversionType, List<ConversionService> conversionList) {
        return (T) conversion(toConvertValue, conversionType.getType(), conversionList);
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param conversionType 转换后的类型
     * @param conversions    类型转换器集合
     * @param <T>            泛型类型
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, SerializationTypeToken<T> conversionType, ConversionService... conversions) {
        return (T) conversion(toConvertValue, conversionType.getType(), Arrays.asList(conversions));
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param conversionType 转换后的类型（支持泛型Token）
     * @param <T>            泛型类型
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, SerializationTypeToken<T> conversionType) {
        return (T) conversion(toConvertValue, conversionType.getType());
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param conversionType 转换后的类型
     * @param conversionList 类型转换器集合
     * @param <T>            泛型类型
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, Type conversionType, List<ConversionService> conversionList) {
        return (T) conversion(toConvertValue, ResolvableType.forType(conversionType), conversionList);
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param conversionType 转换后的类型
     * @param conversions    类型转换器集合
     * @param <T>            泛型类型
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, Type conversionType, ConversionService... conversions) {
        return (T) conversion(toConvertValue, ResolvableType.forType(conversionType), Arrays.asList(conversions));
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param conversionType 转换后的类型（支持泛型嵌套）
     * @param <T>            泛型类型
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, Type conversionType) {
        return (T) conversion(toConvertValue, ResolvableType.forType(conversionType));
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param conversionType 转换后的类型
     * @param conversionList 类型转换器集合
     * @param <T>            泛型类型
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, Class<T> conversionType, List<ConversionService> conversionList) {
        return (T) conversion(toConvertValue, ResolvableType.forRawClass(conversionType), conversionList);
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param conversionType 转换后的类型
     * @param conversions    类型转换器集合
     * @param <T>            泛型类型
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, Class<T> conversionType, ConversionService... conversions) {
        return (T) conversion(toConvertValue, ResolvableType.forRawClass(conversionType), Arrays.asList(conversions));
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param conversionType 转换后的类型
     * @param <T>            泛型类型
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, Class<T> conversionType) {
        return (T) conversion(toConvertValue, ResolvableType.forRawClass(conversionType));
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 转换后的类型
     * @param conversionList 类型转换器集合
     * @return 转换后的对象
     */
    public static Object conversion(Object toConvertValue, ResolvableType resolvableType, List<ConversionService> conversionList) {
        return conversion(toConvertValue, resolvableType, conversionList, DEFAULT_FUNCTION);
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 转换后的类型
     * @param conversions    类型转换器集合
     * @return 转换后的对象
     */
    public static Object conversion(Object toConvertValue, ResolvableType resolvableType, ConversionService... conversions) {
        return conversion(toConvertValue, resolvableType, Arrays.asList(conversions), DEFAULT_FUNCTION);
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 转换后的类型
     * @return 转换后的对象
     */
    public static Object conversion(Object toConvertValue, ResolvableType resolvableType) {
        return conversion(toConvertValue, resolvableType, DEFAULT_FUNCTION);
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 转换后的类型
     * @param function       基本类型转换方法
     * @return 转换后的对象
     */
    public static Object conversion(Object toConvertValue, ResolvableType resolvableType, Function<Object, Object> function) {
        return conversion(toConvertValue, resolvableType, DEFAULT_CONVERSION_LIST, function);
    }

    /**
     * 将一个对象转换为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 转换后的类型
     * @param function       基本类型转换方法
     * @param conversions    类型转换器集合
     * @return 转换后的对象
     */
    public static Object conversion(Object toConvertValue, ResolvableType resolvableType, Function<Object, Object> function, ConversionService... conversions) {
        return conversion(toConvertValue, resolvableType, Arrays.asList(conversions), function);
    }

    /**
     * 核心转换逻辑：将一个对象转换为指定类型的对象，支持标量、容器、Map、POJO等全部类型体系及泛型嵌套
     * <p>分派顺序：Object早退 -> 兼容性早退 -> 集合 -> 数组 -> Map -> 基本类型 -> Class -> 枚举 -> POJO</p>
     * <p>兼容性早退：若目标类型与源类型兼容（含容器类型及其泛型），将直接返回源对象的引用（零拷贝），
     * 不做深复制，此时对返回值的修改会影响源对象。</p>
     *
     * @param toConvertValue 待转换的对象
     * @param returnType     转换后的目标类型
     * @param conversions    类型转换器集合
     * @param function       基本类型转换方法
     * @return 转换后的对象
     * @throws TypeConversionException 转换失败时抛出
     */
    public static Object conversion(Object toConvertValue, ResolvableType returnType, List<ConversionService> conversions, Function<Object, Object> function) {
        conversions = conversions == null ? DEFAULT_CONVERSION_LIST : conversions;
        function = function == null ? DEFAULT_FUNCTION : function;
        Object functionValue = function.apply(toConvertValue);

        if (functionValue == null) {
            return null;
        }
        Class<?> returnClass = returnType.toClass();

        // 转换的目标类为Object类型
        if (Object.class.equals(returnClass)) {
            return functionValue;
        }

        // 目标类型完全兼容待转换的类型
        if (ClassUtils.compatibleOrNot(returnType, ClassUtils.getResolvableType(functionValue))) {
            return functionValue;
        }

        // 转换的目标类为集合类型
        if (Collection.class.isAssignableFrom(returnClass)) {
            return conversionToCollection(functionValue, returnType, conversions, function);
        }

        // 转换的目标类为数组类型
        if (returnClass.isArray()) {
            return conversionToArray(functionValue, returnType, conversions, function);
        }

        // 转换的目标类为Map类型
        if (Map.class.isAssignableFrom(returnClass)) {
            return conversionToMap(functionValue, returnType, conversions, function);
        }

        // 转换的目标类为基本类型
        if (ClassUtils.isSimpleBaseType(returnClass)) {
            return conversionToBaseType(functionValue, returnClass);
        }

        // 转换的目标类为Class类型
        if (Class.class.isAssignableFrom(returnClass)) {
            return conversionToClass(functionValue);
        }

        // 转换目标是一个枚举类型
        if (returnClass.isEnum()) {
            return conversionToEnum(functionValue, returnClass);
        }

        return conversionToPojo(functionValue, returnType, conversions, function);
    }

    /**
     * 将对象转换为Spring的资源对象{@link Resource}
     *
     * @param toConvertValue 待转换的对象
     * @return Spring的资源对象
     */
    private static Resource conversionToResource(Object toConvertValue) {
        String resourceName = conversion(toConvertValue, String.class);
        return PATH_RESOURCE_SCANNER.getResource(resourceName);
    }

    /**
     * 将对象转换为Spring的资源数组对象{@link Resource}
     *
     * @param toConvertValue 待转换的对象
     * @return Spring的资源数组对象
     */
    private static Resource[] conversionToResources(Object toConvertValue) {
        if (ContainerUtils.isSpecificElementIterable(toConvertValue, Resource.class)) {
            return ContainerUtils.iterableToArray(ContainerUtils.getIterable(toConvertValue, Resource.class), Resource.class);
        }
        // 如果待转换对象是可迭代的，则遍历迭代器逐个进行转换
        if (ContainerUtils.isIterable(toConvertValue)) {
            Iterator<Object> iterator = ContainerUtils.getIterator(toConvertValue);
            List<Resource> resourceList = new ArrayList<>();
            while (iterator.hasNext()) {
                resourceList.add(conversion(iterator.next(), Resource.class));
            }
            return ContainerUtils.listToArray(resourceList, Resource.class);
        }
        // 其他类型则会先转换为String之后再转换为Resource[]
        else {
            return getResources(conversion(toConvertValue, String.class));
        }
    }

    /**
     * 将对象转换为{@link InputStream}
     *
     * @param toConvertValue 待转换的对象
     * @return InputStream
     */
    private static InputStream conversionToInputStream(Object toConvertValue) {
        return resourceToInputStream(conversionToResource(toConvertValue));
    }

    /**
     * 将对象转换为{@link InputStream}数组
     *
     * @param toConvertValue 待转换的对象
     * @return InputStream数组
     */
    private static InputStream[] conversionToInputStreamArray(Object toConvertValue) {
        if (ContainerUtils.isSpecificElementIterable(toConvertValue, InputStream.class)) {
            return ContainerUtils.iterableToArray(ContainerUtils.getIterable(toConvertValue, InputStream.class), InputStream.class);
        }
        List<InputStream> inputStreamList = Stream.of(conversionToResources(toConvertValue)).map(ConversionUtils::resourceToInputStream).collect(Collectors.toList());
        return ContainerUtils.listToArray(inputStreamList, InputStream.class);
    }

    /**
     * 将对象转换为{@link OutputStream}
     * <p>注意：该转换会立即打开（或创建）目标文件对应的输出流，若文件已存在，其内容会被直接清空（截断）。
     * 请仅在明确需要写入该文件时使用，并自行负责流的关闭。</p>
     *
     * @param toConvertValue 待转换的对象
     * @return OutputStream
     */
    private static OutputStream conversionToOutputStream(Object toConvertValue) {
        return resourceToOutputStream(conversionToResource(toConvertValue));
    }

    /**
     * 将对象转换为{@link OutputStream}数组
     *
     * @param toConvertValue 待转换的对象
     * @return OutputStream数组
     */
    private static OutputStream[] conversionToOutputStreamArray(Object toConvertValue) {
        if (ContainerUtils.isSpecificElementIterable(toConvertValue, OutputStream.class)) {
            return ContainerUtils.iterableToArray(ContainerUtils.getIterable(toConvertValue, OutputStream.class), OutputStream.class);
        }
        List<OutputStream> outputStreamList = Stream.of(conversionToResources(toConvertValue)).map(ConversionUtils::resourceToOutputStream).collect(Collectors.toList());
        return ContainerUtils.listToArray(outputStreamList, OutputStream.class);
    }

    /**
     * 将对象转换为{@link File}
     *
     * @param toConvertValue 待转换的对象
     * @return File
     */
    private static File conversionToFile(Object toConvertValue) {
        return resourceToFile(conversionToResource(toConvertValue));
    }

    /**
     * 将对象转换为{@link File[]}
     *
     * @param toConvertValue 待转换的对象
     * @return File数组
     */
    private static File[] conversionToFileArray(Object toConvertValue) {
        if (ContainerUtils.isSpecificElementIterable(toConvertValue, File.class)) {
            return ContainerUtils.iterableToArray(ContainerUtils.getIterable(toConvertValue, File.class), File.class);
        }
        List<File> fileList = Stream.of(conversionToResources(toConvertValue)).map(ConversionUtils::resourceToFile).collect(Collectors.toList());
        return ContainerUtils.listToArray(fileList, File.class);
    }

    /**
     * 将对象转换为{@link URL}
     *
     * @param toConvertValue 待转换的对象
     * @return URL
     */
    private static URL conversionToURL(Object toConvertValue) {
        return resourceToURL(conversionToResource(toConvertValue));
    }

    /**
     * 将对象转换为{@link URL}数组
     *
     * @param toConvertValue 待转换的对象
     * @return URL数组
     */
    private static URL[] conversionToURLArray(Object toConvertValue) {
        if (ContainerUtils.isSpecificElementIterable(toConvertValue, URL.class)) {
            return ContainerUtils.iterableToArray(ContainerUtils.getIterable(toConvertValue, URL.class), URL.class);
        }
        List<URL> urlList = Stream.of(conversionToResources(toConvertValue)).map(ConversionUtils::resourceToURL).collect(Collectors.toList());
        return ContainerUtils.listToArray(urlList, URL.class);
    }

    /**
     * 将对象转换为{@link URI}
     *
     * @param toConvertValue 待转换的对象
     * @return URI
     */
    private static URI conversionToURI(Object toConvertValue) {
        return resourceToURI(conversionToResource(toConvertValue));
    }

    /**
     * 将对象转换为{@link URI}数组
     *
     * @param toConvertValue 待转换的对象
     * @return URI数组
     */
    private static URI[] conversionToURIArray(Object toConvertValue) {
        if (ContainerUtils.isSpecificElementIterable(toConvertValue, URI.class)) {
            return ContainerUtils.iterableToArray(ContainerUtils.getIterable(toConvertValue, URI.class), URI.class);
        }
        List<URI> uriList = Stream.of(conversionToResources(toConvertValue)).map(ConversionUtils::resourceToURI).collect(Collectors.toList());
        return ContainerUtils.listToArray(uriList, URI.class);
    }

    /**
     * 将资源表达式解析为资源组
     * <p>注意：当资源表达式为通配符模式但没有任何匹配结果时，会回退为将表达式按字面路径解析为
     * 单个资源（保留该行为以兼容单文件/协议前缀场景），问题会推迟到真正读取资源时才会暴露。</p>
     *
     * @param locationPattern 资源表达式
     * @return 资源组
     */
    private static Resource[] getResources(String locationPattern) {
        try {
            Resource[] resources = PATH_RESOURCE_SCANNER.getResources(locationPattern);
            return ContainerUtils.isEmptyArray(resources) ? new Resource[]{PATH_RESOURCE_SCANNER.getResource(locationPattern)} : resources;
        } catch (IOException e) {
            throw new TypeConversionException("Unable to convert '" + locationPattern + "' to spring resource group!", e);
        }
    }

    /**
     * 将{@link Resource}对象转换为{@link File}
     *
     * @param resource 资源对象
     * @return File
     */
    private static File resourceToFile(Resource resource) {
        try {
            return resource.getFile();
        } catch (IOException e) {
            throw new TypeConversionException("Exception getting file info from resource['" + resource + "']", e);
        }
    }

    /**
     * 将{@link Resource}对象转换为{@link InputStream}
     *
     * @param resource 资源对象
     * @return InputStream
     */
    private static InputStream resourceToInputStream(Resource resource) {
        try {
            return resource.getInputStream();
        } catch (IOException e) {
            throw new TypeConversionException("Exception getting input stream info from resource['" + resource + "']", e);
        }
    }

    /**
     * 将{@link Resource}对象转换为{@link OutputStream}
     * <p>注意：若资源对应的文件已存在，其内容会被直接清空（截断）。</p>
     *
     * @param resource 资源对象
     * @return OutputStream
     */
    private static OutputStream resourceToOutputStream(Resource resource) {
        if (resource.isFile()) {
            try {
                return new FileOutputStream(resource.getFile());
            } catch (IOException e) {
                throw new TypeConversionException("Exception getting output stream from file resource '" + resource + "'", e);
            }
        } else {
            throw new TypeConversionException("'" + resource + "' is not a file resource and cannot be converted to an output stream.");
        }
    }

    /**
     * 将{@link Resource}对象转换为{@link URL}
     *
     * @param resource 资源对象
     * @return URL
     */
    private static URL resourceToURL(Resource resource) {
        try {
            return resource.getURL();
        } catch (IOException e) {
            throw new TypeConversionException("Exception getting URL info from resource['" + resource + "']", e);
        }
    }

    /**
     * 将{@link Resource}对象转换为{@link URI}
     *
     * @param resource 资源对象
     * @return URI
     */
    private static URI resourceToURI(Resource resource) {
        try {
            return resource.getURI();
        } catch (IOException e) {
            throw new TypeConversionException("Exception getting UTI info from resource['" + resource + "']", e);
        }
    }

    /**
     * 将传入的待转换的对象转换为Class对象
     * <p>使用当前线程的上下文类加载器按类名加载。</p>
     *
     * @param toConvertValue 待转换的对象（类全名）
     * @return Class对象
     */
    private static Class<?> conversionToClass(Object toConvertValue) {
        return ClassUtils.forName(String.valueOf(toConvertValue), Thread.currentThread().getContextClassLoader());
    }

    /**
     * 将传入的待转换的对象转换为枚举类型的值
     * <p>匹配规则（按顺序）：去除首尾空白后，先尝试与枚举常量名精确匹配，再尝试忽略大小写匹配；
     * 均无法匹配时抛出{@link TypeConversionException}并给出全部候选常量。</p>
     *
     * @param toConvertValue 待转换的对象
     * @param enumClass      目标枚举类型
     * @return 枚举常量
     */
    private static Object conversionToEnum(Object toConvertValue, Class<?> enumClass) {
        String enumName = conversion(toConvertValue, String.class).trim();
        Object[] enumConstants = enumClass.getEnumConstants();
        // 1.与枚举常量名精确匹配
        for (Object enumConstant : enumConstants) {
            if (((Enum<?>) enumConstant).name().equals(enumName)) {
                return enumConstant;
            }
        }
        // 2.忽略大小写匹配
        for (Object enumConstant : enumConstants) {
            if (((Enum<?>) enumConstant).name().equalsIgnoreCase(enumName)) {
                return enumConstant;
            }
        }
        throw new TypeConversionException("Cannot convert value ['" + enumName + "'] to enum type '" + enumClass.getName() + "', the candidates are: " + Arrays.toString(enumConstants));
    }

    /**
     * 将传入的待转换的对象转换为某个指定类型的对象
     * <p>处理流程：先尝试特殊类型转换器（Resource、File、InputStream、OutputStream、URL、URI）；
     * Map源执行Map -> POJO的字段填充；其他对象优先尝试已注册的{@link ConversionService}，
     * 未命中时先展开为Map再进行填充。</p>
     * <p>Map -> POJO 的字段处理规则：</p>
     * <p>1. 跳过static与final字段；</p>
     * <p>2. transient字段会照常参与转换（如需排除请在实体设计上处理）；</p>
     * <p>3. 仅按字段映射名（@TargetField或字段名）与Map的key精确匹配，不做大小写/分隔符的松散匹配
     * （松散匹配场景请使用{@link LooseBind}体系）；</p>
     * <p>4. null值写入primitive字段时抛出{@link TypeConversionException}；</p>
     * <p>5. 目标类型必须提供无参构造器，Map中不存在的字段保持默认值。</p>
     *
     * @param toConvertValue 待转换的对象
     * @param returnType     转换后的目标类型
     * @param conversions    类型转换器集合
     * @param function       基本类型转换方法
     * @return 目标实体
     * @throws TypeConversionException 目标类型无法实例化或字段赋值失败时抛出
     */
    private static Object conversionToPojo(Object toConvertValue, ResolvableType returnType, List<ConversionService> conversions, Function<Object, Object> function) {
        Class<?> returnClass = returnType.toClass();

        // 特殊类型（Resource、File、InputStream、OutputStream、URL、URI）
        Function<Object, Object> specialTypeConverter = getSpecialTypeConverter(returnClass);
        if (specialTypeConverter != null) {
            return specialTypeConverter.apply(toConvertValue);
        }

        if (toConvertValue instanceof Map) {
            Map<String, Object> valueMap = (Map<String, Object>) toConvertValue;
            Field[] fields = ClassUtils.getAllFields(returnClass);
            Object resultPojo;
            try {
                resultPojo = ClassUtils.newObject(returnClass);
            } catch (Exception e) {
                throw new TypeConversionException("Cannot instantiate the target type '" + returnClass.getName() + "' for Map to POJO conversion: a no-argument constructor is required!", e);
            }
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                String mappingName = getMappingName(field);
                if (valueMap.containsKey(mappingName)) {
                    Object fieldValue = conversion(valueMap.get(mappingName), ResolvableType.forField(field, returnType), conversions, function);
                    // null值不允许写入primitive字段，此处给出信息明确的异常（直接反射赋值会抛出难以理解的IllegalArgumentException）
                    if (fieldValue == null && field.getType().isPrimitive()) {
                        throw new TypeConversionException("Cannot set the null value to the primitive field '" + field.getName() + "' of class '" + returnClass.getName() + "', target field type: " + field.getType());
                    }
                    try {
                        FieldUtils.setValue(resultPojo, field, fieldValue);
                    } catch (Exception e) {
                        throw new TypeConversionException("Failed to set the value of field '" + field.getName() + "' of class '" + returnClass.getName() + "', target field type: " + field.getType() + ", value = '" + fieldValue + "'", e);
                    }
                } else {
                    log.debug("Field '{}' with mapping name '{}' of type '{}' was not found in the source map, it will keep the default value.", field.getName(), mappingName, returnClass.getName());
                }
            }
            return resultPojo;
        } else {
            try {
                for (ConversionService conversion : conversions) {
                    if (conversion == null) {
                        continue;
                    }
                    if (conversion.canConvert(returnType, ResolvableType.forInstance(toConvertValue))) {
                        conversion.addUseConversions(conversions);
                        return conversion.conversion(toConvertValue);
                    }
                }
                return conversionToPojo(pojoToMap(null, toConvertValue), returnType, conversions, function);
            } catch (TypeConversionException e) {
                throw new TypeConversionException("Type conversion failed, type incompatible! value = '(" + toConvertValue.getClass() + ") " + toConvertValue + "' , conversion-type = '" + returnType.toString() + "'", e);
            }
        }
    }

    /**
     * 将传入的待转换的对象转换为Map类型的对象
     * <p>规则：ConfigurationMap源在目标为Properties和ConfigurationMap时直接转换/返回；
     * 普通Map源按目标泛型对key/value做递归转换，目标Map类型通过无参构造创建
     * （接口、抽象类或无可用构造器时回退为LinkedHashMap）；其他对象先展开为Map再转换。</p>
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 转换后的目标类型
     * @param conversions    类型转换器集合
     * @param function       基本类型转换方法
     * @return Map对象
     * @throws TypeConversionException 转换失败时抛出
     */
    private static Map<?, ?> conversionToMap(Object toConvertValue, ResolvableType resolvableType, List<ConversionService> conversions, Function<Object, Object> function) {
        Class<?> targetClass = resolvableType.toClass();
        if (toConvertValue instanceof ConfigurationMap) {
            if (Properties.class.isAssignableFrom(targetClass)) {
                return ((ConfigurationMap) toConvertValue).toProperties(true);
            }
            if (ConfigurationMap.class.isAssignableFrom(targetClass)) {
                return (ConfigurationMap) toConvertValue;
            }
            toConvertValue = ((ConfigurationMap) toConvertValue).getDataMap();
        }
        if (toConvertValue instanceof Map) {
            Map<Object, Object> valueMap = (Map<Object, Object>) toConvertValue;
            if (ConfigurationMap.class.isAssignableFrom(targetClass)) {
                ConfigurationMap cmap = new ConfigurationMap();
                cmap.addProperties(valueMap);
                return cmap;
            }

            Map<Object, Object> resultMap = (Map<Object, Object>) ClassUtils.createObject(targetClass, () -> new LinkedHashMap<>(valueMap.size()));
            for (Map.Entry<Object, Object> entry : valueMap.entrySet()) {
                resultMap.put(conversion(entry.getKey(), resolvableType.getGeneric(0), conversions, function),
                        conversion(entry.getValue(), resolvableType.getGeneric(1), conversions, function));
            }
            return resultMap;
        } else {
            try {
                return conversionToMap(pojoToMap(null, toConvertValue), resolvableType, conversions, function);
            } catch (TypeConversionException e) {
                throw new TypeConversionException("Type conversion failed, type incompatible! value = '(" + toConvertValue.getClass() + ") " + toConvertValue + "' , conversion-type = '" + resolvableType.toString() + "'", e);
            }

        }
    }

    /**
     * 将Pojo类型转换为Map类型
     * <p>字段处理规则：跳过static字段；final与transient字段会照常参与转换；
     * 简单类型/枚举字段直接取值，其他类型字段递归展开；遇到循环引用时以null代替。</p>
     *
     * @param mapKey 外部key
     * @param pojo   Pojo实体
     * @return Map类型结果
     */
    private static Map<?, ?> pojoToMap(String mapKey, Object pojo) {
        return pojoToMap(mapKey, pojo, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /**
     * 将Pojo类型转换为Map类型
     *
     * @param mapKey  外部key
     * @param pojo    Pojo实体
     * @param visited 当前递归路径上已经展开过的对象，用于循环引用检测
     * @return Map类型结果
     */
    private static Map<?, ?> pojoToMap(String mapKey, Object pojo, Set<Object> visited) {
        // 循环引用检测：跟踪当前递归路径上的对象，遇到环时以null代替，避免无限递归
        boolean needTrack = false;
        if (pojo != null) {
            Class<?> pojoClass = pojo.getClass();
            needTrack = !ClassUtils.isSimpleBaseType(pojoClass) && !pojoClass.isEnum();
        }
        if (needTrack && !visited.add(pojo)) {
            if (mapKey == null) {
                return null;
            }
            Map<Object, Object> cycleMap = new LinkedHashMap<>(1);
            cycleMap.put(mapKey, null);
            return cycleMap;
        }
        try {
            // 未提供外部key，执行转换逻辑
            if (mapKey == null) {
                if (pojo == null) {
                    return null;
                }
                Class<?> toConvertValueClass = pojo.getClass();
                if (ClassUtils.isSimpleBaseType(toConvertValueClass)
                        || Collection.class.isAssignableFrom(toConvertValueClass)
                        || toConvertValueClass.isArray()) {
                    throw new TypeConversionException("Type '" + pojo.getClass() + "' cannot be expanded as a property source for object conversion!");
                }

                if (Map.class.isAssignableFrom(toConvertValueClass)) {
                    return (Map<Object, Object>) pojo;
                }
                Map<Object, Object> resultMap = new LinkedHashMap<>();
                Field[] allFields = ClassUtils.getAllFields(toConvertValueClass);
                for (Field field : allFields) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    String mappingName = getMappingName(field);
                    if (ClassUtils.isSimpleBaseType(field.getType()) || field.getType().isEnum()) {
                        resultMap.put(mappingName, FieldUtils.getValue(pojo, field));
                    } else {
                        resultMap.put(mappingName, pojoToMap(mappingName, FieldUtils.getValue(pojo, field), visited).get(mappingName));
                    }
                }
                return resultMap;
            }
            // 提供外部key，则需要构建外层Map，并将转换结果put到外层Map
            else {
                Map<Object, Object> resultMap = new LinkedHashMap<>();
                if (pojo == null) {
                    resultMap.put(mapKey, null);
                    return resultMap;
                }

                ResolvableType toConvertValueType = ResolvableType.forInstance(pojo);
                Class<?> toConvertValueClass = toConvertValueType.toClass();
                if (ClassUtils.isSimpleBaseType(toConvertValueClass)) {
                    throw new TypeConversionException("Type '" + pojo.getClass() + "' cannot be expanded as a property source for object conversion!");
                }
                if (Map.class.isAssignableFrom(toConvertValueClass)) {
                    Map<Object, Object> map = (Map<Object, Object>) pojo;
                    resultMap.put(mapKey, map);
                    return resultMap;
                }
                if (toConvertValueClass.isArray()) {
                    Class<?> componentType = toConvertValueClass.getComponentType();
                    if (ClassUtils.isSimpleBaseType(componentType)) {
                        resultMap.put(mapKey, pojo);
                    } else {
                        int length = Array.getLength(pojo);
                        Object[] arrayObject = new Object[length];
                        for (int i = 0; i < length; i++) {
                            arrayObject[i] = pojoToMap(null, Array.get(pojo, i), visited);
                        }
                        resultMap.put(mapKey, arrayObject);
                    }
                    return resultMap;

                }
                if (Collection.class.isAssignableFrom(toConvertValueClass)) {
                    Collection collectionValue = (Collection) pojo;
                    if (((Collection<?>) pojo).isEmpty()) {
                        resultMap.put(mapKey, collectionValue);
                    } else {
                        Class<?> collectionEntryClass = null;
                        for (Object collectionObject : collectionValue) {
                            if (collectionObject != null) {
                                collectionEntryClass = collectionObject.getClass();
                                break;
                            }
                        }
                        // 集合中的所有元素均为null或者元素为基本类型
                        if (collectionEntryClass == null || ClassUtils.isSimpleBaseType(collectionEntryClass)) {
                            resultMap.put(mapKey, collectionValue);
                        } else {
                            if (Set.class.isAssignableFrom(toConvertValueClass)) {
                                Set<Object> setValue = new HashSet(collectionValue.size());
                                for (Object collectionEntry : collectionValue) {
                                    setValue.add(pojoToMap(null, collectionEntry, visited));
                                }
                                resultMap.put(mapKey, setValue);
                            } else {
                                // List以及其他集合类型（如Queue、Deque等）统一按List形式展开，避免静默丢失数据
                                List<Object> listValue = new ArrayList(collectionValue.size());
                                for (Object collectionEntry : collectionValue) {
                                    listValue.add(pojoToMap(null, collectionEntry, visited));
                                }
                                resultMap.put(mapKey, listValue);
                            }
                        }
                    }
                    return resultMap;
                }
                Map<Object, Object> map = new LinkedHashMap<>();
                Field[] allFields = ClassUtils.getAllFields(toConvertValueClass);
                for (Field field : allFields) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    String mappingName = getMappingName(field);
                    if (ClassUtils.isSimpleBaseType(field.getType()) || field.getType().isEnum()) {
                        map.put(mappingName, FieldUtils.getValue(pojo, field));
                    } else {
                        map.put(mappingName, pojoToMap(mappingName, FieldUtils.getValue(pojo, field), visited).get(mappingName));
                    }
                }
                resultMap.put(mapKey, map);
                return resultMap;

            }
        } finally {
            if (needTrack) {
                visited.remove(pojo);
            }
        }
    }


    /**
     * 将Iterator物化为List，让一次性数据源走统一的迭代转换流程
     *
     * @param iterator 迭代器
     * @return 物化后的List
     */
    private static List<Object> materializeIterator(Iterator<?> iterator) {
        List<Object> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    /**
     * 将传入的待转换的对象转换为数组类型的对象
     * <p>特殊规则：Iterator数据源先物化为List；String源转换byte[]/Byte[]时按Base64解码，
     * 转换char[]/Character[]时逐字符展开；源类型与目标元素类型兼容时直接封装为单元素数组；
     * 集合/数组/其他对象统一按元素递归转换。</p>
     *
     * @param toConvertValue 待转换的对象
     * @param returnType     转换后的目标类型
     * @param conversions    类型转换器集合
     * @param function       基本类型转换方法
     * @return 数组对象
     * @throws TypeConversionException 元素转换失败或null写入primitive元素数组时抛出
     */
    private static Object conversionToArray(Object toConvertValue, ResolvableType returnType, List<ConversionService> conversions, Function<Object, Object> function) {

        // Iterator类型的数据源统一物化为集合后处理
        if (toConvertValue instanceof Iterator) {
            toConvertValue = materializeIterator((Iterator<?>) toConvertValue);
        }

        // 处理String -> byte[]/Byte[]的场景，会尝试使用Base64进行解码
        Class<?> returnClass = returnType.toClass();
        if (toConvertValue instanceof String) {
            if (returnClass == byte[].class) {
                return Base64Utils.decodeFromString((String) toConvertValue);
            }
            if (returnClass == Byte[].class) {
                byte[] bytes = Base64Utils.decodeFromString((String) toConvertValue);
                Byte[] byteArray = new Byte[bytes.length];
                for (int i = 0; i < bytes.length; i++) {
                    byteArray[i] = bytes[i];
                }
                return byteArray;
            }
            if (returnClass == char[].class) {
                return ((String) toConvertValue).toCharArray();
            }
            if (returnClass == Character[].class) {
                char[] chars = ((String) toConvertValue).toCharArray();
                Character[] characterArray = new Character[chars.length];
                for (int i = 0; i < chars.length; i++) {
                    characterArray[i] = chars[i];
                }
                return characterArray;
            }
        }

        ResolvableType resultArrayEntryType = returnType.getComponentType();
        Class<?> resultArrayEntryClass = resultArrayEntryType.toClass();

        // 待转换的对象类型继承自目标类型的元素的类型
        if (resultArrayEntryClass.isAssignableFrom(toConvertValue.getClass())) {
            Object returnArray = Array.newInstance(resultArrayEntryClass, 1);
            Array.set(returnArray, 0, toConvertValue);
            return returnArray;
        }

        // 处理特殊的数组元素类型（Resource、File、InputStream、OutputStream、URL、URI）
        Function<Object, Object> specialTypeArrayConverter = getSpecialTypeArrayConverter(resultArrayEntryClass);
        Object[] specialTypeArray = specialTypeArrayConverter == null ? null : (Object[]) specialTypeArrayConverter.apply(toConvertValue);

        if (specialTypeArray != null) {
            return ContainerUtils.arrayDowncast(specialTypeArray, resultArrayEntryClass);
        }

        // 集合
        if (toConvertValue instanceof Collection) {
            Collection<?> collectionValue = (Collection<?>) toConvertValue;
            Object arrayObject = Array.newInstance(resultArrayEntryClass, collectionValue.size());
            int i = 0;
            for (Object collectionEntry : collectionValue) {
                setArrayElement(arrayObject, i++, conversion(collectionEntry, resultArrayEntryType, conversions, function), resultArrayEntryClass);
            }
            return arrayObject;
        }

        // 数组
        else if (toConvertValue.getClass().isArray()) {
            int length = Array.getLength(toConvertValue);
            Object arrayObject = Array.newInstance(resultArrayEntryClass, length);
            for (int i = 0; i < length; i++) {
                setArrayElement(arrayObject, i, conversion(Array.get(toConvertValue, i), resultArrayEntryType, conversions, function), resultArrayEntryClass);
            }
            return arrayObject;
        }

        // 其他
        else {
            Object arrayObject = Array.newInstance(resultArrayEntryClass, 1);
            setArrayElement(arrayObject, 0, conversion(toConvertValue, resultArrayEntryType, conversions, function), resultArrayEntryClass);
            return arrayObject;
        }
    }

    /**
     * 为数组元素赋值
     * <p>null值写入primitive元素类型的数组（如int[]）或类型不匹配时，抛出信息明确的{@link TypeConversionException}。</p>
     *
     * @param array        目标数组
     * @param index        元素下标
     * @param elementValue 元素值
     * @param elementType  数组元素类型
     */
    private static void setArrayElement(Object array, int index, Object elementValue, Class<?> elementType) {
        if (elementValue == null && elementType.isPrimitive()) {
            throw new TypeConversionException("Cannot set the null value to the primitive array element, array type: '" + elementType.getName() + "[]', index: " + index);
        }
        try {
            Array.set(array, index, elementValue);
        } catch (Exception e) {
            throw new TypeConversionException("Failed to set the value of the array ['" + elementType.getName() + "[]'] at index " + index + ", value = '" + elementValue + "'", e);
        }
    }

    /**
     * 将传入的待转换的对象转换为Collection类型的对象
     * <p>规则：Iterator数据源先物化为List；目标可赋值为List时按顺序收集（默认ArrayList），
     * 否则收集为Set（默认HashSet）；元素类型命中特殊类型数组转换器（Resource、File、
     * InputStream、OutputStream、URL、URI）时优先走特殊转换。</p>
     *
     * @param toConvertValue 待转换的对象
     * @param returnType     转换后的目标类型
     * @param conversions    类型转换器集合
     * @param function       基本类型转换方法
     * @return Collection对象
     * @throws TypeConversionException 元素转换失败时抛出
     */
    private static Collection<?> conversionToCollection(Object toConvertValue, ResolvableType returnType, List<ConversionService> conversions, Function<Object, Object> function) {

        // Iterator类型的数据源统一物化为集合后处理
        if (toConvertValue instanceof Iterator) {
            toConvertValue = materializeIterator((Iterator<?>) toConvertValue);
        }

        Class<?> returnClass = returnType.toClass();
        boolean resultTypeIsList = List.class.isAssignableFrom(returnClass);

        // 处理特殊的集合类型
        if (returnType.hasGenerics() && returnType.resolveGeneric(0) != null) {
            Class<?> returnGenericClass = returnType.resolveGeneric(0);
            Function<Object, Object> specialTypeArrayConverter = getSpecialTypeArrayConverter(returnGenericClass);
            Object[] specialTypeArray = specialTypeArrayConverter == null ? null : (Object[]) specialTypeArrayConverter.apply(toConvertValue);
            if (specialTypeArray != null) {
                if (resultTypeIsList) {
                    List<Object> list = (List<Object>) ClassUtils.createObject(returnClass, () -> new ArrayList<>());
                    ContainerUtils.copyToList(ContainerUtils.arrayDowncast(specialTypeArray, returnGenericClass), list);
                    return list;
                }
                Set<Object> set = (Set<Object>) ClassUtils.createObject(returnClass, () -> new HashSet<>());
                ContainerUtils.copyToSet(ContainerUtils.arrayDowncast(specialTypeArray, returnGenericClass), set);
                return set;
            }
        }

        // List
        if (resultTypeIsList) {
            List<Object> list;

            // value是集合
            if (toConvertValue instanceof Collection) {
                Collection<?> collectionValue = (Collection<?>) toConvertValue;
                list = (List<Object>) ClassUtils.createObject(returnClass, () -> new ArrayList<>(collectionValue.size()));
                for (Object collectionEntry : collectionValue) {
                    list.add(conversion(collectionEntry, returnType.getGeneric(0), conversions, function));
                }
                return list;
            }
            // value是数组
            else if (toConvertValue.getClass().isArray()) {
                int length = Array.getLength(toConvertValue);
                if (length == 0) {
                    return (Collection<?>) ClassUtils.createObject(returnClass, () -> new ArrayList<>(0));
                }

                ResolvableType arrayEntryType = returnType.hasGenerics() ? returnType.getGeneric(0) : ResolvableType.forInstance(toConvertValue).getComponentType();
                list = (List<Object>) ClassUtils.createObject(returnClass, () -> new ArrayList<>(length));
                for (int i = 0; i < length; i++) {
                    list.add(conversion(Array.get(toConvertValue, i), arrayEntryType, conversions, function));
                }
                return list;
            }
            // 其他类型
            else {
                list = (List<Object>) ClassUtils.createObject(returnClass, () -> new ArrayList<>(1));
                ResolvableType arrayEntryType = returnType.hasGenerics() ? returnType.getGeneric(0) : ResolvableType.forInstance(toConvertValue).getComponentType();
                list.add(conversion(toConvertValue, arrayEntryType, conversions, function));
                return list;
            }
        }
        // Set
        else {
            Set<Object> set;

            // value是集合
            if (toConvertValue instanceof Collection) {
                Collection<?> collectionValue = (Collection<?>) toConvertValue;
                set = (Set<Object>) ClassUtils.createObject(returnClass, () -> new HashSet<>(collectionValue.size()));
                for (Object collectionEntry : collectionValue) {
                    set.add(conversion(collectionEntry, returnType.getGeneric(0), conversions, function));
                }
                return set;
            }
            // value是数组
            else if (toConvertValue.getClass().isArray()) {
                int length = Array.getLength(toConvertValue);
                if (length == 0) {
                    return (Collection<?>) ClassUtils.createObject(returnClass, () -> new HashSet<>(0));
                }

                ResolvableType arrayEntryType = returnType.hasGenerics() ? returnType.getGeneric(0) : ResolvableType.forInstance(toConvertValue).getComponentType();
                set = (Set<Object>) ClassUtils.createObject(returnClass, () -> new HashSet<>(length));
                for (int i = 0; i < length; i++) {
                    set.add(conversion(Array.get(toConvertValue, i), arrayEntryType, conversions, function));
                }
                return set;
            }
            // 其他类型
            else {
                set = (Set<Object>) ClassUtils.createObject(returnClass, () -> new HashSet<>(1));
                ResolvableType arrayEntryType = returnType.hasGenerics() ? returnType.getGeneric(0) : ResolvableType.forInstance(toConvertValue).getComponentType();
                set.add(conversion(toConvertValue, arrayEntryType, conversions, function));
                return set;
            }
        }
    }

    /**
     * 将传入的待转换的对象转换为基本类型的值
     * <p>注意：当待转换对象为集合/数组时，只取迭代器的首元素参与转换（其余元素被忽略），
     * 空集合会按字面量字符串"null"参与转换（对String目标得到"null"，对数字目标会转换失败）；
     * 当目标为String且源为byte[]/Byte[]时，采用Base64编码语义。</p>
     *
     * @param toConvertValue 待转换的对象
     * @param baseType       基本类型
     * @param <T>            基本类型泛型
     * @return 基本类型值
     */
    private static <T> T conversionToBaseType(Object toConvertValue, Class<T> baseType) {

        if (baseType == String.class) {
            // byte[]/Byte[] -> String 采用Base64编码语义（反向转换的解码见conversionToArray）
            if (toConvertValue instanceof byte[]) {
                return (T) Base64Utils.encodeToString((byte[]) toConvertValue);
            }
            if (toConvertValue instanceof Byte[]) {
                Byte[] byteArray = (Byte[]) toConvertValue;
                byte[] bytes = new byte[byteArray.length];
                for (int i = 0; i < byteArray.length; i++) {
                    bytes[i] = byteArray[i];
                }
                return (T) Base64Utils.encodeToString(bytes);
            }
        }

        String stringValue;
        if (ContainerUtils.isIterable(toConvertValue)) {
            Iterator<Object> iterator = ContainerUtils.getIterator(toConvertValue);
            stringValue = String.valueOf(ContainerUtils.getIteratorFirst(iterator));
        } else {
            stringValue = String.valueOf(toConvertValue);
        }
        try {
            return JavaConversion.fromString(stringValue, baseType, false);
        } catch (Exception e) {
            throw new TypeConversionException("Cannot convert the value of " + toConvertValue.getClass() + " type ['" + toConvertValue + "'] to target type " + baseType + " !", e);
        }
    }

    /**
     * 获取属性映射名
     *
     * @param field 属性
     * @return 属性的映射名
     */
    private static String getMappingName(Field field) {
        TargetField targetFieldAnnotation = AnnotatedElementUtils.findMergedAnnotation(field, TargetField.class);
        return (targetFieldAnnotation == null || !StringUtils.hasText(targetFieldAnnotation.value()))
                ? field.getName()
                : targetFieldAnnotation.value();
    }

}
