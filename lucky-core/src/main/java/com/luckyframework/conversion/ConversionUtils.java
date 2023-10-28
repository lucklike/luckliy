package com.luckyframework.conversion;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.proxy.ProxyFactory;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import com.luckyframework.spel.SpELRuntime;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.NonNull;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 万能的数据转化工具，将一个对象转化为另一种指定类型的对象,支持以下类型的转换以及泛型嵌套转化<br/>
 * 数组 -> 数组<br/>
 * 数组 -> 集合<br/>
 * 集合 -> 数组<br/>
 * 集合 -> 集合<br/>
 * Map -> 实体<br/>
 * 实体 -> Map<br/>
 * T -> R<br/>
 * Map&lt;A,B&gt; -> Map&lt;T,R&gt;<br/>
 * Collection&lt;T&gt; -> Collection&lt;R&gt;<br/>
 * T[] -> R[]<br/>
 * String -> 基本类型<br/>
 * String -> 枚举类型<br/>
 * String -> File
 * String -> FileInputStream<br/>
 * String -> FileOutputStream<br/>
 * String -> URL<br/>
 * String -> URI<br/>
 * String -> Resource<br/>
 * String[]、Collection&lt;String&gt; -> 基本类型[]、Collection&lt;基本类型&gt;<br/>
 * String[]、Collection&lt;String&gt; -> 枚举[]、Collection&lt;枚举&gt;<br/>
 * String[]、Collection&lt;String&gt; -> File[]<br/>
 * String[]、Collection&lt;String&gt; -> FileInputStream[]<br/>
 * String[]、Collection&lt;String&gt; -> FileOutputStream[]<br/>
 * String[]、Collection&lt;String&gt; -> URL[]<br/>
 * String[]、Collection&lt;String&gt; -> URI[]<br/>
 * String[]、Collection&lt;String&gt; -> Resource[]<br/>
 * ......
 * ......
 */
@SuppressWarnings("all")
public abstract class ConversionUtils {

    /**
     * 默认的前置转化器
     */
    private static final Function<Object, Object> DEFAULT_FUNCTION = s -> s == ConfigurationMap.NULL_ENTRY ? null : s;
    private static final List<ConversionService> DEFAULT_CONVERSION_LIST = new ArrayList<>();
    /**
     * Spring的资源解析对象
     */
    private static final PathMatchingResourcePatternResolver PATH_RESOURCE_SCANNER = new PathMatchingResourcePatternResolver();

    private static final Map<Class<?>, Object> interfaceProxyCache = new ConcurrentHashMap<>(64);


    /**
     * 获取一个转化器接口的代理对象
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
     * 获取一个转化器接口的代理对象
     *
     * @param conversionInterfaceClass 转换服务接口的Class
     * @param <T>                      转换器类型
     * @return 该转换器接口的代理对象
     */
    public static <T> T getConversionServiceProxy(@NonNull Class<T> conversionInterfaceClass) {
        return getConversionServiceProxy(new SpELRuntime(), conversionInterfaceClass);
    }

    /**
     * 将一个对象转化为指定类型的对象
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
     * 将一个对象转化为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param conversionType 转换后的类型
     * @param conversions    类型转换器集合
     * @param <T>            泛型类型
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, SerializationTypeToken<T> conversionType, ConversionService... conversions) {
        return (T) conversion(toConvertValue, conversionType.getType(), Stream.of(conversions).collect(Collectors.toList()));
    }

    /**
     * 将一个对象转化为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 转换后的类型
     * @param <T>
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, SerializationTypeToken<T> conversionType) {
        return (T) conversion(toConvertValue, conversionType.getType());
    }

    /**
     * 将一个对象转化为指定类型的对象
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
     * 将一个对象转化为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param conversionType 转换后的类型
     * @param conversions    类型转换器集合
     * @param <T>            泛型类型
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, Type conversionType, ConversionService... conversions) {
        return (T) conversion(toConvertValue, ResolvableType.forType(conversionType), Stream.of(conversions).collect(Collectors.toList()));
    }

    /**
     * 将一个对象转化为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 转换后的类型
     * @param <T>
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, Type conversionType) {
        return (T) conversion(toConvertValue, ResolvableType.forType(conversionType));
    }

    /**
     * 将一个对象转化为指定类型的对象
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
     * 将一个对象转化为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param conversionType 转换后的类型
     * @param conversions    类型转换器集合
     * @param <T>            泛型类型
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, Class<T> conversionType, ConversionService... conversions) {
        return (T) conversion(toConvertValue, ResolvableType.forRawClass(conversionType), Stream.of(conversions).collect(Collectors.toList()));
    }

    /**
     * 将一个对象转化为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 转换后的类型
     * @param <T>
     * @return 转换后的对象
     */
    public static <T> T conversion(Object toConvertValue, Class<T> conversionType) {
        return (T) conversion(toConvertValue, ResolvableType.forRawClass(conversionType));
    }

