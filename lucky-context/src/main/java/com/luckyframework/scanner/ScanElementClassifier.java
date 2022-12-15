package com.luckyframework.scanner;

import com.luckyframework.annotations.*;
import com.luckyframework.common.TempPair;
import com.luckyframework.common.TempTriple;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.luckyframework.scanner.Constants.*;

/**
 * 扫描元素分类器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/5 上午12:22
 */
public class ScanElementClassifier {

    /** 所有的扫描元素*/
    private final List<AnnotationMetadata> scannerElements;
    /** 按注解区分的扫描元素集*/
    private final Map<String,List<AnnotationMetadata>> cacheAnnotationScannerElementMap = new ConcurrentHashMap<>();
    /** 所有组件元素*/
    private List<AnnotationMetadata> componentList;
    /** 所有插件元素*/
    private List<AnnotationMetadata> pluginList;
    /**
     * 所有没有被{@link com.luckyframework.annotations.ScannerElement @ScannerElement} 标注的扫描元素
     */
    private List<AnnotationMetadata> nonScannerElements;


    public ScanElementClassifier(Class<?>... rootClass) {
        this(new ComponentAutoScanner(rootClass));
    }

    public ScanElementClassifier(String... basePackage) {
        this(new ComponentAutoScanner(basePackage));
    }

    public ScanElementClassifier(Class<?> rootClass) {
        this(new ComponentAutoScanner(rootClass));
    }

    public ScanElementClassifier(String basePackage) {
        this(new ComponentAutoScanner(basePackage));
    }

    public ScanElementClassifier(Scanner scanner) {
        this(scanner.getScannerElements());
    }

    public ScanElementClassifier(List<AnnotationMetadata> scannerElements) {
        this.scannerElements = scannerElements;

    }

    /**
     * 添加一个扫描元素
     * @param scannerElement 扫描元素
     */
    public void addScannerElement(AnnotationMetadata scannerElement){
        scannerElements.add(scannerElement);
    }

    /**
     * 添加一个扫描元素
     * @param scannerElementClass 扫描元素的Class
     */
    public void addScannerElement(Class<?> scannerElementClass){
        addScannerElement(AnnotationMetadata.introspect(scannerElementClass));
    }

    /**
     * 添加一组扫描元素
     * @param scannerElements 扫描元素集合
     */
    public void addScannerElements(Collection<AnnotationMetadata> scannerElements){
        for (AnnotationMetadata scannerElement : scannerElements) {
            addScannerElement(scannerElement);
        }
    }

    /**
     * 添加一组扫描元素
     * @param scannerElementClasses 扫描元素的Class的集合
     */
    public void addScannerElementClasses(Collection<Class<?>> scannerElementClasses){
        for (Class<?> scannerElementClass : scannerElementClasses) {
            addScannerElement(scannerElementClass);
        }
    }


    /**
     * 条件过滤
     * 过滤掉那些被{@link Conditional @Conditional}标注，
     * 但是其中{@link Condition#matches(ConditionContext, AnnotatedTypeMetadata)}返回为false的组件
     * @param conditionContext 条件上下文
     */
    public void conditionFilter(ConditionContext conditionContext){
        scannerElements.removeIf(annotationMetadata -> !conditionJudge(conditionContext, annotationMetadata));
    }

    /**
     * 条件判断
     * @param conditionContext      条件上下文
     * @param annotationMetadata    注解元
     * @return 是否能通过条件判断
     */
    public boolean conditionJudge(ConditionContext conditionContext,AnnotationMetadata annotationMetadata){

        //该注解元素为内部类，先判断他所在的外部类是否符合条件，如果外部类不符合条件直接移除，如果外部类符合条件
        //再判断该内部类是否符合条件
        if(annotationMetadata.getEnclosingClassName() != null){
            String enclosingClassName = annotationMetadata.getEnclosingClassName();
            Class<?> enclosingClass = ClassUtils.forName(enclosingClassName,ClassUtils.getDefaultClassLoader());
            AnnotationMetadata enclosingClassMetadata = AnnotationMetadata.introspect(enclosingClass);
            Condition[] enclosingClassConditions = ScannerUtils.getConditional(enclosingClassMetadata);
            if(!ScannerUtils.conditionIsMatches(enclosingClassConditions,conditionContext,enclosingClassMetadata)){
                return false;
            }
        }

        //判断本注解元素是否符合条件，不符合的直接移除
        Condition[] conditions = ScannerUtils.getConditional(annotationMetadata);
        return ScannerUtils.conditionIsMatches(conditions, conditionContext, annotationMetadata);
    }

