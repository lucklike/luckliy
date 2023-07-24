package com.luckyframework.conversion;

import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.SpELRuntime;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于SpEL表达式的类型转化器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/1 05:31
 */
@SuppressWarnings("all")
public class SpELConversion<T, S> implements ConversionService<T, S>{

    /** 原类型*/
    private final Class<S> sourceClass;
    /** 目标类型*/
    private final Class<T> targetClass;

    /** 转化器签名,转换器的唯一ID*/
    private final String signature;
    /** 目标对象的构造方法*/
    private Supplier<T> initialTargetSuppler;
    /** 内置的SpEL转化器,当属性转化中的两种类型可以被内置SpEL转换器转换时会优先使用转换器转换*/
    private final List<ConversionService> useConversions = new ArrayList<>();
    /** 属性映射关系*/
    private final Map<String, String> mapping;
    /** 真实类中属性的构造方式*/
    private final Map<String, Supplier<Object>> fieldConstructionMap;
    /** 内置的SpEL转换器的方法签名集合*/
    private final Set<String> useConversionsSignatureSet = new HashSet<>();
    /** SpEL表达式运行时环境*/
    private SpELRuntime spELRuntime = new SpELRuntime();
    /** 导入的SpEL包*/
    private final List<String> spelImports = new ArrayList<>();

    /**
     * SpEL转换器构造器
     * @param targetClass               原类型
     * @param sourceClass               目标类型
     * @param useConversions            内置的SpEL转化器
     * @param specialMapping            特殊属性映射关系
     * @param initialTargetSuppler      目标对象的构造方法
     * @param fieldConstructionMap      真实类中属性的构造方式
     * @param ignoredSourceFieldNames   转换中需要忽略的原对象属性名集合
     * @param ignoredTargetFieldNames   转换中需要忽略的真实对象的属性名集合
     */
    public SpELConversion(@NonNull Class<T> targetClass,
                          @NonNull Class<S> sourceClass,
                          @NonNull List<SpELConversion> useConversions,
                          @NonNull Map<String, String> specialMapping,
                          @NonNull Supplier<T> initialTargetSuppler,
                          @NonNull Map<String, Supplier<Object>> fieldConstructionMap,
                          @NonNull Set<String> ignoredSourceFieldNames,
                          @NonNull Set<String> ignoredTargetFieldNames) {
        checkConversionType(sourceClass);
        checkConversionType(targetClass);

        this.sourceClass = sourceClass;
        this.targetClass = targetClass;
        this.signature = getSignature(targetClass, sourceClass);
        this.initialTargetSuppler = initialTargetSuppler;
        this.mapping = getMapping(specialMapping, ignoredSourceFieldNames, ignoredTargetFieldNames);
        this.fieldConstructionMap = fieldConstructionMap;
        addConversions(useConversions);
    }

    /**
     * SpEL转换器构造器
     * @param targetClass               原类型
     * @param sourceClass               目标类型
     * @param useConversions            内置的SpEL转化器
     * @param specialMapping            特殊属性映射关系
     * @param initialSpELExpression     构建真实对象的SpEL表达式
     * @param fieldConstructionMap      真实类中属性的构造方式
     * @param ignoredSourceFieldNames   转换中需要忽略的原对象属性名集合
     * @param ignoredTargetFieldNames   转换中需要忽略的真实对象的属性名集合
     */
    public SpELConversion(@NonNull Class<T> targetClass,
                          @NonNull Class<S> sourceClass,
                          @NonNull List<SpELConversion> useConversions,
                          @NonNull Map<String, String> specialMapping,
                          @NonNull String initialSpELExpression,
                          @NonNull Map<String, Supplier<Object>> fieldConstructionMap,
                          @NonNull Set<String> ignoredSourceFieldNames,
                          @NonNull Set<String> ignoredTargetFieldNames){
        this(targetClass, sourceClass, useConversions, specialMapping, (Supplier<T>) null, fieldConstructionMap, ignoredSourceFieldNames, ignoredTargetFieldNames);
        setInitialTargetSuppler(initialSpELExpression);
    }

