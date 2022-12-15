package com.luckyframework.bean;

import com.luckyframework.annotations.Autowired;
import com.luckyframework.annotations.Qualifier;
import com.luckyframework.annotations.Value;
import com.luckyframework.bean.factory.DisposableBean;
import com.luckyframework.bean.factory.InitializingBean;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.context.ApplicationContext;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.environment.LuckyStandardEnvironment;
import com.luckyframework.exception.*;
import com.luckyframework.expression.StandardBeanExpressionResolver;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.Closeable;
import java.lang.reflect.*;
import java.util.List;

/**
 * Bean注解元素管理器，封装了一系列有关于Bean生命周期的方法
 */
public class BeanAnnotationManager {

    /** Bean工厂*/
    private final ApplicationContext applicationContext;

    private final StandardBeanExpressionResolver exp;
    /** 环境变量*/
    private final Environment environment;
    /** Bean信息*/
    private final String inBeanName;

    public BeanAnnotationManager(String inBeanName, ApplicationContext applicationContext, Environment environment) {
        this.applicationContext = applicationContext;
        this.environment = environment;
        this.inBeanName = inBeanName;
        this.exp = new StandardBeanExpressionResolver();
        this.exp.initializeStandardEvaluationContext(applicationContext, environment);
    }

    public BeanAnnotationManager(ApplicationContext applicationContext, Environment environment){
        this(null, applicationContext,environment);
    }

    /**
     * 给某个Bean注入属性
     * @param bean 非空的bean
     */
    public void injectionField(@NonNull Object bean){
        Assert.notNull(bean,"bean is null");
        Class<?> beanClass = bean.getClass();
        String beanInfo = inBeanName == null ? beanClass.getName() : inBeanName;
        List<Field> resourceFields = ClassUtils.getFieldByAnnotation(beanClass, Resource.class);

        List<Field> qualifierFields = ClassUtils.getFieldByAnnotation(beanClass, Qualifier.class);
        qualifierFields.removeIf(resourceFields::contains);

        List<Field> autowiredFields = ClassUtils.getFieldByAnnotation(beanClass, Autowired.class);
        autowiredFields.removeIf(f -> resourceFields.contains(f) || qualifierFields.contains(f));

        List<Field> valueFields = ClassUtils.getFieldByAnnotation(beanClass, Value.class);
        valueFields.removeIf(f -> resourceFields.contains(f) || qualifierFields.contains(f) || autowiredFields.contains(f));

        for (Field field : resourceFields) {
            String fieldName = field.getName();
            try {
                Resource resource = AnnotationUtils.get(field, javax.annotation.Resource.class);
                String beanName = resource.name();
                // 显示指定了ID时
                if(StringUtils.hasText(beanName)){
                    setFieldValue(bean,field, applicationContext.getBean(beanName));
                }
                // 为显示指定时，先尝试使用属性名作为id去容器中获取，获取不到则改为类型匹配
                else if(applicationContext.containsBean(fieldName)){
                    setFieldValue(bean,field, applicationContext.getBean(fieldName));
                }
                else{
                    setFieldValue(bean,field, applicationContext.getBean(ResolvableType.forField(field,beanClass)));
                }
            }catch (Exception e){
                throw new PropertyValueInjectorException(e,"An exception occurred while injecting '"+fieldName+"' attributes annotated by @Resource annotations into '"+beanInfo+"'");
            }

        }
        for (Field field : qualifierFields) {
            String fieldName = field.getName();
            try {
                Qualifier qualifier = AnnotationUtils.get(field, Qualifier.class);
                String beanName = qualifier.value();
                Object fieldValue;
                if(StringUtils.hasText(beanName)){
                    fieldValue = applicationContext.getBean(beanName);
                }
                else if(applicationContext.containsBean(fieldName)){
                    fieldValue = applicationContext.getBean(fieldName);
                }
                else{
                    fieldValue = applicationContext.getBean(ResolvableType.forField(field, beanClass));
                }
                setBeanFieldValue(bean,field,fieldValue,qualifier.required());
            }catch (Exception e){
                throw new PropertyValueInjectorException(e,"An exception occurred while injecting '"+fieldName+"' attributes annotated by @Qualifier annotations into '"+beanInfo+"'");
            }

        }
        for (Field autowiredField : autowiredFields) {
            try {
                Autowired autowired = AnnotationUtils.get(autowiredField, Autowired.class);
                ResolvableType resolvableType = ResolvableType.forField(autowiredField, beanClass);
                String beanName = autowiredField.getName();
                Object fieldValue;
                try {
                    fieldValue = applicationContext.getBean(resolvableType);
                } catch (BeansException e) {
                    if(applicationContext.containsBean(beanName)){
                        fieldValue = applicationContext.getBean(beanName);
                    }else{
                        throw new NoSuchBeanDefinitionException(resolvableType);
                    }
                }
                setBeanFieldValue(bean,autowiredField,fieldValue,autowired.required());
            }catch (Exception e){
                throw new PropertyValueInjectorException(e,"An exception occurred while injecting '"+autowiredField.getName()+"' attributes annotated by @Autowired annotations into '"+beanInfo+"'");
            }

        }
        for (Field valueField : valueFields) {
            try {
                String value = AnnotationUtils.get(valueField, Value.class).value();
                ResolvableType fieldType = ResolvableType.forField(valueField, beanClass);
                Object fieldValue = getFieldValue(exp, (LuckyStandardEnvironment) environment, value, fieldType);
                setFieldValue(bean, valueField, fieldValue);
            }catch (Exception e){
                throw new PropertyValueInjectorException(e,"An exception occurred while injecting '"+valueField.getName()+"' attributes annotated by @Value annotations into '"+beanInfo+"'");
            }

        }
    }