    /**
     * 加载由{@link Import  @Import}导入的组件
     * 排除由{@link Exclude @Exclude}排除的组件
     */
    public void importAndExcludeComponent(){
        loadImportComponents();
        removeExcludeComponent();
    }

    /**
     * 加载由{@link com.luckyframework.annotations.Import @Import}导入的
     * {@link Configuration @Configuration}组件和普通组件
     */
    public void loadImportComponents(){
        List<AnnotationMetadata> importAnnotationScannerElementList = getScannerElementByAnnotation(IMPORT_ANNOTATION_NAME);
        for (AnnotationMetadata annotationMetadata : importAnnotationScannerElementList) {
            TempTriple<Set<AnnotationMetadata>, Set<Class<? extends ImportSelector>>, Set<Class<? extends ImportBeanDefinitionRegistrar>>>
                    triple = ScannerUtils.getImportComponents(annotationMetadata);
            scannerElements.addAll(triple.getOne());
        }
    }

    public void removeExcludeComponent(){
        List<AnnotationMetadata> excludeAnnotationScannerElementList = getScannerElementByAnnotation(EXCLUDE_ANNOTATION_NAME);
        Set<String> inheritedFromSet = new HashSet<>();
        Set<String> equalsSet = new HashSet<>();
        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
        for (AnnotationMetadata metadata : excludeAnnotationScannerElementList) {
            TempPair<Set<String>, Set<String>> pair = ScannerUtils.getExcludeComponent(scannerElements, metadata);
            inheritedFromSet.addAll(pair.getOne());
            equalsSet.addAll(pair.getTwo());
        }
        scannerElements.removeIf((metadata)->{
            if(equalsSet.contains(metadata.getClassName())){
                return true;
            }
            Class<?> metadataClass = ClassUtils.forName(metadata.getClassName(), classLoader);
            for (String inheritedFromClassStr : inheritedFromSet) {
                Class<?> configClass = ClassUtils.forName(inheritedFromClassStr, classLoader);
               if(configClass.isAssignableFrom(metadataClass)){
                   return true;
               }
            }
            return false;
        });
    }

    /**
     * 获取扫描元素中没有被{@link com.luckyframework.annotations.ScannerElement @ScannerElement}标注的扫描元素
     * 这些组件将以该class的全类名作为bean的名称
     */
    public List<AnnotationMetadata> getNonScannerElements() {
        if(nonScannerElements == null){
            nonScannerElements = new ArrayList<>();
            for (AnnotationMetadata scannerElement : scannerElements) {
                if(!ScannerUtils.annotationIsExist(scannerElement,SCANNER_ELEMENT_ANNOTATION_NAME)){
                    nonScannerElements.add(scannerElement);
                }
            }
        }
        return nonScannerElements;
    }

    /**
     * 获取所有普通组件
     * 包含:
     * {@link com.luckyframework.annotations.Component @Component}，
     * {@link com.luckyframework.annotations.Service @Service}，
     * {@link com.luckyframework.annotations.Repository @Repository}，
     * {@link com.luckyframework.annotations.Configuration @Configuration}，
     * {@link com.luckyframework.annotations.Controller @Controller}，
     * 和没有被{@link com.luckyframework.annotations.ScannerElement @ScannerElement} 标注的扫描元素
     */
    public List<AnnotationMetadata> getComponents() {
        if(componentList == null){
            componentList = new ArrayList<>();
            componentList.addAll(
                    getScannerElementByAnnotation(COMPONENT_ANNOTATION_NAME)
                            .stream()
                            .filter(cm->!cm.isAbstract() && !cm.isInterface())
                            .collect(Collectors.toList())
            );

            componentList.addAll(
                    getNonScannerElements()
                            .stream()
                            .filter(cm->!cm.isAbstract() && !cm.isInterface())
                            .collect(Collectors.toList()));
        }
        return componentList;
    }