    /**
     * SpEL转换器构造器(默认使用目标类的无参构造器初始化目标对象)
     * @param targetClass               原类型
     * @param sourceClass               目标类型
     * @param useConversions            内置的SpEL转化器
     * @param specialMapping            特殊属性映射关系
     * @param fieldConstructionMap      真实类中属性的构造方式
     * @param ignoredSourceFieldNames   转换中需要忽略的原对象属性名集合
     * @param ignoredTargetFieldNames   转换中需要忽略的真实对象的属性名集合
     */
    public SpELConversion(@NonNull Class<T> targetClass,
                          @NonNull Class<S> sourceClass,
                          @NonNull List<SpELConversion> useConversions,
                          @NonNull Map<String, String> specialMapping,
                          @NonNull Map<String, Supplier<Object>> fieldConstructionMap,
                          @NonNull Set<String> ignoredSourceFieldNames,
                          @NonNull Set<String> ignoredTargetFieldNames){
        this(targetClass, sourceClass, useConversions, specialMapping, () -> ClassUtils.newObject(targetClass), fieldConstructionMap, ignoredSourceFieldNames, ignoredTargetFieldNames);
    }

    /**
     * SpEL转换器构造器(默认使用目标类的无参构造器初始化目标对象)
     * @param targetClass               原类型
     * @param sourceClass               目标类型
     * @param useConversions            内置的SpEL转化器
     * @param specialMapping            特殊属性映射关系
     */
    public SpELConversion(@NonNull Class<T> targetClass,
                          @NonNull Class<S> sourceClass,
                          @NonNull List<SpELConversion> useConversions,
                          @NonNull Map<String, String> specialMapping){
        this(targetClass, sourceClass, useConversions, specialMapping, () -> ClassUtils.newObject(targetClass), new HashMap<>(), new HashSet<>(), new HashSet<>());
    }

    /**
     * SpEL转换器构造器(默认使用目标类的无参构造器初始化目标对象)
     * @param targetClass               原类型
     * @param sourceClass               目标类型
     * @param specialMapping            特殊属性映射关系
     */
    public SpELConversion(@NonNull Class<T> targetClass,
                          @NonNull Class<S> sourceClass,
                          @NonNull Map<String, String> specialMapping){
        this(targetClass, sourceClass, new ArrayList<>(), specialMapping, () -> ClassUtils.newObject(targetClass), new HashMap<>(), new HashSet<>(), new HashSet<>());
    }

    /**
     * SpEL转换器构造器(默认使用目标类的无参构造器初始化目标对象)
     * @param targetClass               原类型
     * @param sourceClass               目标类型
     */
    public SpELConversion(@NonNull Class<T> targetClass,
                          @NonNull Class<S> sourceClass){
        this(targetClass, sourceClass, new ArrayList<>(), new HashMap<>(), () -> ClassUtils.newObject(targetClass), new HashMap<>(), new HashSet<>(), new HashSet<>());
    }

    /**
     * 转换类型校验，转换类型不可以为基本类型、数组类型、集合类型以及Map类型
     * @param conversionType 转换类型
     */
    private void checkConversionType(Class<?> conversionType){
        // 不可以是基本类型
        if(ClassUtils.isSimpleBaseType(conversionType)){
            throw new IllegalArgumentException("A conversion type cannot be considered a base type: " + conversionType);
        }
        // 不可以是数组
        if(conversionType.isArray()){
            throw new IllegalArgumentException("The conversion type cannot be an array type: " + conversionType);
        }
        // 不可以是集合
        if(Collection.class.isAssignableFrom(conversionType)){
            throw new IllegalArgumentException("The transformation type cannot be a collection type: " + conversionType);
        }
        // 不可以是Map
        if(Collection.class.isAssignableFrom(conversionType)){
            throw new IllegalArgumentException("The conversion type cannot be Map:"  + conversionType);
        }
    }

    /**
     * 获取原始类型Class
     * @return 原始类型Class
     */
    public Class<S> getSourceClass() {
        return sourceClass;
    }

