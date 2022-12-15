package com.luckyframework.scanner;

import com.luckyframework.annotations.*;
import com.luckyframework.common.TempPair;
import com.luckyframework.common.TempTriple;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.spi.LuckyFactoryLoader;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.StringUtils;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static com.luckyframework.scanner.Constants.*;

/**
 * 提供操作扫描元素的一些基本方法
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/5 上午12:52
 */
public abstract class ScannerUtils {

    /**
     * 判断某个注解元是否被某个注解标注
     * @param annotationMetadata  注解元
     * @param annotationClassName 待判断的注解的全类名
     */
    public static boolean annotationIsExist(AnnotatedTypeMetadata annotationMetadata,
                                            String annotationClassName){
        return annotationMetadata.isAnnotated(annotationClassName);
    }

    /**
     * 判断某个注解元是否被某个注解标注
     * @param annotationMetadata  注解元
     * @param annotationClass 待判断的注解的class
     */
    public static boolean annotationIsExist(AnnotatedTypeMetadata annotationMetadata,
                                            Class<?extends Annotation> annotationClass){
        return annotationIsExist(annotationMetadata,annotationClass.getName());
    }

    /**
     * 判断某个Class是否被某个注解标注
     * @param scannerElementClass  Class
     * @param annotationClass 待判断的注解的class
     */
    public static boolean annotationIsExist(Class<?> scannerElementClass,
                                            Class<?extends Annotation> annotationClass){
        return annotationIsExist(AnnotationMetadata.introspect(scannerElementClass),annotationClass);
    }

    /**
     * 判断某个Class是否为扫描元素
     * @param beanClass 待判断的注解的class
     * @return
     */
    public static boolean isScannerElement(Class<?> beanClass){
        return annotationIsExist(beanClass,ScannerElement.class);
    }

    /**
     * 获取扫描元素的配置名称
     * @param scannerElement 扫描元素
     */
    public static String getScannerElementName(AnnotatedTypeMetadata scannerElement){
        if(scannerElement instanceof AnnotationMetadata){
            if(!annotationIsExist(scannerElement,SCANNER_ELEMENT_ANNOTATION_NAME)){
                return ((AnnotationMetadata)scannerElement).getClassName();
            }
            String scannerElementName = (String)getAnnotationAttribute(scannerElement,SCANNER_ELEMENT_ANNOTATION_NAME, VALUE);
            if(StringUtils.hasText(scannerElementName)){
                return scannerElementName;
            }
            String shortName = org.springframework.util.ClassUtils.getShortName(((AnnotationMetadata)scannerElement).getClassName());
            return Introspector.decapitalize(shortName);
        }
        //MethodMetadata的情况
        else{
            String beanAnnotationName = (String)getAnnotationAttribute(scannerElement, BEAN_ANNOTATION_NAME, VALUE);
            String shortName = org.springframework.util.ClassUtils.getShortName(((MethodMetadata)scannerElement).getMethodName());
            return StringUtils.hasText(beanAnnotationName)
                    ? beanAnnotationName
                    : Introspector.decapitalize(shortName);
        }

    }

    /**
     * 获取扫描元素的配置名称
     * @param scannerElementClass 扫描元素的Class
     */
    public static String getScannerElementName(Class<?> scannerElementClass){
        if(!AnnotationUtils.strengthenIsExist(scannerElementClass,ScannerElement.class)){
            return scannerElementClass.getName();
        }
        String scannerElementName = (String) getAnnotationAttribute(scannerElementClass, ScannerElement.class,VALUE);
        if(StringUtils.hasText(scannerElementName)){
            return scannerElementName;
        }
        String shortName = org.springframework.util.ClassUtils.getShortName(scannerElementClass);
        return Introspector.decapitalize(shortName);
    }

