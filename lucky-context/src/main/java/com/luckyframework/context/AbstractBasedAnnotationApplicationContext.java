package com.luckyframework.context;

import com.luckyframework.annotations.*;
import com.luckyframework.bean.factory.DefaultStandardListableBeanFactory;
import com.luckyframework.bean.factory.StandardVersatileBeanFactory;
import com.luckyframework.common.TempPair;
import com.luckyframework.common.TempTriple;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.definition.BeanDefinitionBuilder;
import com.luckyframework.environment.EnvironmentFactory;
import com.luckyframework.order.OrderRelated;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.scanner.ScanElementClassifier;
import com.luckyframework.scanner.ScannerUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static com.luckyframework.scanner.Constants.*;
import static com.luckyframework.scanner.ScannerUtils.getConditional;
import static com.luckyframework.scanner.ScannerUtils.getScannerElementName;

/**
 * 基于注解实现的ApplicationContext，抽象出了一些公共方法
 * @see SingleComponentApplicationContext
 * @see AnnotationScannerApplicationContext
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/14 下午6:17
 */
public abstract class AbstractBasedAnnotationApplicationContext extends AbstractApplicationContext {

    public AbstractBasedAnnotationApplicationContext(List<AnnotationMetadata> scannerElements){
        scannerClassifier = new ScanElementClassifier(scannerElements);
        this.scannerClassifier.importAndExcludeComponent();
    }

    public AbstractBasedAnnotationApplicationContext(@NonNull AnnotationMetadata scannerElement){
        List<AnnotationMetadata> list = new ArrayList<>();
        list.add(scannerElement);
        list.addAll(ScannerUtils.getAnnotationMetadataBySpi());
        scannerClassifier = new ScanElementClassifier(list);
        this.scannerClassifier.importAndExcludeComponent();
    }


    @Override
    public void addImportAnnotationScannerElement() {
        loadImportSelectorComponents();
    }


    @Override
    public void initEnvironment() {
        this.environment = EnvironmentFactory.defaultEnvironment();
    }

    @Override
    public void initBeanFactory() {
        DefaultStandardListableBeanFactory factory = new DefaultStandardListableBeanFactory(environment);
        beanFactory = new StandardVersatileBeanFactory(factory);
    }

    @Override
    public void loadBeanDefinition() {
        //注册所有@Component组件的bean定义信息
        scannerClassifier.getComponents().forEach(c->{
            TempPair<String, BeanDefinition> pair = createComponentBeanDefinition(c);
            registerBeanDefinition(pair.getOne(),pair.getTwo());
        });

        ////注册所有@Plugins插件的bean定义信息
        scannerClassifier.getPlugins().forEach(p->{
            registerPlugin(getScannerElementName(p),p);
        });

        //注册所有@Configuration @Bean组件的bean定义信息
        scannerClassifier.getConfigurations().forEach(conf->{
            List<TempPair<String, BeanDefinition>> configurationBeanDefinitions = createConfigurationBeanDefinitions(conf);
            for (TempPair<String, BeanDefinition> tempPair : configurationBeanDefinitions) {
                registerBeanDefinition(tempPair.getOne(),tempPair.getTwo());
            }
        });

        //注册所有有@Import注解导入的bean定义信息
        List<AnnotationMetadata> importAnnotationMetadataSet = this.scannerClassifier.getScannerElementByAnnotation(IMPORT_ANNOTATION_NAME);

        // 1.收集@Import注解中的ImportBeanDefinitionRegistrar
        List<TempPair<ImportBeanDefinitionRegistrar,AnnotationMetadata>> importBeanDefinitionRegistrarList = new ArrayList<>();
        for (AnnotationMetadata importMetadata : importAnnotationMetadataSet) {
            TempTriple<Set<AnnotationMetadata>, Set<Class<? extends ImportSelector>>, Set<Class<? extends ImportBeanDefinitionRegistrar>>>
                    triple = ScannerUtils.getImportComponents(importMetadata);
            Set<Class<? extends ImportBeanDefinitionRegistrar>> ImportBeanDefinitionRegistrarClassSet = triple.getThree();
            for (Class<? extends ImportBeanDefinitionRegistrar> aClass : ImportBeanDefinitionRegistrarClassSet) {
                ImportBeanDefinitionRegistrar importBeanDefinitionRegistrar = ClassUtils.newObject(aClass);
                importBeanDefinitionRegistrarList.add(TempPair.of(importBeanDefinitionRegistrar,importMetadata));
            }
        }

        // 2.排序
        importBeanDefinitionRegistrarList.sort(new OrderRelated.TempPairOrderComparator<>());

        // 3.反射实例化ImportBeanDefinitionRegistrar对象，执行ImportBeanDefinitionRegistrar的registerBeanDefinitions方法
        for (TempPair<ImportBeanDefinitionRegistrar, AnnotationMetadata> pair : importBeanDefinitionRegistrarList) {
            ImportBeanDefinitionRegistrar importBeanDefinitionRegistrar = pair.getOne();
            invokeAwareMethod(importBeanDefinitionRegistrar);
            importBeanDefinitionRegistrar.registerBeanDefinitions(pair.getTwo(),this);
        }
    }

