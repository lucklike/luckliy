package com.luckyframework.bean.factory;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.exception.FactoryBeanCreateException;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * 静态方法工厂
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/1 下午10:30
 */
public class StaticMethodFactoryBean extends AbstractFactoryBean {

    /** bean的Class*/
    private final Class<?> beanClass;
    /** 静态方法的名字*/
    private String staticMethodName;
    /** 静态方法实例*/
    private Method staticMethod;

    /**
     *  静态方法工厂构造器
     * @param fullClassName    beanClass的全路径
     * @param loader           使用的类加载器
     * @param staticMethodName 静态方法名
     * @param parameters       静态方法执行时的参数
     */
    public StaticMethodFactoryBean(@NonNull String fullClassName, ClassLoader loader, @NonNull String staticMethodName, Object[] parameters){
        loader = loader == null ? Thread.currentThread().getContextClassLoader() : loader;
        this.beanClass = ClassUtils.forName(fullClassName,loader);
        this.staticMethodName = staticMethodName;
        this.parameters = parameters;
    }

    /**
     * 静态方法工厂构造器
     * @param fullClassName    beanClass的全路径
     * @param staticMethodName 静态方法名
     * @param parameters       静态方法执行时的参数
     */
    public StaticMethodFactoryBean(@NonNull String fullClassName,@NonNull String staticMethodName,Object[] parameters){
        this(fullClassName,null,staticMethodName,parameters);
    }

    /**
     * 静态方法执行时的参数
     * @param fullClassName beanClass的全路径
     * @param loader        使用的类加载器
     * @param staticMethod  静态方法实例
     * @param parameters    静态方法执行时的参数
     */
    public StaticMethodFactoryBean(@NonNull String fullClassName,ClassLoader loader,@NonNull Method staticMethod,Object[] parameters){
        loader = loader == null ? Thread.currentThread().getContextClassLoader() : loader;
        this.beanClass = ClassUtils.forName(fullClassName,loader);
        this.staticMethod = staticMethod;
        this.parameters = parameters;
    }

    /**
     * 静态方法执行时的参数
     * @param fullClassName beanClass的全路径
     * @param staticMethod  静态方法实例
     * @param parameters    静态方法执行时的参数
     */
    public StaticMethodFactoryBean(@NonNull String fullClassName,@NonNull Method staticMethod,Object[] parameters){
        this(fullClassName,null,staticMethod,parameters);
    }

    /**
     * 静态方法执行时的参数
     * @param beanClass        bean的Class
     * @param staticMethodName 静态方法名
     * @param parameters       静态方法执行时的参数
     */
    public StaticMethodFactoryBean(@NonNull Class<?> beanClass,String staticMethodName,Object[] parameters){
        this.beanClass = beanClass;
        this.staticMethodName = staticMethodName;
        this.parameters = parameters;
    }

    /**
     * 静态方法执行时的参数
     * @param beanClass    bean的Class
     * @param staticMethod 静态方法实例
     * @param parameters   静态方法执行时的参数
     */
    public StaticMethodFactoryBean(@NonNull Class<?> beanClass,Method staticMethod,Object[] parameters){
        if(!Modifier.isStatic(staticMethod.getModifiers())){
            throw new FactoryBeanCreateException("You must use static method instances to build 'StaticMethodFactoryBean'");
        }
        this.beanClass = beanClass;
        this.staticMethod = staticMethod;
        this.parameters = parameters;
    }

    @Override
    public Object createBean() {
        return MethodUtils.invoke(beanClass,findStaticMethod(),getRealParameterValues());
    }

    @Override
    public ResolvableType getResolvableType() {
        return ResolvableType.forMethodReturnType(findStaticMethod(),beanClass);
    }

    @Override
    public ResolvableType[] createDependOnTypes() {
        Type[] genericParameterTypes = findStaticMethod().getGenericParameterTypes();
        if(ContainerUtils.isEmptyArray(genericParameterTypes)){
            return super.createDependOnTypes();
        }

        ResolvableType[] resolvableTypes = new ResolvableType[genericParameterTypes.length];
        for (int i = 0; i < genericParameterTypes.length; i++) {
            resolvableTypes[i] = ResolvableType.forType(genericParameterTypes[i]);
        }
        return resolvableTypes;
    }

    public Method findStaticMethod(){
        if(staticMethod == null){
            staticMethod = ClassUtils.findStaticMethod(beanClass, staticMethodName, getParameterResolvableTypes());
        }
        return staticMethod;
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