    /**
     * 获取所有插件
     * 包含：
     * {@link com.luckyframework.annotations.Plugin @Plugin}
     */
    public List<AnnotationMetadata> getPlugins() {
       if(pluginList == null){
           pluginList = new ArrayList<>();
           for (AnnotationMetadata scannerElement : scannerElements) {
               if(ScannerUtils.annotationIsExist(scannerElement,PLUGIN_ELEMENT_ANNOTATION_NAME) ||
                       scannerElement.isAbstract() || scannerElement.isInterface()){
                   pluginList.add(scannerElement);
               }
           }
       }
        return pluginList;
    }

    /**
     * 获取所有Web层组件
     * {@link com.luckyframework.annotations.Controller @Controller}
     */
    public List<AnnotationMetadata> getControllers() {
        return getScannerElementByAnnotation(CONTROLLER_ELEMENT_ANNOTATION_NAME)
                .stream()
                .filter(cm->!cm.isAbstract() && !cm.isInterface())
                .collect(Collectors.toList());
    }

    /**
     * 获取所有服务层组件
     * 包含：
     * {@link com.luckyframework.annotations.Service @Service}
     */
    public List<AnnotationMetadata> getServices() {
        return getScannerElementByAnnotation(SERVICE_ANNOTATION_NAME)
                .stream()
                .filter(cm->!cm.isAbstract() && !cm.isInterface())
                .collect(Collectors.toList());
    }

    /**
     * 获取所有数据层组件
     * {@link com.luckyframework.annotations.Repository @Repository}
     */
    public List<AnnotationMetadata> getRepositories() {
        return getScannerElementByAnnotation(REPOSITORY_ELEMENT_ANNOTATION_NAME).stream()
                .filter(cm->!cm.isAbstract() && !cm.isInterface())
                .collect(Collectors.toList());
    }

    /**
     * 获取所有配置类组件
     * {@link com.luckyframework.annotations.Configuration @Configuration}
     */
    public List<AnnotationMetadata> getConfigurations() {
        return getScannerElementByAnnotation(CONFIGURATION_ELEMENT_ANNOTATION_NAME).stream()
                .filter(cm->!cm.isAbstract() && !cm.isInterface())
                .collect(Collectors.toList());
    }


    /**
     * 获取被某一个注解标注的组件集合
     * @param annotationClass 目标注解的Class
     * @return 被该注解标注的所有组件集合
     */
    public List<AnnotationMetadata> getScannerElementByAnnotation(Class<? extends Annotation> annotationClass){
        return getScannerElementByAnnotation(annotationClass.getName());
    }

    /**
     * 获取被某一个注解标注的组件集合
     * @param annotationClassName 目标注解的全类名
     * @return 被该注解标注的所有组件集合
     */
    public List<AnnotationMetadata> getScannerElementByAnnotation(String annotationClassName){
        List<AnnotationMetadata> annotationMetadataSet = cacheAnnotationScannerElementMap.get(annotationClassName);
        if(annotationMetadataSet == null){
            annotationMetadataSet = new ArrayList<>();
            for (AnnotationMetadata scannerElement : scannerElements) {
                if(ScannerUtils.annotationIsExist(scannerElement,annotationClassName)){
                    annotationMetadataSet.add(scannerElement);
                }
            }
            cacheAnnotationScannerElementMap.put(annotationClassName,annotationMetadataSet);
        }
        return annotationMetadataSet;
    }

}
