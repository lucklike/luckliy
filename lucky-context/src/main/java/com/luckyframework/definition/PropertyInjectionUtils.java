package com.luckyframework.definition;

import com.luckyframework.annotations.*;
import com.luckyframework.bean.factory.BeanReference;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.context.ApplicationContext;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.environment.LuckyStandardEnvironment;
import com.luckyframework.exception.BeansException;
import com.luckyframework.exception.NoSuchBeanDefinitionException;
import com.luckyframework.exception.PropertyValueInjectorException;
import com.luckyframework.expression.StandardBeanExpressionResolver;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 属性注入相关的工具类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/1/28 15:40
 */
public abstract class PropertyInjectionUtils {

    /**
     * 解析得到某个Class中所有需要注入的属性，并将这些属性封装成{@link PropertyValue PropertyValue[]}对象
     *
     * @param beanClass 需要解析的Class
     * @return 需要注入的属性数组
     */
    public static PropertyValue[] getInjectionPropertyValues(Class<?> beanClass) {
        return new PropertyValuesGenerator(getInjectionField(beanClass)).getPropertyValues();
    }

    /**
     * 解析得到某个Class中所有需要执行参数注入的方法，并将这些属性封装成{@link SetterValue SetterValue[]}对象
     *
     * @param beanClass 需要解析的Class
     * @return 需要执行参数注入的方法数组
     */
    public static SetterValue[] getInjectionSetterValues(Class<?> beanClass) {
        return new SetterValuesGenerator(getInjectionMethod(beanClass)).getSetterValues();
    }

    /**
     * 使用反射的方式为某个对象注入属性
     *
     * @param beanName bean的名称
     * @param bean     bean实例
     * @param context  上下文对象
     * @param env      环境变量
     */
    public static void injectionBeanField(String beanName, Object bean, ApplicationContext context, Environment env) {
        new BeanFieldInjection(beanName, bean, context, env).injectionFields();
    }

    /**
     * 使用反射的方式为某个对象的方法注入参数
     *
     * @param beanName bean的名称
     * @param bean     bean实例
     * @param context  上下文对象
     * @param env      环境变量
     */
    public static void injectionBeanMethodParameter(String beanName, Object bean, ApplicationContext context, Environment env) {
        new BeanMethodParameterInjection(beanName, bean, context, env).injectionMethodParameter();
    }

    /**
     * 解析得到某个Class中所有需要注入的属性的分组集合
     *
     * @param beanClass 需要解析的Class
     * @return 某个Class中所有需要注入的属性的分组集合
     */
    public static InjectionElement<Field> getInjectionField(Class<?> beanClass) {
        return new InjectionElement<>(beanClass, ClassUtils::getFieldByAnnotation);
    }

    /**
     * 解析得到某个Class中所有需要执行参数注入的方法的分组集合
     *
     * @param beanClass 需要解析的Class
     * @return 某个Class中所有需要执行参数注入的方法的分组集合
     */
    public static InjectionElement<Method> getInjectionMethod(Class<?> beanClass) {
        return new InjectionElement<>(beanClass, ClassUtils::getMethodByAnnotation);
    }


    /**
     * Value注解表达式解析
     * 表达式会被当做SpEL表达式进行一次解析，如果解析结果为String类型，还会进行一次环境变量解析
     *
     * @param exp             SpEL表达式解析器
     * @param environment     环境变量
     * @param valueExpression value表达式
     * @param type            最终转换类型
     * @return 解析结果
     */
    private static Object valueExpressionParsing(StandardBeanExpressionResolver exp, LuckyStandardEnvironment environment, String valueExpression, ResolvableType type) {
        Object value = exp.evaluate(valueExpression);
        if (value instanceof String) {
            value = environment.resolveRequiredPlaceholdersForObject(value);
        }
        return ConversionUtils.conversion(value, type);
    }

    /**
     * 注入元素，可能是一系列的{@code Field}集合也有可能是一系列的{@code Method}集合
     *
     * @param <T> 元素类型
     */
    static class InjectionElement<T extends AnnotatedElement> {

        /** Bean的Class*/
        private final Class<?> beanClass;

        /** 被{@link Resource @Resource}注解标注的注解元素*/
        private final List<T> resourcesElements;
        /** 被{@link Qualifier @Qualifier}注解标注的注解元素*/
        private final List<T> qualifierElements;
        /** 被{@link Autowired @Autowired}注解标注的注解元素*/
        private final List<T> autowiredElements;
        /** 被{@link BeanNameCollector @BeanNameCollector}注解标注的注解元素*/
        private final List<T> beanNameCollectorElements;
        /** 被{@link BeanCollector @BeanCollector}注解标注的注解元素*/
        private final List<T> beanCollectorElements;
        /** 被{@link Value @Value}注解标注的注解元素*/
        private final List<T> valueElements;


        public InjectionElement(Class<?> beanClass, AnnotatedElementGetter<T> getter) {
            this.beanClass = beanClass;
            resourcesElements = getter.getElements(beanClass, Resource.class);

            qualifierElements = getter.getElements(beanClass, Qualifier.class);
            qualifierElements.removeIf(resourcesElements::contains);

            autowiredElements = getter.getElements(beanClass, Autowired.class);
            autowiredElements.removeIf(e -> resourcesElements.contains(e) || qualifierElements.contains(e));

            beanNameCollectorElements = getter.getElements(beanClass, BeanNameCollector.class);
            beanNameCollectorElements.removeIf(e -> resourcesElements.contains(e) || qualifierElements.contains(e) || autowiredElements.contains(e));

            beanCollectorElements = getter.getElements(beanClass, BeanCollector.class);
            beanCollectorElements.removeIf(e -> resourcesElements.contains(e) || qualifierElements.contains(e) || autowiredElements.contains(e) || beanNameCollectorElements.contains(e));

            valueElements = getter.getElements(beanClass, Value.class);
            valueElements.removeIf(e -> resourcesElements.contains(e) || qualifierElements.contains(e) || autowiredElements.contains(e) || beanNameCollectorElements.contains(e) || beanCollectorElements.contains(e));
        }