    /**
     * 将一个对象转化为指定类型的对象
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
     * 将一个对象转化为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 转换后的类型
     * @param conversions    类型转换器集合
     * @return 转换后的对象
     */
    public static Object conversion(Object toConvertValue, ResolvableType resolvableType, ConversionService... conversions) {
        return conversion(toConvertValue, resolvableType, Stream.of(conversions).collect(Collectors.toList()), DEFAULT_FUNCTION);
    }

    /**
     * 将一个对象转化为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 转换后的可解析类型
     * @return 转换后的对象
     */
    public static Object conversion(Object toConvertValue, ResolvableType resolvableType) {
        return conversion(toConvertValue, resolvableType, DEFAULT_FUNCTION);
    }

    /**
     * 将一个对象转化为指定类型的对象
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
     * 将一个对象转化为指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 转换后的类型
     * @param function       基本类型转换方法
     * @param conversions    类型转换器集合
     * @return
     */
    public static Object conversion(Object toConvertValue, ResolvableType resolvableType, Function<Object, Object> function, ConversionService... conversions) {
        return conversion(toConvertValue, resolvableType, Stream.of(conversions).collect(Collectors.toList()), function);
    }

    /**
     * 将一个对象转化为另一种指定类型的对象,支持以下类型的转换以及泛型嵌套
     *
     * @param toConvertValue 待转换的对象
     * @param returnType     转换后的类型
     * @param conversions    类型转换器集合
     * @param function       基本类型转换方法
     * @return 转换后的对象
     */
    public static Object conversion(Object toConvertValue, ResolvableType returnType, List<ConversionService> conversions, Function<Object, Object> function) {
        Object functionValue = function.apply(toConvertValue);

        if (functionValue == null) {
            return null;
        }
        Class<?> returnClass = returnType.getRawClass();

        // rawClass为null表示resolvableType为？，此时可以直接返回toConvertValue
        if (returnClass == null) {
            return functionValue;
        }

        // 转化的目标类为集合类型
        if (Collection.class.isAssignableFrom(returnClass)) {
            return conversionToCollection(functionValue, returnType, conversions, function);
        }

        // 转化的目标类为数组类型
        if (returnClass.isArray()) {
            return conversionToArray(functionValue, returnType, conversions, function);
        }

        // 转化的目标类为Map类型
        if (Map.class.isAssignableFrom(returnClass)) {
            return conversionToMap(functionValue, returnType, conversions, function);
        }


        // 泛型toString()相同
        if (returnType.toString().equals(ResolvableType.forClass(toConvertValue.getClass()).toString())) {
            return functionValue;
        }

        // 转化的目标类为基本类型
        if (ClassUtils.isSimpleBaseType(returnClass)) {
            return conversionToBaseType(functionValue, returnClass);
        }

        // 转化的目标类为Class类型
        if (Class.class.isAssignableFrom(returnClass)) {
            return conversionToClass(functionValue);
        }

        // 转化目标是一个枚举类型
        if (returnClass.isEnum()) {
            Class<? extends Enum> enumClass = (Class<? extends Enum>) returnClass;
            return Enum.valueOf(enumClass, conversion(functionValue, String.class).toUpperCase());
        }

        // 转化的目标类为Object类型
        if (Object.class.equals(returnClass)) {
            return functionValue;
        }
        return conversionToPojo(functionValue, returnType, conversions, function);
    }

    /**
     * 将对象转化为Spring的资源对象{@link Resource}
     *
     * @param toConvertValue 待转化的对象
     * @return Spring的资源对象
     */
    private static Resource conversionToResource(Object toConvertValue) {
        String resourceName = conversion(toConvertValue, String.class);
        return PATH_RESOURCE_SCANNER.getResource(resourceName);
    }

