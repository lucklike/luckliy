package com.luckyframework.validator.annotations;

import com.luckyframework.reflect.AnnotationUtils;

import javax.validation.Valid;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/21 17:18
 */
public class ValidationAnnotationUtils {

    private static final Class<?>[] EMPTY_OBJECT_ARRAY = new Class[0];

    public static boolean isValidated(AnnotatedElement  annotatedElement){
        return AnnotationUtils.isAnnotated(annotatedElement, Validated.class) || AnnotationUtils.isAnnotated(annotatedElement, Valid.class);
    }

    public static Class<?>[] getGroupClasses(Class<?> clazz, Method method){
        Validated methodValidated = AnnotationUtils.findMergedAnnotation(method, Validated.class);
        if(methodValidated != null){
            return methodValidated.value();
        }

        Validated classValidated = AnnotationUtils.findMergedAnnotation(clazz, Validated.class);
        if(classValidated != null){
            return classValidated.value();
        }

        return EMPTY_OBJECT_ARRAY;
    }

}