        public List<T> getResourcesElements() {
            return resourcesElements;
        }

        public List<T> getQualifierElements() {
            return qualifierElements;
        }

        public List<T> getAutowiredElements() {
            return autowiredElements;
        }

        public List<T> getBeanNameCollectorElements() {
            return beanNameCollectorElements;
        }

        public List<T> getBeanCollectorElements() {
            return beanCollectorElements;
        }

        public List<T> getValueElements() {
            return valueElements;
        }

        public Class<?> getBeanClass() {
            return beanClass;
        }

        public int getElementCount() {
            return resourcesElements.size() + qualifierElements.size() + autowiredElements.size() + beanNameCollectorElements.size() + beanCollectorElements.size() + valueElements.size();
        }

    }

    /**
     * {@link PropertyValue}对象生成器
     */
    static class PropertyValuesGenerator {
        private final InjectionElement<Field> fieldElement;

        PropertyValuesGenerator(InjectionElement<Field> fieldElement) {
            this.fieldElement = fieldElement;
        }

        public PropertyValue[] getPropertyValues() {
            List<PropertyValue> propertyValues = new ArrayList<>(fieldElement.getElementCount());
            propertyValues.addAll(createResourcePropertyValue());
            propertyValues.addAll(createQualifierPropertyValue());
            propertyValues.addAll(createAutowiredPropertyValue());
            propertyValues.addAll(createBeanNameCollectorPropertyValue());
            propertyValues.addAll(createBeanCollectorPropertyValue());
            propertyValues.addAll(createValuePropertyValue());
            return propertyValues.toArray(new PropertyValue[0]);
        }

        public List<PropertyValue> createResourcePropertyValue() {
            List<PropertyValue> propertyValues = new ArrayList<>();
            for (Field field : fieldElement.getResourcesElements()) {
                String fieldName = field.getName();
                Resource resource = AnnotationUtils.get(field, Resource.class);
                BeanReference beanReference = StringUtils.hasText(resource.name())
                        ? BeanReference.builderName(resource.name(), true)
                        : BeanReference.builderAutoNameFirst(fieldName, ResolvableType.forField(field, fieldElement.getBeanClass()), true);
                BeanReferenceUtils.setLazyProperty(field, beanReference);
                propertyValues.add(new PropertyValue(fieldName, beanReference, field));
            }
            return propertyValues;
        }

        public List<PropertyValue> createQualifierPropertyValue() {
            List<PropertyValue> propertyValues = new ArrayList<>();
            for (Field field : fieldElement.getQualifierElements()) {
                String fieldName = field.getName();
                Resource resource = AnnotationUtils.get(field, Resource.class);
                BeanReference beanReference = StringUtils.hasText(resource.name())
                        ? BeanReference.builderName(resource.name(), true)
                        : BeanReference.builderAutoNameFirst(fieldName, ResolvableType.forField(field, fieldElement.getBeanClass()), true);
                BeanReferenceUtils.setLazyProperty(field, beanReference);
                propertyValues.add(new PropertyValue(fieldName, beanReference, field));
            }
            return propertyValues;
        }

        public List<PropertyValue> createAutowiredPropertyValue() {
            List<PropertyValue> propertyValues = new ArrayList<>();
            for (Field field : fieldElement.getAutowiredElements()) {
                String fieldName = field.getName();
                Autowired autowired = AnnotationUtils.get(field, Autowired.class);
                BeanReference beanReference = BeanReference.builderAutoTypeFirst(fieldName, ResolvableType.forField(field, fieldElement.getBeanClass()), autowired.required());
                BeanReferenceUtils.setLazyProperty(field, beanReference);
                propertyValues.add(new PropertyValue(fieldName, beanReference, field));
            }
            return propertyValues;
        }

        public List<PropertyValue> createBeanNameCollectorPropertyValue() {
            List<PropertyValue> propertyValues = new ArrayList<>();
            for (Field field : fieldElement.getBeanNameCollectorElements()) {
                BeanNameCollector nameCollector = AnnotationUtils.get(field, BeanNameCollector.class);
                BeanReference beanReference = BeanReference.buildBeanNameCollector(ResolvableType.forField(field),
                        ResolvableType.forRawClass(nameCollector.value()),
                        nameCollector.exclude(),
                        nameCollector.required());
                BeanReferenceUtils.setLazyProperty(field, beanReference);
                propertyValues.add(new PropertyValue(field.getName(), beanReference, field));
            }
            return propertyValues;
        }

        public List<PropertyValue> createBeanCollectorPropertyValue() {
            List<PropertyValue> propertyValues = new ArrayList<>();
            for (Field field : fieldElement.getBeanCollectorElements()) {
                BeanCollector nameCollector = AnnotationUtils.get(field, BeanCollector.class);
                BeanReference beanReference = BeanReference.buildBeanInstanceCollector(ResolvableType.forField(field),
                        nameCollector.specify(),
                        nameCollector.exclude(),
                        nameCollector.required());
                BeanReferenceUtils.setLazyProperty(field, beanReference);
                propertyValues.add(new PropertyValue(field.getName(), beanReference, field));
            }
            return propertyValues;
        }

        public List<PropertyValue> createValuePropertyValue() {
            List<PropertyValue> propertyValues = new ArrayList<>();
            for (Field field : fieldElement.getValueElements()) {
                Value value = AnnotationUtils.get(field, Value.class);
                BeanReference beanReference = BeanReference.builderValue(value.value(), ResolvableType.forField(field, fieldElement.getBeanClass()));
                BeanReferenceUtils.setLazyProperty(field, beanReference);
                propertyValues.add(new PropertyValue(field.getName(), beanReference, field));
            }
            return propertyValues;
        }

    }