    /**
     * 获取真实类型Class
     * @return 真实类型Class
     */
    public Class<T> getTargetClass() {
        return targetClass;
    }

    /**
     * 获取该转换器的签名信息
     * @return 转换器的签名信息
     */
    public String getSignature() {
        return signature;
    }

    /**
     * 获取SpEL运行时环境
     * @return SpEL运行时环境
     */
    public SpELRuntime getSpELRuntime() {
        return spELRuntime;
    }

    /**
     * 设置SpEL表达式的运行环境
     * @param spELRuntime SpEL表达式的运行环境
     * @return 对象本身
     */
    public SpELConversion<T,S> setSpELRunTime(SpELRuntime spELRuntime){
        this.spELRuntime = spELRuntime;
        return this;
    }

    /**
     * 导入一个执行SpEL表达式时所依赖的包
     * @param packageNames 依赖包
     * @return 对象本身
     */
    public SpELConversion<T,S> importPackages(String... packageNames){
        for (String packageName : packageNames) {
            if(!this.spelImports.contains(packageName)){
                this.spelImports.add(packageName);
            }
        }
        return this;
    }

    /**
     * 获取执行SpEL表达式时所依赖的包
     * @return 执行SpEL表达式时所依赖的包
     */
    public String[] getImportPackages(){
        return this.spelImports.toArray(new String[0]);
    }

    /**
     * 添加一个内置SpEL转换器，返回对象本身(支持使用链式调用)
     * @param conversion SpEL转换器
     * @return 对象本身
     */
    public SpELConversion<T,S> addConversion(ConversionService conversion){
        addUseConversion(conversion);
        return this;
    }

    /**
     * 设置真实对象的初始化构造方式，返回对象本身(支持使用链式调用)
     * @param initialTargetSuppler 真实对象的初始化构造方式
     * @return 对象本身
     */
    public SpELConversion<T,S> setInitialTargetSuppler(Supplier<T> initialTargetSuppler){
        this.initialTargetSuppler = initialTargetSuppler;
        return this;
    }

    /**
     * 设置初始化真实对象的SpEL表达式，返回对象本身(支持使用链式调用)
     * @param initialSpELExpression 初始化真实对象的SpEL表达式
     * @return 对象本身
     */
    public SpELConversion<T,S> setInitialTargetSuppler(String initialSpELExpression){
        this.initialTargetSuppler = () -> spELRuntime.getValueForType(new ParamWrapper(initialSpELExpression).importPackage(getImportPackages()).setExpectedResultType(targetClass));
        return this;
    }

    /**
     * 添加一个组内置SpEL转换器，返回对象本身(支持使用链式调用)
     * @param conversions SpEL转换器集合
     * @return 对象本身
     */
    public SpELConversion<T,S> addConversions(Collection<SpELConversion> conversions){
        conversions.forEach(this::addConversion);
        return this;
    }

    /**
     * 添加一个属性映射
     * @param targetName 真实对象的属性
     * @param sourceName 原类型的属性(支持SpEL表达式)
     * @return 对象本身
     */
    public SpELConversion<T,S> addMapping(String targetName, String sourceName){
        this.mapping.put(targetName, sourceName);
        return this;
    }


    /**
     * 添加一个用于构造真实对象某个属性的SpEL表达式
     * @param targetName            真实对象的属性
     * @param fieldConstructionEx   该属性的构造方式
     * @return 对象本身
     */
    public SpELConversion<T,S> addFieldConstruction(String targetName, String fieldConstructionEx){
        this.fieldConstructionMap.put(targetName, () -> spELRuntime.getValueForType(new ParamWrapper(fieldConstructionEx).importPackage(getImportPackages())));
        return this;
    }

    /**
     * 添加一个真实对象中某个属性属性的构造方式
     * @param targetName            真实对象的属性
     * @param fieldConstruction   该属性的构造方式
     * @return 对象本身
     */
    public SpELConversion<T,S> addFieldConstruction(String targetName, Supplier<Object> fieldConstruction){
        this.fieldConstructionMap.put(targetName, fieldConstruction);
        return this;
    }