    /**
     * 加载所有由{@link com.luckyframework.annotations.Import @Import}注解导入的
     * {@link ImportSelector}实现类返回的组件
     */
    protected void loadImportSelectorComponents() {
        List<AnnotationMetadata> importMetadataSet = this.scannerClassifier.getScannerElementByAnnotation(IMPORT_ANNOTATION_NAME);
        Set<Class<?>> importClasses = new HashSet<>();
        List<TempPair<ImportSelector,AnnotationMetadata>> importSelectorList = new ArrayList<>();

        // 收集所有有@Import注解导入的ImportSelector组件
        for (AnnotationMetadata metadata : importMetadataSet) {
            TempTriple<Set<AnnotationMetadata>, Set<Class<? extends ImportSelector>>, Set<Class<? extends ImportBeanDefinitionRegistrar>>>
                    triple = ScannerUtils.getImportComponents(metadata);
            Set<Class<? extends ImportSelector>> importSelectorClasses = triple.getTwo();
            for (Class<? extends ImportSelector> importSelectorClass : importSelectorClasses) {
                ImportSelector importSelector = ClassUtils.newObject(importSelectorClass);
                importSelectorList.add(TempPair.of(importSelector,metadata));
            }
        }

        // 排序
        importSelectorList.sort(new OrderRelated.TempPairOrderComparator<>());

        //反射实例化所ImportSelector，并调用selectImports方法，将此方法返回的组件加入到扫描元素中
        for (TempPair<ImportSelector, AnnotationMetadata> pair : importSelectorList) {
            ImportSelector importSelector = pair.getOne();
            invokeAwareMethod(pair.getOne());
            Predicate<String> filter = importSelector.getExclusionFilter();
            String[] classNames = importSelector.selectImports(pair.getTwo());
            for (String className : classNames) {
                if(filter != null && !filter.test(className)){
                    continue;
                }
                importClasses.add(ClassUtils.forName(className,ClassUtils.getDefaultClassLoader()));
            }
        }
        this.scannerClassifier.addScannerElementClasses(importClasses);
    }

    /**
     * 创建使用{@link com.luckyframework.annotations.Configuration}注解标注的所有Bean的定义信息
     * @param configuration 被{@link com.luckyframework.annotations.Configuration}注解所标注的元数据
     */
    public List<TempPair<String, BeanDefinition>> createConfigurationBeanDefinitions(AnnotationMetadata configuration){
        List<TempPair<String, BeanDefinition>> beanDefinitions = new ArrayList<>();
        Class<?> configurationClass = ClassUtils.forName(configuration.getClassName(),ClassUtils.getDefaultClassLoader());
        boolean configClassIsLazy = AnnotatedElementUtils.isAnnotated(configurationClass, LAZY_ANNOTATION_NAME) && AnnotatedElementUtils.findMergedAnnotation(configurationClass, Lazy.class).value();
        List<Method> factoryMethods = ClassUtils.getMethodByStrengthenAnnotation(configurationClass, Bean.class);
        for (Method factoryMethod : factoryMethods) {
            StandardMethodMetadata standardMethodMetadata = new StandardMethodMetadata(factoryMethod);
            Condition[] conditional = getConditional(standardMethodMetadata);
            if(!ScannerUtils.conditionIsMatches(conditional,conditionContext,standardMethodMetadata)){
                continue;
            }
            Bean bean = AnnotationUtils.get(factoryMethod,Bean.class);
            BeanDefinition methodDefinition;
            if(ClassUtils.isStaticMethod(factoryMethod)){
                methodDefinition = BeanDefinitionBuilder.builderByStaticFactoryMethod(configurationClass,factoryMethod);
            }else{
                String configurationBeanName = getScannerElementName(configuration);
                methodDefinition = BeanDefinitionBuilder.builderByFactoryMethod(configurationBeanName,factoryMethod);
                methodDefinition.setDependsOn(new String[]{configurationBeanName});
            }
            if(!AnnotatedElementUtils.isAnnotated(factoryMethod,LAZY_ANNOTATION_NAME)){
                methodDefinition.setLazyInit(configClassIsLazy);
            }
            String[] initMethodNames = (String[]) AnnotationUtils.getValue(bean, INIT_METHOD);
            String[] destroyMethodNames = (String[]) AnnotationUtils.getValue(bean, DESTROY_METHOD);
            boolean autowireCandidate = (boolean) AnnotationUtils.getValue(bean, "autowireCandidate");
            methodDefinition.setInitMethodNames(initMethodNames);
            methodDefinition.setDestroyMethodNames(destroyMethodNames);
            methodDefinition.setAutowireCandidate(autowireCandidate);
            methodDefinition.setRole(BeanDefinition.CONFIG_METHOD_BEAN);
            beanDefinitions.add(TempPair.of(getScannerElementName(standardMethodMetadata),methodDefinition));
        }
        return beanDefinitions;
    }

    /**
     * 创建使用{@link com.luckyframework.annotations.Component}注解标注的所有Bean的定义信息
     * @param component 被{@link com.luckyframework.annotations.Component}注解所标注的元数据
     */
    public TempPair<String, BeanDefinition> createComponentBeanDefinition(AnnotationMetadata component){
        BeanDefinition componentDefinition =BeanDefinitionBuilder.builderByConstructor(component.getClassName());
        componentDefinition.setRole(BeanDefinition.SCANNER_BEAN);
        if(component.isAnnotated(CONFIGURATION_ELEMENT_ANNOTATION_NAME)){
            componentDefinition.setRole(BeanDefinition.CONFIGURATION_BEAN);
        }
        return TempPair.of(getScannerElementName(component), componentDefinition);
    }


}