    /**
     * {@link SetterValue}对象生成器
     */
    @SuppressWarnings("all")
    static class SetterValuesGenerator {

        private final InjectionElement<Method> methodElement;
        private final LocalVariableTableParameterNameDiscoverer paramTables = new LocalVariableTableParameterNameDiscoverer();

        SetterValuesGenerator(InjectionElement<Method> methodElement) {
            this.methodElement = methodElement;
        }

        public SetterValue[] getSetterValues() {
            List<SetterValue> propertyValues = new ArrayList<>(methodElement.getElementCount());
            propertyValues.addAll(createResourceSetterValue());
            propertyValues.addAll(createQualifierSetterValue());
            propertyValues.addAll(createAutowiredSetterValue());
            propertyValues.addAll(createBeanNameCollectorSetterValue());
            propertyValues.addAll(createBeanCollectorSetterValue());
            propertyValues.addAll(createValueSetterValue());
            return propertyValues.toArray(new SetterValue[0]);
        }

        public List<SetterValue> createResourceSetterValue() {
            for (Method method : methodElement.getResourcesElements()) {
                if (method.getParameters().length != 1) {
                    throw new IllegalStateException("@Resource annotation requires a single-arg method: " + method);
                }
            }
            List<SetterValue> setterValues = new ArrayList<>();
            for (Method method : methodElement.getResourcesElements()) {
                Resource resourceQualifier = AnnotationUtils.get(method, Resource.class);
                String resourceName = resourceQualifier.name();
                String parameterName = Objects.requireNonNull(paramTables.getParameterNames(method))[0];
                Parameter parameter = method.getParameters()[0];
                ResolvableType paramType = ResolvableType.forType(method.getGenericParameterTypes()[0]);
                Object[] args = new Object[1];
                BeanReference beanReference = StringUtils.hasText(resourceName)
                        ? BeanReference.builderName(resourceName, true)
                        : BeanReference.builderAutoNameFirst(parameterName, paramType, true);
                BeanReferenceUtils.setLazyProperty(method, parameter, beanReference);
                args[0] = beanReference;
                setterValues.add(new SetterValue(method.getName(), method.getParameterTypes(), args, method));
            }
            return setterValues;
        }

        public List<SetterValue> createQualifierSetterValue() {
            for (Method method : methodElement.getQualifierElements()) {
                if (method.getParameters().length != 1) {
                    throw new IllegalStateException("@Qualifier annotation requires a single-arg method: " + method);
                }
            }
            List<SetterValue> setterValues = new ArrayList<>();
            for (Method method : methodElement.getQualifierElements()) {
                Qualifier qualifier = AnnotationUtils.get(method, Qualifier.class);
                String qualifierValue = qualifier.value();
                String parameterName = Objects.requireNonNull(paramTables.getParameterNames(method))[0];
                Parameter parameter = method.getParameters()[0];
                ResolvableType paramType = ResolvableType.forType(method.getGenericParameterTypes()[0]);
                Object[] args = new Object[1];
                BeanReference beanReference = StringUtils.hasText(qualifierValue)
                        ? BeanReference.builderName(qualifierValue, qualifier.required())
                        : BeanReference.builderAutoNameFirst(parameterName, paramType, qualifier.required());
                BeanReferenceUtils.setLazyProperty(method, parameter, beanReference);
                args[0] = beanReference;
                setterValues.add(new SetterValue(method.getName(), method.getParameterTypes(), args, method));
            }
            return setterValues;
        }

        public List<SetterValue> createAutowiredSetterValue() {
            List<SetterValue> setterValues = new ArrayList<>();
            for (Method method : methodElement.getAutowiredElements()) {
                Parameter[] parameters = method.getParameters();
                BeanReference[] beanReferences = null;
                if (parameters != null) {

                    beanReferences = new BeanReference[parameters.length];
                    String[] parameterNames = paramTables.getParameterNames(method);
                    Type[] genericParameterTypes = method.getGenericParameterTypes();
                    boolean methodRequired = method.getAnnotation(Autowired.class).required();

                    for (int j = 0; j < parameters.length; j++) {
                        Parameter parameter = parameters[j];
                        ResolvableType parameterType = ResolvableType.forType(genericParameterTypes[j]);
                        String parameterName = parameterNames[j];

                        // @Resource
                        if (parameter.isAnnotationPresent(Resource.class)) {
                            Resource resource = parameter.getAnnotation(Resource.class);
                            beanReferences[j] = StringUtils.hasText(resource.name())
                                    ? BeanReference.builderName(resource.name(), true)
                                    : BeanReference.builderAutoNameFirst(parameterName, parameterType, true);
                        }
                        // @Qualifier
                        else if (parameter.isAnnotationPresent(Qualifier.class)) {
                            Qualifier qualifier = parameter.getAnnotation(Qualifier.class);
                            beanReferences[j] = StringUtils.hasText(qualifier.value())
                                    ? BeanReference.builderName(qualifier.value(), qualifier.required())
                                    : BeanReference.builderAutoNameFirst(parameterName, parameterType, qualifier.required());
                        }
                        // @Autowired
                        else if (parameter.isAnnotationPresent(Autowired.class)) {
                            beanReferences[j] = BeanReference.builderAutoTypeFirst(parameterName, parameterType, parameter.getAnnotation(Autowired.class).required());
                        }
                        // @BeanNameCollector
                        else if (parameter.isAnnotationPresent(BeanNameCollector.class)) {
                            BeanNameCollector annotation = parameter.getAnnotation(BeanNameCollector.class);
                            beanReferences[j] = BeanReference.buildBeanNameCollector(parameterType, ResolvableType.forRawClass(annotation.value()), annotation.exclude(), annotation.required());
                        }
                        // @BeanCollector
                        else if (parameter.isAnnotationPresent(BeanCollector.class)) {
                            BeanCollector annotation = parameter.getAnnotation(BeanCollector.class);
                            beanReferences[j] = BeanReference.buildBeanInstanceCollector(parameterType, annotation.specify(), annotation.exclude(), annotation.required());

                        }
                        // @Value
                        else if (parameter.isAnnotationPresent(Value.class)) {
                            Value value = parameter.getAnnotation(Value.class);
                            beanReferences[j] = BeanReference.builderValue(value.value(), parameterType);
                        }
                        // 参数无注解
                        else {
                            beanReferences[j] = BeanReference.builderAutoTypeFirst(parameterName, parameterType, methodRequired);
                        }
                        BeanReferenceUtils.setLazyProperty(method, parameter, beanReferences[j]);
                    }
                }
                setterValues.add(new SetterValue(method.getName(), method.getParameterTypes(), beanReferences, method));
            }
            return setterValues;
        }