    /**
     * 将对象转化为Spring的资源数组对象{@link Resource}
     *
     * @param toConvertValue 待转化的对象
     * @return Spring的资源数组对象
     */
    private static Resource[] conversionToResources(Object toConvertValue) {
        // 如果待转换对象是可迭代的，则遍历迭代器逐个的进行转换
        if (ContainerUtils.isIterable(toConvertValue)) {
            Iterator<Object> iterator = ContainerUtils.getIterator(toConvertValue);
            List<Resource> resourceList = new ArrayList<>();
            while (iterator.hasNext()) {
                resourceList.add(conversion(iterator.next(), Resource.class));
            }
            return ContainerUtils.listToArray(resourceList, Resource.class);
        }
        // 其他类型则会先转化为String之后再转化为Resource[]
        else {
            return getResources(conversion(toConvertValue, String.class));
        }
    }

    /**
     * 将对象转化为{@link InputStream}
     *
     * @param toConvertValue 待转化的对象
     * @return InputStream
     */
    private static InputStream conversionToInputStream(Object toConvertValue) {
        return resourceToIuputStream(conversionToResource(toConvertValue));
    }

    /**
     * 将对象转化为{@link InputStream}数组
     *
     * @param toConvertValue 待转化的对象
     * @return InputStream数组
     */
    private static InputStream[] conversionToInputStreamArray(Object toConvertValue) {
        List<InputStream> inputStreamList = Stream.of(conversionToResources(toConvertValue)).map(ConversionUtils::resourceToIuputStream).collect(Collectors.toList());
        return ContainerUtils.listToArray(inputStreamList, InputStream.class);
    }

    /**
     * 将对象转化为{@link OutputStream}
     *
     * @param toConvertValue 待转化的对象
     * @return InputStream
     */
    private static OutputStream conversionToOutputStream(Object toConvertValue) {
        return resourceToOutputStream(conversionToResource(toConvertValue));
    }

    /**
     * 将对象转化为{@link OutputStream}数组
     *
     * @param toConvertValue 待转化的对象
     * @return OutputStream数组
     */
    private static OutputStream[] conversionToOutputStreamArray(Object toConvertValue) {
        List<OutputStream> outputStreamList = Stream.of(conversionToResources(toConvertValue)).map(ConversionUtils::resourceToOutputStream).collect(Collectors.toList());
        return ContainerUtils.listToArray(outputStreamList, OutputStream.class);
    }

    /**
     * 将对象转化为{@link File}
     *
     * @param toConvertValue 待转化的对象
     * @return File
     */
    private static File conversionToFile(Object toConvertValue) {
        return resourceToFile(conversionToResource(toConvertValue));
    }

    /**
     * 将对象转化为{@link File[]}
     *
     * @param toConvertValue 待转化的对象
     * @return File数组
     */
    private static File[] conversionToFileArray(Object toConvertValue) {
        List<File> fileList = Stream.of(conversionToResources(toConvertValue)).map(ConversionUtils::resourceToFile).collect(Collectors.toList());
        return ContainerUtils.listToArray(fileList, File.class);
    }

    /**
     * 将对象转化为{@link URL}
     *
     * @param toConvertValue 待转化的对象
     * @return URL
     */
    private static URL conversionToURL(Object toConvertValue) {
        return resourceToURL(conversionToResource(toConvertValue));
    }

    /**
     * 将对象转化为{@link URL}数组
     *
     * @param toConvertValue 待转化的对象
     * @return URL数组
     */
    private static URL[] conversionToURLArray(Object toConvertValue) {
        List<URL> urlList = Stream.of(conversionToResources(toConvertValue)).map(ConversionUtils::resourceToURL).collect(Collectors.toList());
        return ContainerUtils.listToArray(urlList, URL.class);
    }

    /**
     * 将对象转化为{@link URI}
     *
     * @param toConvertValue 待转化的对象
     * @return URI
     */
    private static URI conversionToURI(Object toConvertValue) {
        return resourceToURI(conversionToResource(toConvertValue));
    }