    public SpELConversion<T, S> removeTargetMapping(String ...fields){
        for (String field : fields) {
            mapping.remove(field);
        }
        return this;
    }

    public SpELConversion<T, S> removeSourceMapping(String ...fields){
            Set<String> removeNames = Stream.of(fields).collect(Collectors.toSet());
        Iterator<Map.Entry<String, String>> iterator = mapping.entrySet().iterator();
        while (iterator.hasNext()){
            if(removeNames.contains(iterator.next().getValue())){
                iterator.remove();
            }
        }
        return this;
    }

    /**
     * 该SpEL属性转换器中是否包含指定的的SpEL转换器
     * @param conversion 指定的的SpEL转换器
     * @return 是否包含
     */
    public boolean hasConversion(ConversionService conversion){
        return useConversionsSignatureSet.contains(conversion.getSignature()) || getSignature().equals(conversion.getSignature());
    }

    /**
     * 判断给定的原类型Class和真实类型Class是否可以被本转换器转换
     * @param sourceClass 原类型Class
     * @param targetClass 真实类型Class
     * @return 是否可以转换
     */
    public boolean canConvert(Class<?> targetClass, Class<?> sourceClass){
        return sourceClass == this.sourceClass && targetClass == this.targetClass;
    }

    /**
     * 将原对象转换为真实对象
     * @param sourceObject 原对象
     * @return 真实对象
     */
    @Override
    public T conversion(S sourceObject){
        return createTargetObject(sourceObject);
    }


    /**
     * 判断给定的原类型Class和真实类型Class是否可以被本转换器转换
     * @param targetType 原类型
     * @param sourceType 真实类型
     * @return 是否可以转换
     */
    @Override
    public boolean canConvert(ResolvableType targetType, ResolvableType sourceType) {
        return this.sourceClass == sourceType.getRawClass() && this.targetClass == targetType.getRawClass();
    }

    @Override
    public void addUseConversion(ConversionService<?, ?> conversionService) {
        if(!hasConversion(conversionService)){
            useConversions.add(conversionService);
            useConversionsSignatureSet.add(conversionService.getSignature());
        }
    }


