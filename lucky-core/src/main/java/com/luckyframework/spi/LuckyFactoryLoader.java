package com.luckyframework.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.objenesis.instantiator.util.ClassUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LUCKY-SPI
 * @author fk
 * @version 1.0
 * @date 2020/12/16 0016 8:59
 */
public final class LuckyFactoryLoader {

    private static final Logger log= LoggerFactory.getLogger("c.l.framework.spi.LuckyFactoryLoader");

    private static final String FACTORIES_RESOURCE_LOCATION = "META-INF/lucky.factories";
    static final Map<ClassLoader, Map<String, List<String>>> cache = new LinkedHashMap<>();

    private LuckyFactoryLoader() {

    }

    public static Set<Class<?>> loadFactoryClasses(Class<?> factoryType,ClassLoader classLoader){
        ClassLoaderAndFactoryNames cf = getClassLoaderAndFactoryNames(factoryType, classLoader);
        List<String> factoryImplementationNames=cf.getFactoryImplementationNames();
        Set<Class<?>> result = new HashSet<>(factoryImplementationNames.size());
        for (String factoryImplementationName : factoryImplementationNames) {
            result.add(forClassFactory(factoryImplementationName, factoryType, cf.getClassLoader()));
        }
        return result;
    }

    /**
     * 得到有关于某个类型的所有SPI实现类实例
     * @param factoryType 待查询的类型
     * @param classLoader 指定一个类加载器，如果没有指定则使用本类的类加载器
     * @param <T>
     * @return
     */
    public static <T> List<T> loadFactories(Class<T> factoryType,ClassLoader classLoader) {
        ClassLoaderAndFactoryNames cf = getClassLoaderAndFactoryNames(factoryType, classLoader);
        List<String> factoryImplementationNames=cf.getFactoryImplementationNames();
        List<T> result = new ArrayList<>(factoryImplementationNames.size());
        for (String factoryImplementationName : factoryImplementationNames) {
            result.add(instantiateFactory(factoryImplementationName, factoryType, cf.getClassLoader()));
        }
//        AnnotationAwareOrderComparator.sort(result);
        return result;
    }

    private static ClassLoaderAndFactoryNames getClassLoaderAndFactoryNames(Class<?> factoryType,ClassLoader classLoader){
        ClassLoaderAndFactoryNames cf=new ClassLoaderAndFactoryNames();
        Assert.notNull(factoryType, "'factoryType' must not be null");
        ClassLoader classLoaderToUse = classLoader;
        if (classLoaderToUse == null) {
            classLoaderToUse = LuckyFactoryLoader.class.getClassLoader();
        }
        cf.setClassLoader(classLoaderToUse);
        List<String> factoryImplementationNames = loadFactoryNames(factoryType, classLoaderToUse);
        cf.setFactoryImplementationNames(factoryImplementationNames);
        if (log.isTraceEnabled()) {
            log.trace("Loaded [" + factoryType.getName() + "] names: " + factoryImplementationNames);
        }
        return cf;
    }

    /**
     * 得到有关于某个类型的所有SPI实现类的全限定名
     * @param factoryType 待查询的类型
     * @param classLoader 指定一个类加载器，如果没有指定则使用本类的类加载器
     * @return
     */
    public static List<String> loadFactoryNames(Class<?> factoryType, ClassLoader classLoader) {
        ClassLoader classLoaderToUse = classLoader;
        if (classLoaderToUse == null) {
            classLoaderToUse = LuckyFactoryLoader.class.getClassLoader();
        }
        String factoryTypeName = factoryType.getName();
        return loadLuckyFactories(classLoaderToUse).getOrDefault(factoryTypeName, Collections.emptyList());
    }


