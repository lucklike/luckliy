package com.luckyframework.reflect;

import com.luckyframework.exception.LuckyReflectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 * 属性反射工具类
 */
public abstract class FieldUtils {

    private static final Logger log= LoggerFactory.getLogger(FieldUtils.class);

    /**
     * 属性是否为数组
     * @param field Field对象
     * @return
     */
    public static boolean isArray(Field field){
        return field.getType().isArray();
    }

    /**
     * 属性是否为集合
     * @param field Field对象
     * @return
     */
    public static boolean isCollection(Field field){
        return Collection.class.isAssignableFrom(field.getType());
    }

    /**
     * 属性是否为Map
     * @param field Field对象
     * @return
     */
    public static boolean isMap(Field field){
        return Map.class.isAssignableFrom(field.getType());
    }

    /**
     * 获取带有泛型的属性的泛型类型,不是泛型属性返回null
     * @param field Field对象
     * @return Class[] OR null
     */
    public static Class<?>[] getGenericType(Field field){
        Type type = field.getGenericType();
        return ClassUtils.getGenericType(type);
    }

    /**
     * 返回集合属性的泛型,如果不是java自带的类型，会在泛型类型后加上$ref<br>
     * List[String]  ->String<br>
     * Map[String,Ingeger]  ->[String,Integer]<br>
     * Map[String,MyPojo]   ->[String,MyPojo$ref]<br>
     *
     * @param field
     * @return
     */
    public static String[] getStrGenericType(Field field) {
        Class<?>[] genericClassArray = getGenericType(field);
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
     * 属性是否为基本集合类型(泛型为JDK类型的集合)
     * @param field Field对象
     * @return
     */
    public static boolean isBasicCollection(Field field){
        Class<?>[] genericClasses=getGenericType(field);
        if(genericClasses==null||genericClasses.length!=1) {
            return false;
        }
        Class<?> generic=genericClasses[0];
        return generic.getClassLoader()==null;
    }

    /**
     * 是否为基本数据类型(JDK类型，以及泛型为基本类型的JDK泛型类)
     * @param field
     * @return
     */
    public static boolean isBasicSimpleType(Field field){
        Class<?> fieldClass=field.getType();
        if(fieldClass.getClassLoader()!=null) {
            return false;
        }
        Class<?>[] genericTypes = getGenericType(field);
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

    public static Field getField(Class<?> clzz,String name){
        try {
            return clzz.getField(name);
        } catch (NoSuchFieldException e) {
            throw new LuckyReflectionException(e);
        }
    }

    public static Field getDeclaredField(Class<?> clzz,String name){
        try {
            return clzz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new LuckyReflectionException(e);
        }
    }

    /**
     * 反射机制获取Field的值
     * @param fieldObject 目标对象
     * @param field Field对象
     * @return
     */
    public static Object getValue(Object fieldObject,Field field){
        try {
            field.setAccessible(true);
            return field.get(fieldObject);
        } catch (IllegalAccessException e) {
            throw new LuckyReflectionException(e);
        }
    }

    /**
     * 反射机制获取Field的值
     * @param fieldObject 目标对象
     * @param fieldName 属性名
     * @return
     */
    public static Object getValue(Object fieldObject, String fieldName){
        Assert.notNull(fieldObject, "fieldObject is null");
        return getValue(fieldObject, getDeclaredField(fieldObject.getClass(), fieldName));
    }

    /**
     * 反射机制设置Field的值
     * @param fieldObject
     * @param field
     */
    public static void setValue(Object fieldObject,Field field,Object fieldValue){
        try {
            field.setAccessible(true);
            field.set(fieldObject,fieldValue);
        } catch (IllegalAccessException e) {
            throw new LuckyReflectionException(e);
        }
    }

    /**
     * 判断目标类型是否为属性类型的子类
     * @param field Field对象
     * @param targetClass 目标类型
     * @return
     */
    public static boolean isSubclass(Field field,Class<?> targetClass){
        return field.getType().isAssignableFrom(targetClass);
    }

    /**
     * 判断目标类型是否为属性类型的父类
     * @param field Field对象
     * @param targetClass 目标类型
     * @return
     */
    public static boolean isParentClass(Field field,Class<?> targetClass){
        return targetClass.isAssignableFrom(field.getType());
    }

    /**
     * 目标对象是否属于属性对应的类型
     * @param field Field对象
     * @param targetObject 目标对象
     * @return
     */
    public static boolean instanceOf(Field field,Object targetObject){
        return isSubclass(field,targetObject.getClass());
    }

    /**
     * 是否为可运算的属性(int double boolean)
     * @param field
     * @return
     */
    public static boolean isCanOperation(Field field){
        Class<?> t = field.getType();
        if(t==int.class||t==Integer.class||
          t==boolean.class||t==Boolean.class||
          t==float.class||t==Float.class||
          t==double.class||t==Double.class) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param field
     * @return
     */
    public static boolean isJDKType(Field field){
        return field.getType().getClassLoader()==null;
    }

}