    /**
     * 通过Set方法给bean注入属性
     * @param bean 非空的bean
     */
    public void invokeSetMethod(@NonNull Object bean){
        Assert.notNull(bean,"bean is null");
        Class<?> beanClass = bean.getClass();
        String beanInfo = inBeanName == null ? beanClass.getName() : inBeanName;

        List<Method> resourceMethods = ClassUtils.getMethodByAnnotation(beanClass, Resource.class);

        List<Method> qualifierMethods = ClassUtils.getMethodByAnnotation(beanClass, Qualifier.class);
        qualifierMethods.removeIf(resourceMethods::contains);

        List<Method> autowiredMethods = ClassUtils.getMethodByAnnotation(beanClass, Autowired.class);
        autowiredMethods.removeIf(f -> resourceMethods.contains(f) || qualifierMethods.contains(f));

        List<Method> valueMethods = ClassUtils.getMethodByAnnotation(beanClass, Value.class);
        valueMethods.removeIf(f -> resourceMethods.contains(f) || qualifierMethods.contains(f) || autowiredMethods.contains(f));

        for (Method method : resourceMethods) {
            if(method.getParameters().length!=1){
                throw new IllegalStateException("@Resource annotation requires a single-arg method: "+method);
            }
        }

        for (Method method : qualifierMethods) {
            if(method.getParameters().length!=1){
                throw new IllegalStateException("@Qualifier annotation requires a single-arg method: "+method);
            }
        }
        LocalVariableTableParameterNameDiscoverer paramTables = new LocalVariableTableParameterNameDiscoverer();

        // 执行被@Resource注解标注的方法
        for (Method method : resourceMethods) {
            try {
                Object[] methodArgs = new Object[1];
                Resource resourceQualifier = AnnotationUtils.get(method,Resource.class);
                String resourceName = resourceQualifier.name();
                String parameterName = paramTables.getParameterNames(method)[0];
                ResolvableType type = ResolvableType.forType(method.getGenericParameterTypes()[0]);

                if(StringUtils.hasText(resourceName)){
                    methodArgs[0] = applicationContext.getBean(resourceName);
                }
                else if(applicationContext.containsBean(parameterName)){
                    methodArgs[0] = applicationContext.getBean(parameterName);
                }
                else{
                    methodArgs[0] = applicationContext.getBean(type);
                }
                invokeMethod(bean,method,methodArgs);
            }catch (Exception e){
                throw new PropertyValueInjectorException(e,"An exception occurred while injecting properties into '"+beanInfo+"' beans through '"+method+"' methods annotated by @Resource annotations");
            }

        }

        // 执行被@Qualifier注解标注的方法
        for (Method method : qualifierMethods) {
            try {
                Object[] methodArgs = new Object[1];
                Qualifier methodQualifier = AnnotationUtils.get(method,Qualifier.class);
                String qualifierValue = methodQualifier.value();
                String parameterName = paramTables.getParameterNames(method)[0];
                ResolvableType type = ResolvableType.forType(method.getGenericParameterTypes()[0]);

                Object arg;
                if(StringUtils.hasText(qualifierValue)){
                    arg = applicationContext.getBean(qualifierValue);
                }
                else if(applicationContext.containsBean(parameterName)){
                    arg = applicationContext.getBean(parameterName);
                }
                else{
                    arg = applicationContext.getBean(type);
                }
                methodArgs[0] = getMethodArg(arg,type,methodQualifier.required());
                invokeMethod(bean,method,methodArgs);
            }catch (Exception e){
                throw new PropertyValueInjectorException(e,"An exception occurred while injecting properties into '"+beanInfo+"' beans through '"+method+"' methods annotated by @Resource annotations");
            }

        }

        // 执行被@Autowired注解标注的方法
        for (Method method : autowiredMethods) {
            try {
                Parameter[] parameters = method.getParameters();
                if(!ContainerUtils.isEmptyArray(parameters)){

                    Object[] methodArgs = new Object[parameters.length];
                    String[] parameterNames = paramTables.getParameterNames(method);
                    Type[] genericParameterTypes = method.getGenericParameterTypes();
                    boolean methodRequired = method.getAnnotation(Autowired.class).required();

                    for (int i = 0; i < parameters.length; i++) {
                        Parameter parameter = parameters[i];
                        ResolvableType parameterType = ResolvableType.forType(genericParameterTypes[i]);
                        String parameterName = parameterNames[i];
                        // @Resource
                        if(parameter.isAnnotationPresent(Resource.class)){
                            Resource resource = parameter.getAnnotation(Resource.class);
                            String resourceName = resource.name();
                            if(StringUtils.hasText(resourceName)){
                                methodArgs[i] = applicationContext.getBean(resourceName);
                            }else if(applicationContext.containsBean(parameterName)){
                                methodArgs[i] = applicationContext.getBean(parameterName);
                            }else{
                                methodArgs[i] = applicationContext.getBean(parameterType);
                            }
                        }
                        // @Qualifier
                        else if(parameter.isAnnotationPresent(Qualifier.class)){
                            Qualifier qualifier = parameter.getAnnotation(Qualifier.class);
                            String qualifierValue = qualifier.value();
                            Object argI;
                            if(StringUtils.hasText(qualifierValue)){
                                argI = applicationContext.getBean(qualifierValue);
                            }else if (applicationContext.containsBean(parameterName)){
                                argI = applicationContext.getBean(parameterName);
                            }else{
                                argI = applicationContext.getBean(parameterType);
                            }
                            methodArgs[i] = getMethodArg(argI,parameterType,qualifier.required());
                        }
                        // @Autowired
                        else if(parameter.isAnnotationPresent(Autowired.class)){
                            Autowired autowired = parameter.getAnnotation(Autowired.class);
                            Object argI;
                            try {
                                argI = applicationContext.getBean(parameterType);
                            } catch (BeansException e) {
                                if(applicationContext.containsBean(parameterName)){
                                    argI = applicationContext.getBean(parameterName);
                                }else{
                                    throw new NoSuchBeanDefinitionException(parameterType);
                                }
                            }
                            methodArgs[i] = getMethodArg(argI,parameterType,autowired.required());
                        }
                        // @Value
                        else if(parameter.isAnnotationPresent(Value.class)){
                            Value value = parameter.getAnnotation(Value.class);
                            String valueExpression = StringUtils.hasText(value.value()) ? value.value() : parameterName;
                            methodArgs[i] = getFieldValue(exp, (LuckyStandardEnvironment) environment, valueExpression, ResolvableType.forType(genericParameterTypes[i]));
                        }
                        // 参数无注
                        else{
                            Object argI;
                            try {
                                argI = applicationContext.getBean(parameterType);
                            } catch (BeansException e) {
                                if(applicationContext.containsBean(parameterName)){
                                    argI = applicationContext.getBean(parameterName);
                                }else{
                                    throw new NoSuchBeanDefinitionException(parameterType);
                                }
                            }
                            methodArgs[i] = getMethodArg(argI,parameterType,methodRequired);
                        }
                    }
                    invokeMethod(bean,method,methodArgs);
                }
            }catch (Exception e){
                throw new PropertyValueInjectorException(e,"An exception occurred while injecting properties into '"+beanInfo+"' beans through '"+method+"' methods annotated by @Resource annotations");
            }

        }

        // 执行被@Value注解标注的方法
        for (Method method : valueMethods) {
            try {
                Parameter[] parameters = method.getParameters();
                if(!ContainerUtils.isEmptyArray(parameters)){
                    String methodValue = method.getAnnotation(Value.class).value();
                    boolean methodValueIsNotEmpty = StringUtils.hasText(methodValue);
                    Object[] methodArgs = new Object[parameters.length];
                    String[] parameterNames = paramTables.getParameterNames(method);
                    Type[] genericParameterTypes = method.getGenericParameterTypes();
                    for (int i = 0; i < parameters.length; i++) {
                        String valueExpression;
                        if(methodValueIsNotEmpty){
                            valueExpression = parameters[i].isAnnotationPresent(Value.class)
                                    ? parameters[i].getAnnotation(Value.class).value()
                                    : methodValue;
                        } else {
                            valueExpression = parameters[i].isAnnotationPresent(Value.class)
                                    ? parameters[i].getAnnotation(Value.class).value()
                                    : parameterNames[i];
                        }
                        methodArgs[i] = getFieldValue(exp, (LuckyStandardEnvironment) environment, valueExpression, ResolvableType.forType(genericParameterTypes[i]));
                    }
                    invokeMethod(bean,method,methodArgs);
                }
            }catch (Exception e){
                throw new PropertyValueInjectorException(e,"An exception occurred while injecting properties into '"+beanInfo+"' beans through '"+method+"' methods annotated by @Resource annotations");
            }
        }
    }

