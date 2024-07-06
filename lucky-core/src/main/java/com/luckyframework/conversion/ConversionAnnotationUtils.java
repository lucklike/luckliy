package com.luckyframework.conversion;

import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.spel.SpELRuntime;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 转换器注解相关的工具类
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/4 20:55
 */
public class ConversionAnnotationUtils {

    /**
     * 创建一个转换器
     * @param targetType 目标类型
     * @param targetType 原类型
     * @return 转换器实例
     */
    @SuppressWarnings("all")
    public static SpELConversion createConversion(ResolvableType targetType, ResolvableType sourceType){
        Class<?> targetClass = ConversionService.getConversionClass(targetType);
        Class<?> sourceClass = ConversionService.getConversionClass(sourceType);
        return new SpELConversion<>(targetClass, sourceClass);
    }

    /**
     * 创建一个转换器
     * @param conversionInterfaceClass 转换器所在类
     * @param conversionMethod         转换器所在方法
     * @return  转换器实例
     */
    @SuppressWarnings("all")
    public static SpELConversion createConversion(Class<?> conversionInterfaceClass, Method conversionMethod){
        SpELConversion conversion = createConversion(ResolvableType.forMethodReturnType(conversionMethod), ResolvableType.forMethodParameter(conversionMethod, 0));
        convertInitialize(conversion, conversionMethod);
        return conversion;
    }

    /**
     * 创建一个转换器
     * @param conversionMethod 转换器方法实例
     * @return 转换器实例
     */
    @SuppressWarnings("all")
    public static SpELConversion createConversion(Method conversionMethod){
        return createConversion(conversionMethod.getDeclaringClass(), conversionMethod);
    }

    /**
     * 转换器初始化，设置mapping、ignoredField、fieldSuppler以及initialTargetSuppler信息
     * @param conversionService 转换器
     * @param method            转换器所在方法
     */
    @SuppressWarnings("all")
    public static void convertInitialize(SpELConversion conversionService, Method method){
        // 收集mapping信息
        ConversionAnnotationUtils.getMapping(method).forEach((k, v) -> conversionService.addMapping(k, v));

        // 收集禁止转换的属性信息，并将这些映射关系从mapping中移除
        TempPair<String[], String[]> pair = ConversionAnnotationUtils.getIgnoredFieldPair(method);
        conversionService.removeTargetMapping(pair.getOne());
        conversionService.removeSourceMapping(pair.getTwo());

        // 收集属性初始化方法
        ConversionAnnotationUtils.getFieldSuppler(method).forEach((k, v) -> conversionService.addFieldConstruction(k, v));
        // 设置目标对象的初始化方法
        String targetInitialSuppler = ConversionAnnotationUtils.getTargetInitialSuppler(method);
        if(StringUtils.hasText(targetInitialSuppler)){
            conversionService.setInitialTargetSuppler(targetInitialSuppler);
        }
    }

    /**
     * 尝试注册一个转化器,如果该方法被{@link ConversionRegister @ConversionRegister}
     * 标注则会注册到ConversionManager中，该注解只能用在{@link Interconversion}接口的子接口方法上
     * @param method            转换器所在方法
     * @param conversionService 转换器实例
     */
    @SuppressWarnings("all")
    public static void tryToRegisterConvert(Method method, ConversionService conversionService){
        ConversionRegister sourceAnn = AnnotationUtils.findMergedAnnotation(method, ConversionRegister.class);
        if(sourceAnn != null){
            String conversionName = StringUtils.hasText(sourceAnn.value()) ? sourceAnn.value() : method.getName();
            ConversionManager.registryConversionService(conversionName, conversionService);
        }
    }

    /**
     * 获取所有{@link Mapping @Mapping}注解中的属性映射信息
     * @param method 代理方法
     * @return 配置的属性映射信息
     */
    public static Map<String, String> getMapping(Method method){
        Map<String, String> mapping = new HashMap<>();
        Mapping[] mappingAnnotations = getMappingAnnotations(method);
        for (Mapping mappingAnnotation : mappingAnnotations) {
            mapping.put(mappingAnnotation.target(), mappingAnnotation.source());
        }
        return mapping;
    }