    /**
     * 获取扫描元素的配置名称
     * @param beanMethod BeanMethod
     */
    public static String getScannerElementName(Method beanMethod){
        if(!AnnotatedElementUtils.isAnnotated(beanMethod,Bean.class)){
            throw new RuntimeException(beanMethod+"not the bean method.");
        }

        Bean bean = AnnotatedElementUtils.getMergedAnnotation(beanMethod, Bean.class);
        return StringUtils.hasText(bean.value())
                ? bean.value()
                : Introspector.decapitalize(org.springframework.util.ClassUtils.getShortName(beanMethod.getName()));
    }


    /**
     * 获取某个扫描元素上某个注解的所有属性值
     * @param scannerElement 扫描元素
     * @param annotationClassName 目标注解的全类名
     * @return 注解所有属性值组成的Map
     */
    public static Map<String,Object> getAnnotationAttributes(AnnotatedTypeMetadata scannerElement,String annotationClassName){
        return scannerElement.getAnnotationAttributes(annotationClassName);
    }

    /**
     * 获取某个扫描元素上某个注解的某个属性值
     * @param scannerElement 扫描元素
     * @param annotationClassName 目标注解的全类名
     * @param attributeName 注解的属性
     * @return 注解属性值
     */
    public static Object getAnnotationAttribute(AnnotatedTypeMetadata scannerElement,String annotationClassName,String attributeName){
       return getAnnotationAttributes(scannerElement,annotationClassName).get(attributeName);
    }

    /**
     * 获取某个扫描元素上某个注解的某个属性值
     * @param scannerElementClass 扫描元素的Class
     * @param annotationClass  目标注解的Class
     * @param attributeName 注解的属性
     * @return 注解属性值
     */
    public static Object getAnnotationAttribute(Class<?> scannerElementClass, Class<? extends Annotation> annotationClass,String attributeName){
        Annotation mergedAnnotation = AnnotatedElementUtils.findMergedAnnotation(scannerElementClass, annotationClass);
        return org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes(mergedAnnotation).get(attributeName);
    }

    /**
     * 从扫描元素中获取{@link Condition[]}
     * 有被{@link com.luckyframework.annotations.Conditional}标注的返回其value对应的对象，
     * 否则返回{@link null}
     * @param scannerElement
     * @return
     */
    public static Condition[] getConditional(AnnotatedTypeMetadata scannerElement){
        Condition[] conditions = null;
        if(annotationIsExist(scannerElement, CONDITIONAL_ANNOTATION_NAME)){
            Class<? extends Condition>[] conditionClasses
                    = (Class<? extends Condition>[]) getAnnotationAttribute(scannerElement, CONDITIONAL_ANNOTATION_NAME, VALUE);
            conditions = conditionClassesToEntity(conditionClasses);
        }
        return conditions;
    }

    /**
     * 将Condition的Class数组转化为对应的实例对象
     * @param conditionClasses
     * @return
     */
    public static Condition[] conditionClassesToEntity(Class<? extends Condition>[] conditionClasses){
        Condition[] conditions = new Condition[conditionClasses.length];
        int i = 0;
        for (Class<? extends Condition> conditionClass : conditionClasses) {
            conditions[i++] = ClassUtils.newObject(conditionClass);
        }
        return conditions;
    }

    /**
     * 判断某个注解元素是否符合条件过滤
     * @param conditions 条件对象数组
     * @param context 条件上下文
     * @param scannerElement 注解元素
     * @return
     */
    public static boolean conditionIsMatches(Condition[] conditions, ConditionContext context,AnnotatedTypeMetadata scannerElement){
        if(conditions == null){
            return true;
        }
        for (Condition condition : conditions) {
            if(!condition.matches(context,scannerElement)){
                return false;
            }
        }
        return true;
    }