    /**
     * 执行bean的初始化操作
     * @param bean 非空的bean
     */
    public void initialize(@NonNull Object bean){
        Assert.notNull(bean,"bean is null");
        Class<?> beanClass = bean.getClass();
        String beanInfo = inBeanName == null ? beanClass.getName() : inBeanName;
        if(bean instanceof InitializingBean){
            try {
                ((InitializingBean)bean).afterPropertiesSet();
            } catch (Exception e) {
                throw new BeanCreationException("An exception occurred when using the 'InitializingBean#afterPropertiesSet()' method to initialize the bean named '"+beanInfo+"'",e);
            }
        }
        List<Method> initMethodList = ClassUtils.getMethodByAnnotation(beanClass, PostConstruct.class);
        for (Method initMethod : initMethodList) {
            try{
                invokeMethod(bean,initMethod);
            }catch (Exception e){
                throw new BeanCreationException("An exception occurs when the bean named '"+beanInfo+"' is initialized using the initialization method '"+initMethod+"()' ",e);
            }
        }
    }

    /**
     * 执行bean的销毁方法
     * @param bean 非空的bean
     */
    public void destroy(@NonNull Object bean){
        Assert.notNull(bean,"bean is null");
        Class<?> beanClass = bean.getClass();
        String beanInfo = inBeanName == null ? beanClass.getName() : inBeanName;
        if(bean instanceof DisposableBean){
            try {
                ((DisposableBean)bean).destroy();
            } catch (Exception e) {
                throw new BeanDisposableException("An exception occurred when using the 'DisposableBean#destroy()' destruction method of the bean named '"+beanInfo+"'.",e);
            }
        } else if (bean instanceof Closeable) {
            try {
                ((Closeable)bean).close();
            }catch (Exception e){
                throw new BeanDisposableException("An exception occurred when using the 'Closeable#close()' destruction method of the bean named '"+beanInfo+"'.",e);
            }
        }
        List<Method> destroyMethodList = ClassUtils.getMethodByAnnotation(beanClass, PreDestroy.class);
        for (Method destroyMethod : destroyMethodList) {
            try {
                invokeMethod(bean,destroyMethod);
            }catch (Exception e){
                throw new BeanDisposableException("An exception occurred when using the destroy method '"+destroyMethod+"()' in the bean definition. bean: '"+beanInfo+"'",e);
            }
        }
    }


