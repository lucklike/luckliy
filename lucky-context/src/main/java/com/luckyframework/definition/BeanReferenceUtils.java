package com.luckyframework.definition;

import com.luckyframework.annotations.Lazy;
import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.bean.factory.BeanReference;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.environment.LuckyStandardEnvironment;
import com.luckyframework.exception.NoSuchBeanDefinitionException;
import com.luckyframework.expression.StandardBeanExpressionResolver;
import com.luckyframework.proxy.CglibObjectCreator;
import com.luckyframework.proxy.ProxyFactory;
import org.springframework.cglib.proxy.Dispatcher;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

import static com.luckyframework.scanner.Constants.LAZY_ANNOTATION_NAME;

/**
 * bean引用相关的工具类
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/10 下午11:30
 */
public class BeanReferenceUtils {

    private final static Object[] NULL_PARAMETER = new Object[0];

    private static StandardBeanExpressionResolver beanExpressionResolver;

    public static StandardBeanExpressionResolver getBeanExpressionResolver(BeanFactory beanFactory, Environment environment){
        if(beanExpressionResolver == null){
            beanExpressionResolver = new StandardBeanExpressionResolver();
            beanExpressionResolver.initializeStandardEvaluationContext(beanFactory, environment);
        }
        return beanExpressionResolver;
    }

    /**
     * 将输入参数转化为对应的真实值
     * @param beanFactory bean工厂
     * @param environment 环境变量
     * @param inputParameter 输入参数
     * @return 真实值数组
     */
    @NonNull
    public static Object[] getRealParameterValues(BeanFactory beanFactory, Environment environment, Object[] inputParameter){
        return getRealParameterValues(beanFactory, environment, inputParameter,true);
    }

    /**
     * 将输入参数转化为可能是懒加载的实参数值
     * @param beanFactory       bean工厂
     * @param environment       环境变量
     * @param inputParameter    输入参数
     * @return 可能是懒加载的真实值数组
     */
    @NonNull
    public static Object[] getMayBeLazyRealParameterValues(BeanFactory beanFactory, Environment environment,Object[] inputParameter){
        return getRealParameterValues(beanFactory, environment, inputParameter,false);
    }

    @NonNull
    public static Object[] getRealParameterValues(BeanFactory beanFactory, Environment environment,Object[] inputParameter,boolean ignoreLazy){
        if (inputParameter == null){
            return NULL_PARAMETER;
        }
        Object[] realValues = new Object[inputParameter.length];
        int i = 0;
        for (Object param : inputParameter) {
            if(param instanceof BeanReference){
                BeanReference beanReference = (BeanReference) param;
                if(!ignoreLazy && beanReference.isLazy()){
                    realValues[i++] = beanReferenceToLazyRealObject((BeanReference) param,beanFactory,environment);
                }else{
                    realValues[i++] = beanReferenceToRealObject((BeanReference) param,beanFactory,environment);
                }
            }else{
                realValues[i++]  =  param;
            }
        }
        return realValues;
    }

    /**
     * 将一个Bean引用转化为懒加载的Bean对象
     * @param beanReference Bean引用
     * @param beanFactory   Bean工厂
     * @param environment   环境变量
     * @return Bean引用指向的懒加载对象
     */
    public static Object beanReferenceToLazyRealObject(BeanReference beanReference,BeanFactory beanFactory, Environment environment){
        Class<?> targetClass = beanReference.getType().getRawClass();
        Constructor<?> constructor = ClassUtils.findConstructor(Objects.requireNonNull(targetClass));
        Class<?>[] constructorParameterTypes = ClassUtils.findConstructorParameterTypes(constructor);
        Object[] nullElementArray = new Object[constructorParameterTypes.length];
        Dispatcher dispatcher = () -> beanReferenceToRealObject(beanReference,beanFactory,environment);
        CglibObjectCreator creator = (en)-> en.create(constructorParameterTypes,nullElementArray);
        return ProxyFactory.getCglibProxyObject(targetClass,creator,dispatcher);
    }

    /**
     * 将一个Bean引用转化为真实的Bean对象
     * @param beanReference Bean引用
     * @param beanFactory   Bean工厂
     * @param environment   环境变量
     * @return Bean引用指向的真实对象
     */
    public static Object beanReferenceToRealObject(BeanReference beanReference,BeanFactory beanFactory, Environment environment){

        Object realValue;
        //ID查找
        if(beanReference.isByName()){
            realValue = beanFactory.getBean(beanReference.getBeanName());

        }
        // 名称优先
        else if (beanReference.isAutoNameFirst()) {
            realValue = beanFactory.containsBean(beanReference.getBeanName())
                    ? beanFactory.getBean(beanReference.getBeanName())
                    : beanFactory.getBean(beanReference.getType());
        }
        // 类型优先
        else if (beanReference.isAutoTypeFirst()) {
            try {
                realValue = beanFactory.getBean(beanReference.getType());
            }catch (Exception e){
                String beanName = beanReference.getBeanName();;
                if(beanFactory.containsBean(beanName)){
                    realValue = beanFactory.getBean(beanName);
                }else{
                    throw e;
                }
            }
        }
        //类型查找
        else if (beanReference.isByType()){
            realValue = beanFactory.getBean(beanReference.getType());
        }
        //环境变量或SpEL表达式
        else{
            try {
                StandardBeanExpressionResolver exp = getBeanExpressionResolver(beanFactory, environment);
                realValue = getFieldValue(exp, (LuckyStandardEnvironment) environment, beanReference.getBeanName(), beanReference.getType());
            }catch (IllegalArgumentException e) {
                throw new NoSuchBeanDefinitionException(beanReference.getType(), e.getMessage());
            }
        }
        if(realValue == null && beanReference.isRequired()){
            throw new NoSuchBeanDefinitionException(beanReference.getType());
        }
        return realValue;
    }

    public static void setLazyProperty(Constructor<?> constructor, Parameter parameter, BeanReference beanReference){
        Lazy lazy = AnnotatedElementUtils.isAnnotated(parameter,LAZY_ANNOTATION_NAME)
                ? AnnotatedElementUtils.findMergedAnnotation(parameter,Lazy.class)
                : AnnotatedElementUtils.findMergedAnnotation(constructor,Lazy.class);
        if(lazy != null){
            beanReference.setLazy(lazy.value());
        }
    }

    public static void setLazyProperty(Field field, BeanReference beanReference){
        Lazy lazy = AnnotatedElementUtils.findMergedAnnotation(field, Lazy.class);
        if(lazy != null){
            beanReference.setLazy(lazy.value());
        }
    }

    public static void setLazyProperty(Method method, Parameter parameter, BeanReference beanReference){
        Lazy lazy = AnnotatedElementUtils.isAnnotated(parameter,LAZY_ANNOTATION_NAME)
                ? AnnotatedElementUtils.findMergedAnnotation(parameter,Lazy.class)
                : AnnotatedElementUtils.findMergedAnnotation(method,Lazy.class);
        if(lazy != null){
            beanReference.setLazy(lazy.value());
        }
    }

    public static Object getFieldValue(StandardBeanExpressionResolver exp, LuckyStandardEnvironment environment, String valueExpression, ResolvableType type){
        Object value = exp.evaluate(valueExpression);
        if(value instanceof String){
            value = environment.resolveRequiredPlaceholdersForObject(value);
        }
        return ConversionUtils.conversion(value, type);
    }
}
