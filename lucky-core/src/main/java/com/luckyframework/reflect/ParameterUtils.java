package com.luckyframework.reflect;

import org.springframework.util.StringUtils;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * @author fk7075
 * @version 1.0
 * @date 2020/11/16 13:05
 */
public abstract class ParameterUtils {

    public static String getParamName(Parameter param, String paramName){
        Param rp = AnnotationUtils.findMergedAnnotation(param, Param.class);
        if (rp == null) {
            return paramName == null ? param.getName() : paramName;
        }
        return StringUtils.hasText(rp.value()) ? rp.value() : (paramName == null ? param.getName() : paramName);
    }

    /**
     * 获取带有泛型的属性的泛型类型,不是泛型属性返回null
     * @param parameter Parameter对象
     * @return Class[] OR null
     */
    public static Class<?>[] getGenericType(Parameter parameter){
        Type type = parameter.getParameterizedType();
        return ClassUtils.getGenericType(type);
    }

    /**
     * 返回集合参数的泛型,如果不是java自带的类型，会在泛型类型后加上$ref<br>
     * List[String]  ->String<br>
     * Map[String,Ingeger]  ->[String,Integer]<br>
     * Map[String,MyPojo]   ->[String,MyPojo$ref]<br>
     * @param parameter  Parameter对象
     * @return
     */
    public static String[] getStrGenericType(Parameter parameter) {
        Class<?>[] genericClassArray = getGenericType(parameter);
        if(genericClassArray==null) {
            return null;
        }
        String[] gener = new String[genericClassArray.length];
        for (int i = 0; i < gener.length; i++) {
            Class<?> gc=genericClassArray[i];
            if(gc.getClassLoader()!=null) {
                gener[i]=gc.getSimpleName()+"$ref";
            } else {
                gener[i]=gc.getSimpleName();
            }
        }
        return gener;
    }

    /**
     * 参数是否为基本集合类型(泛型为JDK类型的集合)
     * @param parameter Parameter对象
     * @return
     */
    public static boolean isBasicCollection(Parameter parameter){
        if(!Collection.class.isAssignableFrom(parameter.getType())){
            return false;
        }
        Class<?>[] genericClasses=getGenericType(parameter);
        if(genericClasses==null||genericClasses.length!=1) {
            return false;
        }
        Class<?> generic=genericClasses[0];
        return generic.getClassLoader()==null;
    }

    /**
     * 是否为基本数据类型(JDK类型，以及泛型为基本类型的JDK泛型类)
     * @param parameter Parameter对象
     * @return
     */
    public static boolean isBasicSimpleType(Parameter parameter){
        Class<?> fieldClass=parameter.getType();
        if(fieldClass.getClassLoader()!=null) {
            return false;
        }
        Class<?>[] genericTypes = getGenericType(parameter);
        if(genericTypes==null) {
            return true;
        }
        for (Class<?> clzz:genericTypes){
            if(clzz.getClassLoader()!=null) {
                return false;
            }
        }
        return true;
    }

}