    /**
     * 获取注解元素中由{@link com.luckyframework.annotations.Import @Import}导入的
     * {@link Configuration @Configuration}、
     * {@link ImportSelector}、
     * {@link ImportBeanDefinitionRegistrar}
     * 和普通组件
     * @param annotationMetadata 当前注解元素
     * @return
     */
    public static TempTriple<Set<AnnotationMetadata>,Set<Class<? extends ImportSelector>>,Set<Class<? extends ImportBeanDefinitionRegistrar>>> getImportComponents(AnnotationMetadata annotationMetadata){
        Set<AnnotationMetadata> importComponentSet = new HashSet<>();
        Set<Class<? extends ImportSelector>> importSelectorSet = new HashSet<>();
        Set<Class<? extends ImportBeanDefinitionRegistrar>> importBeanDefinitionRegistrarSet = new HashSet<>();
        if(annotationIsExist(annotationMetadata,IMPORT_ANNOTATION_NAME)){
            Class<?> importLocationClass = ClassUtils.forName(annotationMetadata.getClassName(),ClassUtils.getDefaultClassLoader());
            Set<Import> imports = AnnotatedElementUtils.findAllMergedAnnotations(importLocationClass, Import.class);
            for (Import anImport : imports) {
                Class<?>[] importClasses = anImport.value();
                for (Class<?> importClass : importClasses) {
                    if(ImportBeanDefinitionRegistrar.class.isAssignableFrom(importClass)){
                        importBeanDefinitionRegistrarSet.add((Class<? extends ImportBeanDefinitionRegistrar>) importClass);
                    }
                    else if(ImportSelector.class.isAssignableFrom(importClass)){
                        importSelectorSet.add((Class<? extends ImportSelector>) importClass);
                    }
                    else{
                        importComponentSet.add(AnnotationMetadata.introspect(importClass));
                    }
                }
            }
        }
        return TempTriple.of(importComponentSet,importSelectorSet,importBeanDefinitionRegistrarSet);
    }

    public static TempPair<Set<String>,Set<String>> getExcludeComponent(List<AnnotationMetadata> allScannerElement, AnnotationMetadata annotationMetadata){
        Set<String> inheritedFromSet = new HashSet<>();
        Set<String> equalsSet = new HashSet<>();
        if(annotationIsExist(annotationMetadata,EXCLUDE_ANNOTATION_NAME)){
            Class<?> excludeLocationClass = ClassUtils.forName(annotationMetadata.getClassName(),ClassUtils.getDefaultClassLoader());
            Set<Exclude> excludes = AnnotatedElementUtils.findAllMergedAnnotations(excludeLocationClass, Exclude.class);
            for (Exclude anExclude : excludes) {
                Class<?>[] inheritedFromClasses = anExclude.inheritedFrom();
                for (Class<?> inheritedFromClass : inheritedFromClasses) {
                    if(ExcludeClassNames.class.isAssignableFrom(inheritedFromClass)){
                        ExcludeClassNames excludeClassNames = (ExcludeClassNames) ClassUtils.newObject(inheritedFromClass);
                        String[] classNames = excludeClassNames.excludeClassNames(Collections.unmodifiableList(allScannerElement), annotationMetadata);
                        inheritedFromSet.addAll(Arrays.asList(classNames));
                    }else{
                        inheritedFromSet.add(inheritedFromClass.getName());
                    }
                }

                Class<?>[] equalsClasses = anExclude.equals();
                for (Class<?> equalsClass : equalsClasses) {
                    if(ExcludeClassNames.class.isAssignableFrom(equalsClass)){
                        ExcludeClassNames excludeClassNames = (ExcludeClassNames) ClassUtils.newObject(equalsClass);
                        String[] classNames = excludeClassNames.excludeClassNames(Collections.unmodifiableList(allScannerElement), annotationMetadata);
                        equalsSet.addAll(Arrays.asList(classNames));
                    }else{
                        equalsSet.add(equalsClass.getName());
                    }
                }

            }
        }
        return TempPair.of(inheritedFromSet,equalsSet);
    }

    public static List<AnnotationMetadata> getAnnotationMetadataBySpi(){
        List<AnnotationMetadata> spiScannerElementList = new ArrayList<>();
        ClassLoader loader = ClassUtils.getDefaultClassLoader();
        List<String> spiClassNames = LuckyFactoryLoader.loadFactoryNames(ScannerElement.class, loader);
        for (String spiClassName : spiClassNames) {
            spiScannerElementList.add(AnnotationMetadata.introspect(ClassUtils.forName(spiClassName,loader)));
        }
        return spiScannerElementList;
    }

}