    /**
     * 获取转化过程中的所有属性映关系
     * @param specialMapping            指定的特殊的属性映射关系
     * @param ignoredSourceFieldNames   转换中需要忽略的原对象属性名集合
     * @param ignoredTargetFieldNames   转换中需要忽略的真实对象属性名集合
     * @return 属性映射关系
     */
    private Map<String, String> getMapping(Map<String, String> specialMapping, Set<String> ignoredSourceFieldNames, Set<String> ignoredTargetFieldNames){
        Map<String, String> mapping = new HashMap<>();
        mapping.putAll(getDefaultMapping());
        mapping.putAll(specialMapping);
        Iterator<Map.Entry<String, String>> iterator = mapping.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, String> entry = iterator.next();
            String targetName = entry.getKey();
            String sourceName = entry.getValue();
            if(ignoredSourceFieldNames.contains(sourceName) || ignoredTargetFieldNames.contains(targetName)){
                iterator.remove();
            }
        }
        return mapping;
    }

    /**
     * 获取默认的属性映射关系(即同名属性)
     * @return 默认的属性映射关系
     */
    private Map<String, String> getDefaultMapping(){
        Field[] sourceFields = ClassUtils.getAllFields(sourceClass);
        Set<String> targetFieldNameSet = Stream.of(ClassUtils.getAllFields(targetClass)).map(Field::getName).collect(Collectors.toSet());
        Map<String, String> defaultMapping = new HashMap<>();
        for (Field sourceField : sourceFields) {
            String sourceName = sourceField.getName();
            if(targetFieldNameSet.contains(sourceName)){
                defaultMapping.put(sourceName, sourceName);
            }
        }
        return defaultMapping;
    }

    /**
     * 创建真实对象
     * @param sourceObject 原对象
     * @return 真实对象
     */
    private T createTargetObject(S sourceObject){
        T targetObject = initialTargetSuppler.get();
        mapping.keySet().forEach((targetField) -> initializeTargetObjectFieldChain(spELRuntime, getImportPackages(), targetObject, fieldConstructionMap, targetField));
        targetMappingSetting(targetObject, sourceObject);
        return targetObject;
    }

    /**
     * 初始化真实对象中指定的属性链
     * @param targetObject          真实对象
     * @param fieldConstructionMap  注册的属性构造方法
     * @param fieldChain            属性链
     */
    public static void initializeTargetObjectFieldChain(SpELRuntime spELRuntime, String[] spelImports, Object targetObject, Map<String, Supplier<Object>> fieldConstructionMap, String fieldChain){
        String[] fieldArray = fieldChain.split("\\.");
        if(fieldArray.length > 1){
            Class<?> objectClass = targetObject.getClass();
            String fieldExpression = "";
            for (int i = 0; i < fieldArray.length - 1; i++) {
                fieldExpression += fieldArray[i];
                Supplier constructionSuoolier = fieldConstructionMap.get(fieldExpression);
                ResolvableType fieldType = spELRuntime.getValueForType(new ParamWrapper(fieldExpression).setRootObject(objectClass).importPackage(spelImports));
                Object fieldValue = spELRuntime.getValueForType(new ParamWrapper(fieldExpression).setRootObject(targetObject).importPackage(spelImports));

                // 当属性链中的某个属性为null时需要手动构造该属性
                if(fieldValue == null){
                    // 配置中存在该属性的构造表达式时强行使用该表达式进行初始化
                    if(constructionSuoolier != null){
                        fieldValue = constructionSuoolier.get();
                        Object conversionValue = ConversionUtils.conversion(fieldValue, fieldType);
                        spELRuntime.setValue(new ParamWrapper(fieldExpression).setRootObject(targetObject).importPackage(spelImports), conversionValue);
                    }
                    // 没有构造表达式配置且又为null的非基本类型时，尝试采用无参构造器来进行初始化
                    else{
                        Class<?> fieldClass = fieldType.getRawClass();
                        if(!ClassUtils.isSimpleBaseType(fieldClass)){
                            spELRuntime.setValue(new ParamWrapper(fieldExpression).setRootObject(targetObject).importPackage(spelImports), ClassUtils.newObject(fieldClass));
                        }
                    }
                }
                fieldExpression += ".";
            }
        }
    }

    /**
     * 根据映射关系将原对象中的属性全部映射到目标对象中
     * @param targetObject 真实对象
     * @param sourceObject 原对象
     */
    private void targetMappingSetting(T targetObject, S sourceObject) {
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String targetName = entry.getKey();
            String sourceName = entry.getValue();

            ResolvableType targetType = spELRuntime.getValueForType(new ParamWrapper(targetName).setRootObject(targetClass).importPackage(getImportPackages()));
            ResolvableType sourceType = null;
            try {
                sourceType = spELRuntime.getValueForType(new ParamWrapper(sourceName).setRootObject(sourceObject.getClass()).importPackage(getImportPackages()));
            }catch (Exception ignored){

            }

            if(sourceType != null){
                Object sourceFieldValue = spELRuntime.getValueForType(new ParamWrapper(sourceName).setRootObject(sourceObject).importPackage(getImportPackages()));
                Object targetFieldValue = ConversionUtils.conversion(sourceFieldValue, targetType, useConversions);
                spELRuntime.setValue(new ParamWrapper(targetName).setRootObject(targetObject).importPackage(getImportPackages()), targetFieldValue);
            }
            else{
                Object targetFieldValue = spELRuntime.getValueForType(new ParamWrapper(sourceName).setRootObject(sourceObject).setExpectedResultType(targetType).importPackage(getImportPackages()));
                spELRuntime.setValue(new ParamWrapper(targetName).setRootObject(targetObject).importPackage(getImportPackages()), targetFieldValue);
            }
        }
    }

    @Override
    public String toString() {
        return getSignature();
    }
}
