package com.luckyframework.bean.factory;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;


/**
 * 构造器工厂
 * @author fk7075
 * @version 1.0.0
 * @date 2021/6/28 下午10:59
 */
public class ConstructorFactoryBean extends AbstractFactoryBean {

    /** bean的Class*/
    private final Class<?> beanClass;
    /** bean的构造器*/
    private Constructor<?> constructor;

    /**
     * 构造器工厂构造函数
     * @param fullClassName 目标类的全路径
     * @param loader        类加载器
     * @param parameters    构造器参数
     */
    public ConstructorFactoryBean(@NonNull String fullClassName, ClassLoader loader, Object[] parameters){
        if(StringUtils.hasText(fullClassName)){
            throw new IllegalArgumentException("fullClassName is required");
        }
        loader = loader == null ? Thread.currentThread().getContextClassLoader() : loader;
        this.beanClass = ClassUtils.forName(fullClassName,loader);
        this.parameters = parameters;
    }

    /**
     * 构造器工厂构造函数(默认使用线程上下文类加载器)
     * @param fullClassName  目标类的全路径
     * @param parameters     构造器参数
     */
    public ConstructorFactoryBean(@NonNull String fullClassName,Object[] parameters){
        this(fullClassName,null,parameters);
    }

    /**
     * 构造器工厂构造函数(默认使用线程上下文类加载器和无参构造器)
     * @param fullClassName  目标类的全路径
     */
    public ConstructorFactoryBean(@NonNull String fullClassName){
        this(fullClassName,null);
    }

    /**
     * 构造器工厂构造函数
     * @param beanClass   目标类的Class
     * @param parameters  构造器的参数
     */
    public ConstructorFactoryBean(@NonNull Class<?> beanClass,Object[] parameters){
        Assert.notNull(beanClass,"");
        this.beanClass = beanClass;
        this.parameters = parameters;
    }

    /**
     * 构造器工厂构造函数(默认使用无参构造)
     * @param beanClass   目标类的Class
     */
    public ConstructorFactoryBean(@NonNull Class<?> beanClass){
        this(beanClass,null);
    }

    public ConstructorFactoryBean(@NonNull Constructor<?> constructor,Object[] parameters){
        this.constructor = constructor;
        this.parameters = parameters;
        this.beanClass = constructor.getDeclaringClass();
    }

    @Override
    public Object createBean() {
        Constructor<?> constructor = findConstructor();
        return ClassUtils.newObject(constructor,getRealParameterValues());
    }

    @Override
    public ResolvableType[] createDependOnTypes() {
        Type[] genericParameterTypes = findConstructor().getGenericParameterTypes();
        if(ContainerUtils.isEmptyArray(genericParameterTypes)){
            return super.createDependOnTypes();
        }
        ResolvableType[] resolvableTypes = new ResolvableType[genericParameterTypes.length];
        for (int i = 0; i < genericParameterTypes.length; i++) {
            resolvableTypes[i] = ResolvableType.forType(genericParameterTypes[i]);
        }
        return resolvableTypes;
    }

    @NonNull
    private Constructor<?> findConstructor(){
        if(constructor == null){
            constructor = ClassUtils.findConstructor(beanClass, getParameterResolvableTypes());
            constructor.setAccessible(true);
        }
        return constructor;
    }


    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Override
    public ResolvableType getResolvableType() {
        return ResolvableType.forClass(beanClass);
    }

    @Override
    public Class<?>[] getParameterClasses() {
        return parameterValueToClasses(parameters);
    }

    @Override
    public ResolvableType[] getParameterResolvableTypes() {
        return parameterValueToResolvableTypes(parameters);
    }

    @Override
    public Object[] getRealParameterValues() {
        return getRealParameterValues(parameters);
    }
}