    /**
     * 获取{@link UseConversion @UseConversion}注解中导入的所有的类型转换器
     * 并讲这些转换器转换成{@link ConversionService}实例
     * @return 导入的其他转化器
     */
    @SuppressWarnings("all")
    public static List<ConversionService> getUseConversionService(SpELRuntime spELRuntime, AnnotatedElement conversionAnnotatedElement){
        List<ConversionService> useConversionList = new ArrayList<>();
        UseConversion useConversion = AnnotationUtils.findMergedAnnotation(conversionAnnotatedElement, UseConversion.class);
        if(useConversion != null){
            Class<?>[] classes = useConversion.classes();
            for (Class<?> aClass : classes) {

                // 当导入的接口为Interconversion接口的子接口时
                if(Interconversion.class != aClass && Interconversion.class.isAssignableFrom(aClass)){
                    Interconversion interconversion = ConversionUtils.getConversionServiceProxy(spELRuntime, (Class<? extends Interconversion>)aClass);
                    useConversionList.add(interconversion.getSourceConversion());
                    useConversionList.add(interconversion.getTargetConversion());
                }

                // 当导入的接口为普通接口时
                else{
                    getUseConversionName(aClass).forEach(n -> useConversionList.add(ConversionManager.getConversionService(n)));
                }
            }

            // 注册使用名称导入的转换器
            String[] names = useConversion.names();
            for (String name : names) {
                ConversionService service = ConversionManager.getConversionService(name);
                Assert.notNull(service, "Cannot import a converter with name '"+name+"' for '"+conversionAnnotatedElement.toString()+"' because there is no converter with that name!");
                useConversionList.add(service);
            }
        }
        return useConversionList;
    }

    /**
     * 获取{@link FieldSuppler @FieldSuppler}注解中
     * 配置的所有属性的初始化方式(SpEL表达式方式)
     * @param method 代理方法
     * @return 属性的初始化方式集合
     */
    public static Map<String, String> getFieldSuppler(Method method){
        Map<String, String> fieldSuppMap = new HashMap<>();
        FieldSuppler[] fieldSupplers = getFieldSupplerAnnotations(method);
        for (FieldSuppler suppler : fieldSupplers) {
            fieldSuppMap.put(suppler.name(), suppler.suppler());
        }
        return fieldSuppMap;
    }

    /**
     * 获取{@link TargetInitialSuppler @TargetInitialSuppler}注解中配置的
     * 目标类型对象的初始化方式(SpEL表达式方式)
     * @param method 代理方法
     * @return 目标类型对象的初始化方式
     */
    public static String getTargetInitialSuppler(Method method){
        TargetInitialSuppler initialSuppler = AnnotationUtils.findMergedAnnotation(method, TargetInitialSuppler.class);
        return initialSuppler == null ? null : initialSuppler.value();
    }

    /**
     * 获取{@link IgnoreField @IgnoreField}注解中配置的那些被禁止转换的属性信息
     * one: target
     * two: source
     * @param method 代理方法
     * @return 禁止转换的属性信息
     */
    public static TempPair<String[], String[]> getIgnoredFieldPair(Method method){
        IgnoreField ignoreField = AnnotationUtils.findMergedAnnotation(method, IgnoreField.class);
        if(ignoreField == null){
            return TempPair.of(new String[0], new String[0]);
        }
        return TempPair.of(ignoreField.targets(),ignoreField.sources());
    }

    /**
     * 获取代理方法中所有的{@link FieldSuppler @FieldSuppler}注解
     * @param method 代理方法
     * @return 方法上所有的@FieldSuppler注解实例
     */
    public static FieldSuppler[] getFieldSupplerAnnotations(Method method){
        if(method.isAnnotationPresent(FieldSupplers.class)){
            return method.getAnnotation(FieldSupplers.class).value();
        }
        if(method.isAnnotationPresent(FieldSuppler.class))
            return new FieldSuppler[]{method.getAnnotation(FieldSuppler.class)};
        return new FieldSuppler[]{};
    }

    /**
     * 获取代理方法中所有的{@link Mapping @Mapping}注解
     * @param method 代理方法
     * @return 方法上所有的@Mapping注解实例
     */
    private static Mapping[] getMappingAnnotations(Method method){
        if(method.isAnnotationPresent(Mappings.class)){
            return method.getAnnotation(Mappings.class).value();
        }
        if(method.isAnnotationPresent(Mapping.class))
            return new Mapping[]{method.getAnnotation(Mapping.class)};
        return new Mapping[]{};
    }

    /**
     * 获取一个普通接口Class中存在的所有转换器的名字
     * @param conversionClass 普通接口的Class
     * @return 该接口Class中存在的所有转换器的名字
     */
    public static Set<String> getUseConversionName(Class<?> conversionClass){
        List<Method> conversionMethods = ClassUtils.getMethodByStrengthenAnnotation(conversionClass, ConversionMethod.class);
        Set<String> conversionNameSet = new HashSet<>(conversionMethods.size());
        for (Method conversionMethod : conversionMethods) {
            String conversionName = AnnotationUtils.findMergedAnnotation(conversionMethod, ConversionMethod.class).name();
            conversionName = StringUtils.hasText(conversionName) ? conversionName : conversionMethod.getName();
            conversionNameSet.add(conversionName);
        }
        return conversionNameSet;
    }
}