    /**
     * 解析 META-INF/lucky.factories文件
     * @param classLoader 类加载器
     * @return
     */
    private static Map<String, List<String>> loadLuckyFactories(ClassLoader classLoader) {
        Map<String, List<String>> result = cache.get(classLoader);
        if (result != null) {
            return result;
        }

        result = new HashMap<>();
        try {
            Enumeration<URL> urls = classLoader.getResources(FACTORIES_RESOURCE_LOCATION);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                InputStream inputStream = url.openStream();
                Properties properties =new Properties();
                properties.load(inputStream);
                for (Map.Entry<?, ?> entry : properties.entrySet()) {
                    String factoryTypeName = ((String) entry.getKey()).trim();
                    String[] factoryImplementationNames =
                            StringUtils.commaDelimitedListToStringArray((String) entry.getValue());
                    for (String factoryImplementationName : factoryImplementationNames) {
                        result.computeIfAbsent(factoryTypeName, key -> new ArrayList<>())
                                .add(factoryImplementationName.trim());
                    }
                }
            }

            // Replace all lists with unmodifiable lists containing unique elements
            result.replaceAll((factoryType, implementations) -> implementations.stream().distinct()
                    .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList)));
            cache.put(classLoader, result);
        }
        catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load factories from location [" +
                    FACTORIES_RESOURCE_LOCATION + "]", ex);
        }
        return result;
    }


    /**
     * 实例化一个SPI抽象的实例
     * @param factoryImplementationName SPI实例的全限定名
     * @param factoryType SPI抽象类型
     * @param classLoader 类加载器
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <T> T instantiateFactory(String factoryImplementationName, Class<T> factoryType, ClassLoader classLoader) {
        try {
            Class<?> factoryImplementationClass = ClassUtils.getExistingClass(classLoader,factoryImplementationName);
            if (!factoryType.isAssignableFrom(factoryImplementationClass)) {
                throw new IllegalArgumentException(
                        "Class [" + factoryImplementationName + "] is not assignable to factory type [" + factoryType.getName() + "]");
            }
            return (T) ClassUtils.newInstance(factoryImplementationClass);
        }
        catch (Throwable ex) {
            throw new IllegalArgumentException(
                    "Unable to instantiate factory class [" + factoryImplementationName + "] for factory type [" + factoryType.getName() + "]",
                    ex);
        }
    }

    /**
     * 得到一个SPI抽象的具体Class
     * @param factoryImplementationName SPI实例的全限定名
     * @param factoryType SPI抽象类型
     * @param classLoader 类加载器
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Class<?> forClassFactory(String factoryImplementationName, Class<?> factoryType, ClassLoader classLoader) {
        try {
            Class<?> factoryImplementationClass = ClassUtils.getExistingClass(classLoader,factoryImplementationName);
            if(factoryType.isAnnotation()){
                Class<? extends Annotation> factoryTAnnType= (Class<? extends Annotation>) factoryType;
                if(factoryImplementationClass.isAnnotationPresent(factoryTAnnType)){
                    return factoryImplementationClass;
                }
                throw new IllegalArgumentException(
                        "Class [" + factoryImplementationName + "] is not assignable to factory type [(Annotation) " + factoryType.getName() + "]");
            }
            if (!factoryType.isAssignableFrom(factoryImplementationClass)) {
                throw new IllegalArgumentException(
                        "Class [" + factoryImplementationName + "] is not assignable to factory type [" + factoryType.getName() + "]");
            }
            return factoryImplementationClass;
        }
        catch (Throwable ex) {
            throw new IllegalArgumentException(
                    "Unable to instantiate factory class [" + factoryImplementationName + "] for factory type [" + factoryType.getName() + "]",
                    ex);
        }
    }

    static class ClassLoaderAndFactoryNames{
        private ClassLoader classLoader;
        private List<String> factoryImplementationNames;

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public void setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public List<String> getFactoryImplementationNames() {
            return factoryImplementationNames;
        }

        public void setFactoryImplementationNames(List<String> factoryImplementationNames) {
            this.factoryImplementationNames = factoryImplementationNames;
        }
    }

}
