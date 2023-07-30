package com.luckyframework.bean;

import com.luckyframework.bean.factory.DisposableBean;
import com.luckyframework.bean.factory.InitializingBean;
import com.luckyframework.context.ApplicationContext;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.environment.LuckyStandardEnvironment;
import com.luckyframework.exception.BeanCreationException;
import com.luckyframework.exception.BeanDisposableException;
import com.luckyframework.exception.NoSuchBeanDefinitionException;
import com.luckyframework.expression.StandardBeanExpressionResolver;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Bean注解元素管理器，封装了一系列有关于Bean生命周期的方法
 */
public class BeanAnnotationManager {

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
    private final String inBeanName;

    public BeanAnnotationManager(String inBeanName, ApplicationContext applicationContext, Environment environment) {
        this.applicationContext = applicationContext;
        this.environment = environment;
        this.inBeanName = inBeanName;
        this.exp = new StandardBeanExpressionResolver();
        this.exp.initializeStandardEvaluationContext(applicationContext, environment);
    }

    public BeanAnnotationManager(ApplicationContext applicationContext, Environment environment) {
        this(null, applicationContext, environment);
    }

    /**
     * 执行bean的初始化操作
     *
     * @param bean 非空的bean
     */
    public void initialize(@NonNull Object bean) {
        Assert.notNull(bean, "bean is null");
        Class<?> beanClass = bean.getClass();
        String beanInfo = inBeanName == null ? beanClass.getName() : inBeanName;
        if (bean instanceof InitializingBean) {
            try {
                ((InitializingBean) bean).afterPropertiesSet();
            } catch (Exception e) {
                throw new BeanCreationException("An exception occurred when using the 'InitializingBean#afterPropertiesSet()' method to initialize the bean named '" + beanInfo + "'", e);
            }
        }
        List<Method> initMethodList = ClassUtils.getMethodByAnnotation(beanClass, PostConstruct.class);
        for (Method initMethod : initMethodList) {
            try {
                invokeMethod(bean, initMethod);
            } catch (Exception e) {
                throw new BeanCreationException("An exception occurs when the bean named '" + beanInfo + "' is initialized using the initialization method '" + initMethod + "()' ", e);
            }
        }
    }

    /**
     * 执行bean的销毁方法
     *
     * @param bean 非空的bean
     */
    public void destroy(@NonNull Object bean) {
        Assert.notNull(bean, "bean is null");
        Class<?> beanClass = bean.getClass();
        String beanInfo = inBeanName == null ? beanClass.getName() : inBeanName;
        if (bean instanceof DisposableBean) {
            try {
                ((DisposableBean) bean).destroy();
            } catch (Exception e) {
                throw new BeanDisposableException("An exception occurred when using the 'DisposableBean#destroy()' destruction method of the bean named '" + beanInfo + "'.", e);
            }
        } else if (bean instanceof Closeable) {
            try {
                ((Closeable) bean).close();
            } catch (Exception e) {
                throw new BeanDisposableException("An exception occurred when using the 'Closeable#close()' destruction method of the bean named '" + beanInfo + "'.", e);
            }
        }
        List<Method> destroyMethodList = ClassUtils.getMethodByAnnotation(beanClass, PreDestroy.class);
        for (Method destroyMethod : destroyMethodList) {
            try {
                invokeMethod(bean, destroyMethod);
            } catch (Exception e) {
                throw new BeanDisposableException("An exception occurred when using the destroy method '" + destroyMethod + "()' in the bean definition. bean: '" + beanInfo + "'", e);
            }
        }
    }


    //---------------------------------------------------------------------------------
    //                 工具方法，当值为null，但是isRequired为true时 抛出异常
    //---------------------------------------------------------------------------------

    private void setBeanFieldValue(Object bean, Field field, Object fieldValue, boolean isRequired) {
        if (fieldValue != null) {
            setFieldValue(bean, field, fieldValue);
        } else if (isRequired) {
            throw new NoSuchBeanDefinitionException(ResolvableType.forField(field, bean.getClass()));
        }
    }

    private Object getMethodArg(Object arg, ResolvableType argType, boolean isRequired) {
        if (arg == null && isRequired) {
            throw new NoSuchBeanDefinitionException(argType);
        }
        return arg;
    }

    private void setFieldValue(Object bean, Field field, Object fieldValue) {
        if (Modifier.isStatic(field.getModifiers())) {
            FieldUtils.setValue(bean.getClass(), field, fieldValue);
        } else {
            FieldUtils.setValue(bean, field, fieldValue);
        }
    }

    private void invokeMethod(Object bean, Method method, Object... methodArgs) {
        if (Modifier.isStatic(method.getModifiers())) {
            MethodUtils.invoke(bean.getClass(), method, methodArgs);
        } else {
            MethodUtils.invoke(bean, method, methodArgs);
        }
    }

    private static Object getFieldValue(StandardBeanExpressionResolver exp, LuckyStandardEnvironment environment, String valueExpression, ResolvableType type) {
        Object value = exp.evaluate(valueExpression);
        if (value instanceof String) {
            value = environment.resolveRequiredPlaceholdersForObject(value);
        }
        return ConversionUtils.conversion(value, type);
    }
}