    /**
     * 将对象转化为{@link URI}数组
     *
     * @param toConvertValue 待转化的对象
     * @return URI数组
     */
    private static URI[] conversionToURIArray(Object toConvertValue) {
        List<URI> uriList = Stream.of(conversionToResources(toConvertValue)).map(ConversionUtils::resourceToURI).collect(Collectors.toList());
        return ContainerUtils.listToArray(uriList, URI.class);
    }

    /**
     * 将资源表达式解析为资源组
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
     * 将{@link Resource}对象转化为{@link File}
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
     * 将{@link Resource}对象转化为{@link InputStream}
     *
     * @param resource 资源对象
     * @return InputStream
     */
    private static InputStream resourceToIuputStream(Resource resource) {
        try {
            return resource.getInputStream();
        } catch (IOException e) {
            throw new TypeConversionException("Exception getting input stream info from resource['" + resource + "']", e);
        }
    }

    /**
     * 将{@link Resource}对象转化为{@link OutputStream}
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
     * 将{@link Resource}对象转化为{@link URL}
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
     * 将{@link Resource}对象转化为{@link URI}
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
     * 将传入的待转换的对象转化为Class对象
     *
     * @param toConvertValue 待转换的对象
     * @return Class对象
     */
    private static Class<?> conversionToClass(Object toConvertValue) {
        return ClassUtils.forName(String.valueOf(toConvertValue), Thread.currentThread().getContextClassLoader());
    }