        public List<SetterValue> createBeanNameCollectorSetterValue() {
            for (Method method : methodElement.getBeanNameCollectorElements()) {
                if (method.getParameters().length != 1) {
                    throw new IllegalStateException("@BeanNameCollector annotation requires a single-arg method: " + method);
                }
            }
            List<SetterValue> setterValues = new ArrayList<>();
            for (Method method : methodElement.getBeanNameCollectorElements()) {
                BeanNameCollector beanNameCollector = AnnotationUtils.get(method, BeanNameCollector.class);
                ResolvableType paramType = ResolvableType.forType(method.getGenericParameterTypes()[0]);
                Object[] args = new Object[1];
                args[0] = BeanReference.buildBeanNameCollector(paramType, ResolvableType.forRawClass(beanNameCollector.value()), beanNameCollector.exclude(), beanNameCollector.required());
                setterValues.add(new SetterValue(method.getName(), method.getParameterTypes(), args, method));
            }
            return setterValues;
        }

        public List<SetterValue> createBeanCollectorSetterValue() {
            for (Method method : methodElement.getBeanCollectorElements()) {
                if (method.getParameters().length != 1) {
                    throw new IllegalStateException("@BeanCollector annotation requires a single-arg method: " + method);
                }
            }
            List<SetterValue> setterValues = new ArrayList<>();
            for (Method method : methodElement.getBeanCollectorElements()) {
                BeanCollector beanCollector = AnnotationUtils.get(method, BeanCollector.class);
                ResolvableType paramType = ResolvableType.forType(method.getGenericParameterTypes()[0]);
                Object[] args = new Object[1];
                args[0] = BeanReference.buildBeanInstanceCollector(paramType, beanCollector.specify(), beanCollector.exclude(), beanCollector.required());
                setterValues.add(new SetterValue(method.getName(), method.getParameterTypes(), args, method));
            }
            return setterValues;
        }

        public List<SetterValue> createValueSetterValue() {
            List<SetterValue> setterValues = new ArrayList<>();
            for (Method method : methodElement.getValueElements()) {
                Parameter[] parameters = method.getParameters();
                BeanReference[] beanReferences = null;
                if (parameters != null) {

                    beanReferences = new BeanReference[parameters.length];
                    String methodValue = method.getAnnotation(Value.class).value();
                    boolean methodValueIsNotEmpty = StringUtils.hasText(methodValue);
                    String[] parameterNames = paramTables.getParameterNames(method);
                    Type[] genericParameterTypes = method.getGenericParameterTypes();

                    for (int j = 0; j < parameters.length; j++) {
                        String valueExpression;
                        if (methodValueIsNotEmpty) {
                            valueExpression = parameters[j].isAnnotationPresent(Value.class)
                                    ? parameters[j].getAnnotation(Value.class).value()
                                    : methodValue;
                        } else {
                            valueExpression = parameters[j].isAnnotationPresent(Value.class)
                                    ? parameters[j].getAnnotation(Value.class).value()
                                    : parameterNames[j];
                        }
                        beanReferences[j] = BeanReference.builderValue(valueExpression, ResolvableType.forType(genericParameterTypes[j]));
                        BeanReferenceUtils.setLazyProperty(method, parameters[j], beanReferences[j]);
                    }
                }
                setterValues.add(new SetterValue(method.getName(), method.getParameterTypes(), beanReferences, method));
            }
            return setterValues;
        }
    }

    /**
     * Bean属性注入器
     */
    static class BeanFieldInjection {

        private final InjectionElement<Field> fieldElements;

        /**
         * Bean工厂
         */
        private final ApplicationContext applicationContext;
        private final StandardBeanExpressionResolver exp;
        /**
         * 环境变量
         */
        private final Environment environment;
        /**
         * Bean信息
         */
        private final String beanInfo;
        private final Object bean;

        public BeanFieldInjection(String beanName, @NonNull Object bean, ApplicationContext applicationContext, Environment environment) {
            Assert.notNull(bean, "bean is null");
            this.bean = bean;
            this.fieldElements = getInjectionField(bean.getClass());
            this.applicationContext = applicationContext;
            this.environment = environment;
            this.beanInfo = beanName == null ? fieldElements.getBeanClass().getName() : beanName;
            this.exp = new StandardBeanExpressionResolver();
            this.exp.initializeStandardEvaluationContext(applicationContext, environment);
        }

        public void injectionFields() {
            injectionResourceFields();
            injectionQualifierFields();
            injectionAutowiredFields();
            injectionBeanNameCollectorFields();
            injectionBeanCollectorFields();
            injectionValueFields();
        }

        public void injectionResourceFields() {
            for (Field field : fieldElements.getResourcesElements()) {
                String fieldName = field.getName();
                try {
                    Resource resource = AnnotationUtils.get(field, javax.annotation.Resource.class);
                    String beanName = resource.name();
                    // 显示指定了ID时
                    if (StringUtils.hasText(beanName)) {
                        setFieldValue(bean, field, applicationContext.getBean(beanName));
                    }
                    // 为显示指定时，先尝试使用属性名作为id去容器中获取，获取不到则改为类型匹配
                    else if (applicationContext.containsBean(fieldName)) {
                        setFieldValue(bean, field, applicationContext.getBean(fieldName));
                    } else {
                        setFieldValue(bean, field, applicationContext.getBean(ResolvableType.forField(field, fieldElements.getBeanClass())));
                    }
                } catch (Exception e) {
                    throw new PropertyValueInjectorException(e, "An exception occurred while injecting '" + fieldName + "' attributes annotated by @Resource annotations into '" + beanInfo + "'");
                }
            }
        }