    //---------------------------------------------------------------------------------
    //                 工具方法，当值为null，但是isRequired为true时 抛出异常
    //---------------------------------------------------------------------------------

    private void setBeanFieldValue(Object bean,Field field,Object fieldValue,boolean isRequired){
        if(fieldValue != null){
            setFieldValue(bean,field,fieldValue);
        } else if(isRequired){
            throw new NoSuchBeanDefinitionException(ResolvableType.forField(field, bean.getClass()));
        }
    }

    private Object getMethodArg(Object arg,ResolvableType argType,boolean isRequired){
        if(arg == null && isRequired){
            throw new NoSuchBeanDefinitionException(argType);
        }
        return arg;
    }

    private void setFieldValue(Object bean,Field field,Object fieldValue){
        if(Modifier.isStatic(field.getModifiers())){
            FieldUtils.setValue(bean.getClass(),field,fieldValue);
        }else{
            FieldUtils.setValue(bean,field,fieldValue);
        }
    }

    private void invokeMethod(Object bean,Method method,Object...methodArgs){
        if(Modifier.isStatic(method.getModifiers())){
            MethodUtils.invoke(bean.getClass(),method,methodArgs);
        }else{
            MethodUtils.invoke(bean,method,methodArgs);
        }
    }

    private static Object getFieldValue(StandardBeanExpressionResolver exp, LuckyStandardEnvironment environment, String valueExpression, ResolvableType type){
        Object value = exp.evaluate(valueExpression);
        if(value instanceof String){
            value = environment.resolveRequiredPlaceholdersForObject(value);
        }
        return ConversionUtils.conversion(value, type);
    }
}
