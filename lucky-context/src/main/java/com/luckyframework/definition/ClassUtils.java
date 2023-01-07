package com.luckyframework.definition;

import com.luckyframework.annotations.Autowired;
import com.luckyframework.annotations.Qualifier;
import com.luckyframework.annotations.Value;
import com.luckyframework.bean.factory.BeanReference;
import com.luckyframework.exception.FactoryBeanCreateException;
import com.luckyframework.scanner.ScannerUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/27 下午3:12
 */
public class ClassUtils {

    public static Constructor<?> findConstructor(Class<?> aClass){
        if(aClass.isInterface()){
            throw new FactoryBeanCreateException("'"+aClass+"' is an interface, the constructor of an interface cannot be looked up.");
        }
        Constructor<?>[] constructors = aClass.getDeclaredConstructors();
        List<Constructor<?>> hitConstructors = new ArrayList<>();
        if(constructors.length == 0){
            throw new FactoryBeanCreateException("No constructor available in class '"+aClass+"'.");
        }
        if(constructors.length == 1){
            hitConstructors.add(constructors[0]);
        }else{
            for (Constructor<?> constructorEntry : constructors) {
                if(constructorEntry.isAnnotationPresent(com.luckyframework.annotations.Constructor.class)){
                    hitConstructors.add(constructorEntry);
                }
            }
        }
        if(hitConstructors.size() == 1){
            return hitConstructors.get(0);
        }else if(hitConstructors.size() == 0){
            throw new FactoryBeanCreateException("There are multiple constructors in '"+aClass+"',but none of them are marked with '@Constructor',Lucky does not know which one to use.");
        }else{
            throw new FactoryBeanCreateException("Multiple constructors using '@Constructor' tags were found in '"+aClass+"',and Lucky did not know which to use.");
        }
    }

    public static Class<?>[] findConstructorParameterTypes(Constructor<?> constructor){
        Parameter[] parameters = constructor.getParameters();
        Class<?>[] parameterTypes = new Class[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parameterTypes[i] = parameters[i].getType();
        }
        return parameterTypes;
    }

    public static Object[] findConstructorBeanReferenceParameters(Constructor<?> constructor){
        Parameter[] parameterTypes = constructor.getParameters();
        Object[] parameters = new Object[parameterTypes.length];
        AnnotationMetadata cm = AnnotationMetadata.introspect(constructor.getDeclaringClass());
        int index = 0;
        if(!cm.isIndependent()){
            parameters[index++] = getEnclosingClassParameter(cm);
        }
        for (int i = index; i < parameterTypes.length; i++) {
            Parameter parameterType = parameterTypes[i];
            //Java ID注入
            if(parameterType.isAnnotationPresent(Resource.class)){
                Resource resource = parameterType.getAnnotation(Resource.class);
                parameters[i] = StringUtils.hasText(resource.name())
                        ? BeanReference.builderName(resource.name(),true)
                        : BeanReference.builderType(ResolvableType.forConstructorParameter(constructor,i),true);
            }
            //Lucky ID注入
            else if(parameterType.isAnnotationPresent(Qualifier.class)){
                Qualifier qualifier = parameterType.getAnnotation(Qualifier.class);
                parameters[i] = StringUtils.hasText(qualifier.value())
                        ? BeanReference.builderName(qualifier.value(), qualifier.required())
                        : BeanReference.builderType(ResolvableType.forConstructorParameter(constructor,i), qualifier.required());

            }
            //类型注入
            else if(parameterType.isAnnotationPresent(Autowired.class)){
                Autowired autowired = parameterType.getAnnotation(Autowired.class);
                parameters[i]=BeanReference.builderType(ResolvableType.forConstructorParameter(constructor,i)
                        ,autowired.required());
            }
            //注入环境变量
            else if(parameterType.isAnnotationPresent(Value.class)){
                Value value = parameterType.getAnnotation(Value.class);
                parameters[i] = BeanReference.builderValue(value.value(),ResolvableType.forConstructorParameter(constructor,i));
            }
            //没有注解，按照类型注入
            else{
                parameters[i] = BeanReference.builderType(ResolvableType.forConstructorParameter(constructor,i),false);
            }
            if(parameters[i] instanceof BeanReference){
                BeanReferenceUtils.setLazyProperty(constructor,parameterType,(BeanReference)parameters[i]);
            }
        }
        return parameters;
    }



    public static Object getEnclosingClassParameter(ClassMetadata cm){
        String enclosingClassName = cm.getEnclosingClassName();
        Class<?> enclosingClass = com.luckyframework.reflect.ClassUtils.forName(enclosingClassName, com.luckyframework.reflect.ClassUtils.getDefaultClassLoader());
        return BeanReference.builderName(ScannerUtils.getScannerElementName(enclosingClass));
    }

    public static Object[] getMethodBeanReferenceParameters(Method method){
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Parameter[] methodParameters = method.getParameters();
        Object[] parameters = new Object[genericParameterTypes.length];
        for (int i = 0; i < genericParameterTypes.length; i++) {
            Type genericParameterType = genericParameterTypes[i];
            Parameter methodParameter = methodParameters[i];
            //Java ID注入
            if(methodParameter.isAnnotationPresent(Resource.class)){
                Resource resource = methodParameter.getAnnotation(Resource.class);
                parameters[i] = StringUtils.hasText(resource.name())
                        ? BeanReference.builderName(resource.name(),true)
                        : BeanReference.builderType(ResolvableType.forType(genericParameterType),true);
            }
            //Lucky ID注入
            else if(methodParameter.isAnnotationPresent(Qualifier.class)){
                Qualifier qualifier = methodParameter.getAnnotation(Qualifier.class);
                parameters[i] = StringUtils.hasText(qualifier.value())
                        ? BeanReference.builderName(qualifier.value(),qualifier.required())
                        : BeanReference.builderType(ResolvableType.forType(genericParameterType),qualifier.required());
            }
            //类型注入
            else if(methodParameter.isAnnotationPresent(Autowired.class)){
                Autowired autowired = methodParameter.getAnnotation(Autowired.class);
                parameters[i] = BeanReference.builderType(ResolvableType.forType(genericParameterType),autowired.required());
            }
            //注入环境变量
            else if(methodParameter.isAnnotationPresent(Value.class)){
                Value value = methodParameter.getAnnotation(Value.class);
                parameters[i] = BeanReference.builderValue(value.value(),ResolvableType.forType(genericParameterType));
            }
            //没有注解，按照类型注入
            else{
                parameters[i] = BeanReference.builderType(ResolvableType.forType(genericParameterType),false);
            }
            if(parameters[i] instanceof BeanReference){
                BeanReferenceUtils.setLazyProperty(method,methodParameter,(BeanReference)parameters[i]);
            }
        }
        return parameters;
    }
}