        public void injectionQualifierFields() {
            for (Field field : fieldElements.getQualifierElements()) {
                String fieldName = field.getName();
                try {
                    Qualifier qualifier = AnnotationUtils.get(field, Qualifier.class);
                    String beanName = qualifier.value();
                    Object fieldValue;
                    if (StringUtils.hasText(beanName)) {
                        fieldValue = applicationContext.getBean(beanName);
                    } else if (applicationContext.containsBean(fieldName)) {
                        fieldValue = applicationContext.getBean(fieldName);
                    } else {
                        fieldValue = applicationContext.getBean(ResolvableType.forField(field, fieldElements.getBeanClass()));
                    }
                    setBeanFieldValue(bean, field, fieldValue, qualifier.required());
                } catch (Exception e) {
                    throw new PropertyValueInjectorException(e, "An exception occurred while injecting '" + fieldName + "' attributes annotated by @Qualifier annotations into '" + beanInfo + "'");
                }

            }
        }

        public void injectionAutowiredFields() {
            for (Field autowiredField : fieldElements.getAutowiredElements()) {
                try {
                    Autowired autowired = AnnotationUtils.get(autowiredField, Autowired.class);
                    ResolvableType resolvableType = ResolvableType.forField(autowiredField, fieldElements.getBeanClass());
                    String beanName = autowiredField.getName();
                    Object fieldValue;
                    try {
                        fieldValue = applicationContext.getBean(resolvableType);
                    } catch (BeansException e) {
                        if (applicationContext.containsBean(beanName)) {
                            fieldValue = applicationContext.getBean(beanName);
                        } else {
                            throw new NoSuchBeanDefinitionException(resolvableType);
                        }
                    }
                    setBeanFieldValue(bean, autowiredField, fieldValue, autowired.required());
                } catch (Exception e) {
                    throw new PropertyValueInjectorException(e, "An exception occurred while injecting '" + autowiredField.getName() + "' attributes annotated by @Autowired annotations into '" + beanInfo + "'");
                }
            }
        }

        public void injectionBeanNameCollectorFields() {
            for (Field beanNameCollectorField : fieldElements.getBeanNameCollectorElements()) {
                try {
                    BeanNameCollector beanNameCollector = AnnotationUtils.get(beanNameCollectorField, BeanNameCollector.class);
                    ResolvableType resolvableType = ResolvableType.forField(beanNameCollectorField, fieldElements.getBeanClass());
                    List<String> fieldValue = Stream.of(applicationContext.getBeanNamesForType(beanNameCollector.value()))
                            .filter(n -> !ContainerUtils.inArrays(beanNameCollector.exclude(), n))
                            .collect(Collectors.toList());
                    setBeanFieldValue(bean, beanNameCollectorField, ConversionUtils.conversion(fieldValue, resolvableType), beanNameCollector.required());
                } catch (Exception e) {
                    throw new PropertyValueInjectorException(e, "An exception occurred while injecting '" + beanNameCollectorField.getName() + "' attributes annotated by @BeanNameCollector annotations into '" + beanInfo + "'");
                }
            }
        }

        public void injectionBeanCollectorFields() {
            for (Field beanCollectorField : fieldElements.getBeanCollectorElements()) {
                try {
                    BeanCollector beanCollector = AnnotationUtils.get(beanCollectorField, BeanCollector.class);
                    ResolvableType resolvableType = ResolvableType.forField(beanCollectorField, fieldElements.getBeanClass());
                    Class<?> rawClass = Objects.requireNonNull(resolvableType.getRawClass());

                    if (!resolvableType.isArray() && !Collection.class.isAssignableFrom(rawClass)) {
                        throw new IllegalArgumentException("The element type annotated by the @BeanCollector annotation must be an array or a collection type");
                    }

                    if (Collection.class.isAssignableFrom(rawClass) && !resolvableType.hasGenerics()) {
                        throw new IllegalArgumentException("Collection element annotated by @BeanCollector annotations must have a generic type");
                    }

                    ResolvableType beanType = resolvableType.isArray() ? resolvableType.getComponentType() : resolvableType.getGeneric(0);
                    List<Object> beans = new ArrayList<>();
                    if (!ContainerUtils.isEmptyArray(beanCollector.specify())) {
                        for (String specifyName : beanCollector.specify()) {
                            if (applicationContext.isTypeMatch(specifyName, beanType)) {
                                beans.add(applicationContext.getBean(specifyName));
                            } else {
                                throw new IllegalStateException("Bean instance collection failed: The type of the specified '" + specifyName + "' bean is not compatible with the type('" + beanType + "') of the bean pair to be collected");
                            }
                        }
                    } else {
                        Stream.of(applicationContext.getBeanNamesForType(beanType))
                                .filter(n -> !ContainerUtils.inArrays(beanCollector.exclude(), n))
                                .forEach(n -> beans.add(applicationContext.getBean(n)));
                    }
                    AnnotationAwareOrderComparator.sort(beans);
                    setBeanFieldValue(bean, beanCollectorField, ConversionUtils.conversion(beans, resolvableType), beanCollector.required());
                } catch (Exception e) {
                    throw new PropertyValueInjectorException(e, "An exception occurred while injecting '" + beanCollectorField.getName() + "' attributes annotated by @BeanCollector annotations into '" + beanInfo + "'");
                }
            }
        }