    /**
     * 将传入的待转换的对象转化为某个指定类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param returnType 目标类型
     * @return 目标实体
     */
    private static Object conversionToPojo(Object toConvertValue, ResolvableType returnType, List<ConversionService> conversions, Function<Object, Object> function) {
        Class<?> returnClass = returnType.getRawClass();

        // 资源类型
        if (Resource.class.isAssignableFrom(returnClass)) {
            return conversionToResource(toConvertValue);
        }

        // 转化类型为文件类型
        if (File.class.isAssignableFrom(returnClass)) {
            return conversionToFile(toConvertValue);
        }

        // 转化类型为InputStream类型
        if (InputStream.class.isAssignableFrom(returnClass)) {
            return conversionToInputStream(toConvertValue);
        }

        // 转化类型为OutputStream类型
        if (OutputStream.class.isAssignableFrom(returnClass)) {
            return conversionToOutputStream(toConvertValue);
        }

        // 转化类型为URL
        if (URL.class.isAssignableFrom(returnClass)) {
            return conversionToURL(toConvertValue);
        }

        // 转化类型为URI
        if (URI.class.isAssignableFrom(returnClass)) {
            return conversionToURI(toConvertValue);
        }

        if (toConvertValue instanceof Map) {
            Map<String, Object> valueMap = (Map<String, Object>) toConvertValue;
            Field[] fields = ClassUtils.getAllFields(returnClass);
            Object resultPojo = ClassUtils.newObject(returnClass);
            for (Field field : fields) {
                String mappingName = getMappingName(field);
                if (valueMap.containsKey(mappingName)) {
                    FieldUtils.setValue(resultPojo, field, conversion(valueMap.get(mappingName), ResolvableType.forField(field, returnClass), conversions, function));
                }
            }
            return resultPojo;
        } else {
            try {
                for (ConversionService conversion : conversions) {
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
     * 将传入的待转换的对象转化为Map类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param resolvableType 目标类型
     * @return Map对象
     */
    private static Map<?, ?> conversionToMap(Object toConvertValue, ResolvableType resolvableType, List<ConversionService> conversions, Function<Object, Object> function) {
        Class<?> targetClass = resolvableType.getRawClass();
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

            Map<Object, Object> resultMap = (Map<Object, Object>) ClassUtils.createObject(targetClass, () -> new HashMap<>(valueMap.size()));
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
     * 将Pojo类型转化为Map类型
     *
     * @param mapKey 外部key
     * @param pojo   Pojo实体
     * @return Map类型结果
     */
    private static Map<?, ?> pojoToMap(String mapKey, Object pojo) {
        //未提供外部key，执行转换逻辑
        if (mapKey == null) {
            if (pojo == null) {
                return null;
            }
            Class<?> toConvertValueClass = pojo.getClass();
            if (ClassUtils.isSimpleBaseType(toConvertValueClass)
                    || Collection.class.isAssignableFrom(toConvertValueClass)
                    || toConvertValueClass.isArray()) {
                throw new TypeConversionException("Type '" + pojo.getClass() + "' cannot be converted to '" + Map.class + "' type!");
            }

            if (Map.class.isAssignableFrom(toConvertValueClass)) {
                return (Map<Object, Object>) pojo;
            }
            Map<Object, Object> resultMap = new LinkedHashMap<>();
            Field[] allFields = ClassUtils.getAllFields(toConvertValueClass);
            for (Field field : allFields) {
                String mappingName = getMappingName(field);
                if (ClassUtils.isSimpleBaseType(field.getType())) {
                    resultMap.put(mappingName, FieldUtils.getValue(pojo, field));
                } else {
                    resultMap.put(mappingName, pojoToMap(mappingName, FieldUtils.getValue(pojo, field)).get(mappingName));
                }
            }
            return resultMap;
        }
        //提供外部key，则需要构建外层Map，并将转换结果put到外层Map
        else {
            Map<Object, Object> resultMap = new LinkedHashMap<>();
            if (pojo == null) {
                resultMap.put(mapKey, null);
                return resultMap;
            }

            ResolvableType toConvertValueType = ResolvableType.forInstance(pojo);
            Class<?> toConvertValueClass = toConvertValueType.getRawClass();
            if (ClassUtils.isSimpleBaseType(toConvertValueClass)) {
                throw new TypeConversionException("Type '" + pojo.getClass() + "' cannot be converted to '" + Map.class + "' type!");
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
                    Class<?> arrayEntryClass = toConvertValueClass.getComponentType();
                    Object[] arrayObject = new Object[length];
                    for (int i = 0; i < length; i++) {
                        arrayObject[i] = pojoToMap(null, Array.get(pojo, i));
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
                        if (List.class.isAssignableFrom(toConvertValueClass)) {
                            List<Object> listValue = new ArrayList(collectionValue.size());
                            for (Object collectionEntry : collectionValue) {
                                listValue.add(pojoToMap(null, collectionEntry));
                            }
                            resultMap.put(mapKey, listValue);
                        } else if (Set.class.isAssignableFrom(toConvertValueClass)) {
                            Set<Object> setValue = new HashSet(collectionValue.size());
                            for (Object collectionEntry : collectionValue) {
                                setValue.add(pojoToMap(null, collectionEntry));
                            }
                            resultMap.put(mapKey, setValue);
                        }
                    }
                }
                return resultMap;
            }
            Map<Object, Object> map = new LinkedHashMap<>();
            Field[] allFields = ClassUtils.getAllFields(toConvertValueClass);
            for (Field field : allFields) {
                String mappingName = getMappingName(field);
                if (ClassUtils.isSimpleBaseType(field.getType())) {
                    map.put(mappingName, FieldUtils.getValue(pojo, field));
                } else {
                    map.put(mappingName, pojoToMap(mappingName, FieldUtils.getValue(pojo, field)).get(mappingName));
                }
            }
            resultMap.put(mapKey, map);
            return resultMap;

        }
    }


    /**
     * 将传入的待转换的对象转化为数组类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param returnType     目标类型
     * @param conversions    转换服务
     * @param function       元素转换器
     * @return 数组对象
     */
    private static Object conversionToArray(Object toConvertValue, ResolvableType returnType, List<ConversionService> conversions, Function<Object, Object> function) {

        ResolvableType resultArrayEntryType = returnType.getComponentType();
        Class<?> resultArrayEntryClass = resultArrayEntryType.getRawClass();

        // 待转换的对象类型继承自目标类型的元素的类型
        if (resultArrayEntryClass.isAssignableFrom(toConvertValue.getClass())) {
            Object returnArray = Array.newInstance(resultArrayEntryClass, 1);
            Array.set(returnArray, 0, toConvertValue);
            return returnArray;
        }

        // 处理特殊的数组元素类型

        Object[] specialTypeArray = null;

        // 资源类型
        if (Resource.class.isAssignableFrom(resultArrayEntryClass)) {
            specialTypeArray = conversionToResources(toConvertValue);
        }

        // 转化类型为文件类型
        else if (File.class.isAssignableFrom(resultArrayEntryClass)) {
            specialTypeArray = conversionToFileArray(toConvertValue);
        }

        // 转化类型为InputStream类型
        else if (InputStream.class.isAssignableFrom(resultArrayEntryClass)) {
            specialTypeArray = conversionToInputStreamArray(toConvertValue);
        }

        // 转化类型为OutputStream类型
        else if (OutputStream.class.isAssignableFrom(resultArrayEntryClass)) {
            specialTypeArray = conversionToOutputStreamArray(toConvertValue);
        }

        // 转化类型为URL
        else if (URL.class.isAssignableFrom(resultArrayEntryClass)) {
            specialTypeArray = conversionToURLArray(toConvertValue);
        }

        // 转化类型为URI
        else if (URI.class.isAssignableFrom(resultArrayEntryClass)) {
            specialTypeArray = conversionToURIArray(toConvertValue);
        }

        if (specialTypeArray != null) {
            return ContainerUtils.arrayDowncast(specialTypeArray, resultArrayEntryClass);
        }

        //集合
        if (toConvertValue instanceof Collection) {
            Collection<?> collectionValue = (Collection<?>) toConvertValue;
            Object arrayObject = Array.newInstance(resultArrayEntryClass, collectionValue.size());
            int i = 0;
            for (Object collectionEntry : collectionValue) {
                Array.set(arrayObject, i++, conversion(collectionEntry, resultArrayEntryType, conversions, function));
            }
            return arrayObject;
        }

        //数组
        else if (toConvertValue.getClass().isArray()) {
            int length = Array.getLength(toConvertValue);
            Object arrayObject = Array.newInstance(resultArrayEntryClass, length);
            for (int i = 0; i < length; i++) {
                Array.set(arrayObject, i, conversion(Array.get(toConvertValue, i), resultArrayEntryType, conversions, function));
            }
            return arrayObject;
        }

        //其他
        else {
            Object arrayObject = Array.newInstance(resultArrayEntryClass, 1);
            Array.set(arrayObject, 0, conversion(toConvertValue, resultArrayEntryType, conversions));
            return arrayObject;
        }
    }

    /**
     * 将传入的待转换的对象转化为Collection类型的对象
     *
     * @param toConvertValue 待转换的对象
     * @param returnType     目标类型
     * @return Collection对象
     */
    private static Collection<?> conversionToCollection(Object toConvertValue, ResolvableType returnType, List<ConversionService> conversions, Function<Object, Object> function) {

        Class<?> retuenClass = returnType.getRawClass();
        boolean resultTypeIsList = List.class.isAssignableFrom(retuenClass);

        // 处理特殊的集合类型
        if (returnType.hasGenerics()) {
            Class<?> returnGenericClass = returnType.getGeneric(0).getRawClass();
            Object[] specialTypeArray = null;

            // 资源类型
            if (Resource.class.isAssignableFrom(returnGenericClass)) {
                specialTypeArray = conversionToResources(toConvertValue);
            }

            // 转化类型为文件类型
            else if (File.class.isAssignableFrom(returnGenericClass)) {
                specialTypeArray = conversionToFileArray(toConvertValue);
            }

            // 转化类型为InputStream类型
            else if (InputStream.class.isAssignableFrom(returnGenericClass)) {
                specialTypeArray = conversionToInputStreamArray(toConvertValue);
            }

            // 转化类型为OutputStream类型
            else if (OutputStream.class.isAssignableFrom(returnGenericClass)) {
                specialTypeArray = conversionToOutputStreamArray(toConvertValue);
            }

            // 转化类型为URL
            else if (URL.class.isAssignableFrom(returnGenericClass)) {
                specialTypeArray = conversionToURLArray(toConvertValue);
            }

            // 转化类型为URI
            else if (URI.class.isAssignableFrom(returnGenericClass)) {
                specialTypeArray = conversionToURIArray(toConvertValue);
            }
            if (specialTypeArray != null) {
                if (resultTypeIsList) {
                    List<Object> list = (List<Object>) ClassUtils.createObject(retuenClass, () -> new ArrayList<>());
                    ContainerUtils.copyToList(ContainerUtils.arrayDowncast(specialTypeArray, returnGenericClass), list);
                    return list;
                }
                Set<Object> set = (Set<Object>) ClassUtils.createObject(retuenClass, () -> new HashSet<>());
                ContainerUtils.copyToSet(ContainerUtils.arrayDowncast(specialTypeArray, returnGenericClass), set);
                return set;
            }
        }

        //List
        if (resultTypeIsList) {
            List<Object> list;

            //value是集合
            if (toConvertValue instanceof Collection) {
                Collection<?> collectionValue = (Collection<?>) toConvertValue;
                list = (List<Object>) ClassUtils.createObject(retuenClass, () -> new ArrayList<>(collectionValue.size()));
                for (Object collectionEntry : collectionValue) {
                    list.add(conversion(collectionEntry, returnType.getGeneric(0), conversions, function));
                }
                return list;
            }
            //value是数组
            else if (toConvertValue.getClass().isArray()) {
                int length = Array.getLength(toConvertValue);
                if (length == 0) {
                    return (Collection<?>) ClassUtils.createObject(retuenClass, () -> new ArrayList<>(0));
                }

                ResolvableType arrayEntryType = returnType.hasGenerics() ? returnType.getGeneric(0) : ResolvableType.forInstance(toConvertValue).getComponentType();
                list = (List<Object>) ClassUtils.createObject(retuenClass, () -> new ArrayList<>(length));
                for (int i = 0; i < length; i++) {
                    list.add(conversion(Array.get(toConvertValue, i), arrayEntryType, conversions, function));
                }
                return list;
            }
            //其他类型
            else {
                list = (List<Object>) ClassUtils.createObject(retuenClass, () -> new ArrayList<>(1));
                ResolvableType arrayEntryType = returnType.hasGenerics() ? returnType.getGeneric(0) : ResolvableType.forInstance(toConvertValue).getComponentType();
                list.add(conversion(toConvertValue, arrayEntryType, conversions));
                return list;
            }
        }
        //Set
        else {
            Set<Object> set;

            //value是集合
            if (toConvertValue instanceof Collection) {
                Collection<?> collectionValue = (Collection<?>) toConvertValue;
                set = (Set<Object>) ClassUtils.createObject(retuenClass, () -> new HashSet<>(collectionValue.size()));
                for (Object collectionEntry : collectionValue) {
                    set.add(conversion(collectionEntry, returnType.getGeneric(0), conversions, function));
                }
                return set;
            }
            //value是数组
            else if (toConvertValue.getClass().isArray()) {
                int length = Array.getLength(toConvertValue);
                if (length == 0) {
                    return (Collection<?>) ClassUtils.createObject(retuenClass, () -> new HashSet<>(0));
                }

                ResolvableType arrayEntryType = returnType.hasGenerics() ? returnType.getGeneric(0) : ResolvableType.forInstance(toConvertValue).getComponentType();
                set = (Set<Object>) ClassUtils.createObject(retuenClass, () -> new HashSet<>(length));
                for (int i = 0; i < length; i++) {
                    set.add(conversion(Array.get(toConvertValue, i), arrayEntryType, conversions, function));
                }
                return set;
            }
            //其他类型
            else {
                set = (Set<Object>) ClassUtils.createObject(retuenClass, () -> new HashSet<>(1));
                ResolvableType arrayEntryType = returnType.hasGenerics() ? returnType.getGeneric(0) : ResolvableType.forInstance(toConvertValue).getComponentType();
                set.add(conversion(toConvertValue, arrayEntryType, conversions));
                return set;
            }
        }
    }

    /**
     * 将传入的待转换的对象转化为基本类型的值
     *
     * @param toConvertValue 待转换的对象
     * @param baseType       基本类型
     * @param <T>            基本类型泛型
     * @return 基本类型值
     */
    private static <T> T conversionToBaseType(Object toConvertValue, Class<T> baseType) {
        String stringValue;
        if (ContainerUtils.isIterable(toConvertValue)) {
            Iterator<Object> iterator = ContainerUtils.getIterator(toConvertValue);
            stringValue = String.valueOf(ContainerUtils.getIteratorFirst(iterator));
        } else {
            stringValue = String.valueOf(toConvertValue);
        }
        boolean canCalculated = ClassUtils.isCanCalculated(baseType);
        try {
            return JavaConversion.fromString(stringValue, baseType, canCalculated);
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