        public void injectionValueFields() {
            for (Field valueField : fieldElements.getValueElements()) {
                try {
                    String value = AnnotationUtils.get(valueField, Value.class).value();
                    ResolvableType fieldType = ResolvableType.forField(valueField, fieldElements.getBeanClass());
                    Object fieldValue = valueExpressionParsing(exp, (LuckyStandardEnvironment) environment, value, fieldType);
                    setFieldValue(bean, valueField, fieldValue);
                } catch (Exception e) {
                    throw new PropertyValueInjectorException(e, "An exception occurred while injecting '" + valueField.getName() + "' attributes annotated by @Value annotations into '" + beanInfo + "'");
                }

            }
        }

        private void setBeanFieldValue(Object bean, Field field, Object fieldValue, boolean isRequired) {
            if (fieldValue != null) {
                setFieldValue(bean, field, fieldValue);
            } else if (isRequired) {
                throw new NoSuchBeanDefinitionException(ResolvableType.forField(field, bean.getClass()));
            }
        }

        private void setFieldValue(Object bean, Field field, Object fieldValue) {
            if (Modifier.isStatic(field.getModifiers())) {
                FieldUtils.setValue(bean.getClass(), field, fieldValue);
            } else {
                FieldUtils.setValue(bean, field, fieldValue);
            }
        }

    }

    /**
     * Bean方法参数注入器
     */
    @SuppressWarnings("all")
    static class BeanMethodParameterInjection {

        private final InjectionElement<Method> methodElements;

        /**
         * Bean工厂
         */
        private final ApplicationContext applicationContext;
        private final StandardBeanExpressionResolver exp;
        private final LocalVariableTableParameterNameDiscoverer paramTables = new LocalVariableTableParameterNameDiscoverer();
        /**
         * 环境变量
         */
        private final Environment environment;
        /**
         * Bean信息
         */
        private final String beanInfo;
        private final Object bean;

        public BeanMethodParameterInjection(String beanName, @NonNull Object bean, ApplicationContext applicationContext, Environment environment) {
            Assert.notNull(bean, "bean is null");
            this.bean = bean;
            this.methodElements = getInjectionMethod(bean.getClass());
            this.applicationContext = applicationContext;
            this.environment = environment;
            this.beanInfo = beanName == null ? methodElements.getBeanClass().getName() : beanName;
            this.exp = new StandardBeanExpressionResolver();
            this.exp.initializeStandardEvaluationContext(applicationContext, environment);
        }

        public void injectionMethodParameter() {
            injectionResourceMethodParameter();
            injectionQualifierMethodParameter();
            injectionAutowiredMethodParameter();
            injectionBeanNameCollectorMethodParameter();
            injectionBeanCollectorMethodParameter();
            injectionValueMethodParameter();
        }

        public void injectionResourceMethodParameter() {

            for (Method method : methodElements.getResourcesElements()) {
                if (method.getParameters().length != 1) {
                    throw new IllegalStateException("@Resource annotation requires a single-arg method: " + method);
                }
            }

            for (Method method : methodElements.getResourcesElements()) {
                try {
                    Object[] methodArgs = new Object[1];
                    Resource resourceQualifier = AnnotationUtils.get(method, Resource.class);
                    String resourceName = resourceQualifier.name();
                    String parameterName = Objects.requireNonNull(paramTables.getParameterNames(method))[0];
                    ResolvableType type = ResolvableType.forType(method.getGenericParameterTypes()[0]);

                    if (StringUtils.hasText(resourceName)) {
                        methodArgs[0] = applicationContext.getBean(resourceName);
                    } else if (applicationContext.containsBean(parameterName)) {
                        methodArgs[0] = applicationContext.getBean(parameterName);
                    } else {
                        methodArgs[0] = applicationContext.getBean(type);
                    }
                    invokeMethod(bean, method, methodArgs);
                } catch (Exception e) {
                    throw new PropertyValueInjectorException(e, "An exception occurred while injecting properties into '" + beanInfo + "' beans through '" + method + "' methods annotated by @Resource annotations");
                }
            }
        }

        public void injectionQualifierMethodParameter() {
            for (Method method : methodElements.getQualifierElements()) {
                if (method.getParameters().length != 1) {
                    throw new IllegalStateException("@Qualifier annotation requires a single-arg method: " + method);
                }
            }

            for (Method method : methodElements.getQualifierElements()) {
                try {
                    Object[] methodArgs = new Object[1];
                    Qualifier methodQualifier = AnnotationUtils.get(method, Qualifier.class);
                    String qualifierValue = methodQualifier.value();
                    String parameterName = Objects.requireNonNull(paramTables.getParameterNames(method))[0];
                    ResolvableType type = ResolvableType.forType(method.getGenericParameterTypes()[0]);

                    Object arg;
                    if (StringUtils.hasText(qualifierValue)) {
                        arg = applicationContext.getBean(qualifierValue);
                    } else if (applicationContext.containsBean(parameterName)) {
                        arg = applicationContext.getBean(parameterName);
                    } else {
                        arg = applicationContext.getBean(type);
                    }
                    methodArgs[0] = getMethodArg(arg, type, methodQualifier.required());
                    invokeMethod(bean, method, methodArgs);
                } catch (Exception e) {
                    throw new PropertyValueInjectorException(e, "An exception occurred while injecting properties into '" + beanInfo + "' beans through '" + method + "' methods annotated by @Resource annotations");
                }

            }
        }

        public void injectionAutowiredMethodParameter() {
            for (Method method : methodElements.getAutowiredElements()) {
                try {
                    Parameter[] parameters = method.getParameters();
                    if (!ContainerUtils.isEmptyArray(parameters)) {

                        Object[] methodArgs = new Object[parameters.length];
                        String[] parameterNames = paramTables.getParameterNames(method);
                        Type[] genericParameterTypes = method.getGenericParameterTypes();
                        boolean methodRequired = method.getAnnotation(Autowired.class).required();

                        for (int i = 0; i < parameters.length; i++) {
                            Parameter parameter = parameters[i];
                            ResolvableType parameterType = ResolvableType.forType(genericParameterTypes[i]);
                            assert parameterNames != null;
                            String parameterName = parameterNames[i];
                            // @Resource
                            if (parameter.isAnnotationPresent(Resource.class)) {
                                Resource resource = parameter.getAnnotation(Resource.class);
                                String resourceName = resource.name();
                                if (StringUtils.hasText(resourceName)) {
                                    methodArgs[i] = applicationContext.getBean(resourceName);
                                } else if (applicationContext.containsBean(parameterName)) {
                                    methodArgs[i] = applicationContext.getBean(parameterName);
                                } else {
                                    methodArgs[i] = applicationContext.getBean(parameterType);
                                }
                            }
                            // @Qualifier
                            else if (parameter.isAnnotationPresent(Qualifier.class)) {
                                Qualifier qualifier = parameter.getAnnotation(Qualifier.class);
                                String qualifierValue = qualifier.value();
                                Object argI;
                                if (StringUtils.hasText(qualifierValue)) {
                                    argI = applicationContext.getBean(qualifierValue);
                                } else if (applicationContext.containsBean(parameterName)) {
                                    argI = applicationContext.getBean(parameterName);
                                } else {
                                    argI = applicationContext.getBean(parameterType);
                                }
                                methodArgs[i] = getMethodArg(argI, parameterType, qualifier.required());
                            }
                            // @Autowired
                            else if (parameter.isAnnotationPresent(Autowired.class)) {
                                Autowired autowired = parameter.getAnnotation(Autowired.class);
                                Object argI;
                                try {
                                    argI = applicationContext.getBean(parameterType);
                                } catch (BeansException e) {
                                    if (applicationContext.containsBean(parameterName)) {
                                        argI = applicationContext.getBean(parameterName);
                                    } else {
                                        throw new NoSuchBeanDefinitionException(parameterType);
                                    }
                                }
                                methodArgs[i] = getMethodArg(argI, parameterType, autowired.required());
                            }
                            // @BeanNameCollector
                            else if (parameter.isAnnotationPresent(BeanNameCollector.class)) {
                                BeanNameCollector beanNameCollector = parameter.getAnnotation(BeanNameCollector.class);
                                List<String> argI = Stream.of(applicationContext.getBeanNamesForType(beanNameCollector.value()))
                                        .filter(n -> !ContainerUtils.inArrays(beanNameCollector.exclude(), n))
                                        .collect(Collectors.toList());
                                methodArgs[i] = getMethodArg(ConversionUtils.conversion(argI, parameterType), parameterType, beanNameCollector.required());
                            }
                            // @BeanCollector
                            else if (parameter.isAnnotationPresent(BeanCollector.class)) {
                                BeanCollector beanCollector = parameter.getAnnotation(BeanCollector.class);
                                Class<?> rawClass = Objects.requireNonNull(parameterType.getRawClass());

                                if (!parameterType.isArray() && !Collection.class.isAssignableFrom(rawClass)) {
                                    throw new IllegalArgumentException("The element type annotated by the @BeanCollector annotation must be an array or a collection type");
                                }

                                if (Collection.class.isAssignableFrom(rawClass) && !parameterType.hasGenerics()) {
                                    throw new IllegalArgumentException("Collection element annotated by @BeanCollector annotations must have a generic type");
                                }

                                ResolvableType beanType = parameterType.isArray() ? parameterType.getComponentType() : parameterType.getGeneric(0);
                                List<Object> beans = new ArrayList<>();
                                if (!ContainerUtils.isEmptyArray(beanCollector.specify())) {
                                    for (String specifyName : beanCollector.specify()) {
                                        if (applicationContext.isTypeMatch(specifyName, beanType)) {
                                            beans.add(applicationContext.getBean(specifyName));
                                        } else {
                                            throw new IllegalStateException("Bean instance collection failed: The type of the specified '" + specifyName + "' bean is not compatible with the type('" + beanType + "') of the bean pair to be collected");
                                        }
                                    }
                                } else {
                                    Stream.of(applicationContext.getBeanNamesForType(beanType))
                                            .filter(n -> !ContainerUtils.inArrays(beanCollector.exclude(), n))
                                            .forEach(n -> beans.add(applicationContext.getBean(n)));
                                }
                                AnnotationAwareOrderComparator.sort(beans);
                                methodArgs[i] = getMethodArg(ConversionUtils.conversion(beans, parameterType), parameterType, beanCollector.required());
                            }
                            // @Value
                            else if (parameter.isAnnotationPresent(Value.class)) {
                                Value value = parameter.getAnnotation(Value.class);
                                String valueExpression = StringUtils.hasText(value.value()) ? value.value() : parameterName;
                                methodArgs[i] = valueExpressionParsing(exp, (LuckyStandardEnvironment) environment, valueExpression, ResolvableType.forType(genericParameterTypes[i]));
                            }
                            // 参数无注
                            else {
                                Object argI;
                                try {
                                    argI = applicationContext.getBean(parameterType);
                                } catch (BeansException e) {
                                    if (applicationContext.containsBean(parameterName)) {
                                        argI = applicationContext.getBean(parameterName);
                                    } else {
                                        throw new NoSuchBeanDefinitionException(parameterType);
                                    }
                                }
                                methodArgs[i] = getMethodArg(argI, parameterType, methodRequired);
                            }
                        }
                        invokeMethod(bean, method, methodArgs);
                    }
                } catch (Exception e) {
                    throw new PropertyValueInjectorException(e, "An exception occurred while injecting properties into '" + beanInfo + "' beans through '" + method + "' methods annotated by @Resource annotations");
                }

            }
        }

        public void injectionBeanNameCollectorMethodParameter() {
            for (Method method : methodElements.getBeanNameCollectorElements()) {
                if (method.getParameters().length != 1) {
                    throw new IllegalStateException("@BeanNameCollector annotation requires a single-arg method: " + method);
                }
            }

            for (Method method : methodElements.getBeanNameCollectorElements()) {
                try {
                    Object[] methodArgs = new Object[1];
                    BeanNameCollector beanNameCollector = AnnotationUtils.get(method, BeanNameCollector.class);
                    ResolvableType paramType = ResolvableType.forType(method.getGenericParameterTypes()[0]);
                    List<String> beanNames = Stream.of(applicationContext.getBeanNamesForType(beanNameCollector.value()))
                            .filter(n -> !ContainerUtils.inArrays(beanNameCollector.exclude(), n))
                            .collect(Collectors.toList());
                    methodArgs[0] = getMethodArg(ConversionUtils.conversion(beanNames, paramType), paramType, beanNameCollector.required());
                    invokeMethod(bean, method, methodArgs);
                } catch (Exception e) {
                    throw new PropertyValueInjectorException(e, "An exception occurred while injecting properties into '" + beanInfo + "' beans through '" + method + "' methods annotated by @BeanNameCollector annotations");
                }
            }
        }

        public void injectionBeanCollectorMethodParameter() {
            for (Method method : methodElements.getBeanCollectorElements()) {
                if (method.getParameters().length != 1) {
                    throw new IllegalStateException("@BeanCollector annotation requires a single-arg method: " + method);
                }
            }

            for (Method method : methodElements.getBeanCollectorElements()) {
                try {
                    Object[] methodArgs = new Object[1];
                    BeanCollector beanCollector = AnnotationUtils.get(method, BeanCollector.class);
                    ResolvableType paramType = ResolvableType.forType(method.getGenericParameterTypes()[0]);

                    Class<?> rawClass = Objects.requireNonNull(paramType.getRawClass());

                    if (!paramType.isArray() && !Collection.class.isAssignableFrom(rawClass)) {
                        throw new IllegalArgumentException("The element type annotated by the @BeanCollector annotation must be an array or a collection type");
                    }

                    if (Collection.class.isAssignableFrom(rawClass) && !paramType.hasGenerics()) {
                        throw new IllegalArgumentException("Collection element annotated by @BeanCollector annotations must have a generic type");
                    }

                    ResolvableType beanType = paramType.isArray() ? paramType.getComponentType() : paramType.getGeneric(0);
                    List<Object> beans = new ArrayList<>();
                    if (!ContainerUtils.isEmptyArray(beanCollector.specify())) {
                        for (String specifyName : beanCollector.specify()) {
                            if (applicationContext.isTypeMatch(specifyName, beanType)) {
                                beans.add(applicationContext.getBean(specifyName));
                            } else {
                                throw new IllegalStateException("Bean instance collection failed: The type of the specified '" + specifyName + "' bean is not compatible with the type('" + beanType + "') of the bean pair to be collected");
                            }
                        }
                    } else {
                        Stream.of(applicationContext.getBeanNamesForType(beanType))
                                .filter(n -> !ContainerUtils.inArrays(beanCollector.exclude(), n))
                                .forEach(n -> beans.add(applicationContext.getBean(n)));
                    }
                    AnnotationAwareOrderComparator.sort(beans);
                    methodArgs[0] = getMethodArg(ConversionUtils.conversion(beans, paramType), paramType, beanCollector.required());
                    invokeMethod(bean, method, methodArgs);
                } catch (Exception e) {
                    throw new PropertyValueInjectorException(e, "An exception occurred while injecting properties into '" + beanInfo + "' beans through '" + method + "' methods annotated by @BeanCollector annotations");
                }
            }
        }

        public void injectionValueMethodParameter() {
            for (Method method : methodElements.getValueElements()) {
                try {
                    Parameter[] parameters = method.getParameters();
                    if (!ContainerUtils.isEmptyArray(parameters)) {
                        String methodValue = method.getAnnotation(Value.class).value();
                        boolean methodValueIsNotEmpty = StringUtils.hasText(methodValue);
                        Object[] methodArgs = new Object[parameters.length];
                        String[] parameterNames = paramTables.getParameterNames(method);
                        Type[] genericParameterTypes = method.getGenericParameterTypes();
                        for (int i = 0; i < parameters.length; i++) {
                            String valueExpression;
                            if (methodValueIsNotEmpty) {
                                valueExpression = parameters[i].isAnnotationPresent(Value.class)
                                        ? parameters[i].getAnnotation(Value.class).value()
                                        : methodValue;
                            } else {
                                valueExpression = parameters[i].isAnnotationPresent(Value.class)
                                        ? parameters[i].getAnnotation(Value.class).value()
                                        : parameterNames[i];
                            }
                            methodArgs[i] = valueExpressionParsing(exp, (LuckyStandardEnvironment) environment, valueExpression, ResolvableType.forType(genericParameterTypes[i]));
                        }
                        invokeMethod(bean, method, methodArgs);
                    }
                } catch (Exception e) {
                    throw new PropertyValueInjectorException(e, "An exception occurred while injecting properties into '" + beanInfo + "' beans through '" + method + "' methods annotated by @Resource annotations");
                }
            }
        }

        private Object getMethodArg(Object arg, ResolvableType argType, boolean isRequired) {
            if (arg == null && isRequired) {
                throw new NoSuchBeanDefinitionException(argType);
            }
            return arg;
        }

        private void invokeMethod(Object bean, Method method, Object... methodArgs) {
            if (Modifier.isStatic(method.getModifiers())) {
                MethodUtils.invoke(bean.getClass(), method, methodArgs);
            } else {
                MethodUtils.invoke(bean, method, methodArgs);
            }
        }

    }

    /**
     * 用于获取注解元素的函数式接口
     *
     * @param <T> 注解元素类型
     */
    @FunctionalInterface
    interface AnnotatedElementGetter<T extends AnnotatedElement> {

        List<T> getElements(Class<?> beanClass, Class<? extends Annotation> annotationClass);

    }


}
