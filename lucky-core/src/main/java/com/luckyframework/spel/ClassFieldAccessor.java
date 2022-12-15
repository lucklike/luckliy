package com.luckyframework.spel;

import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.ResolvableType;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

import java.lang.reflect.Field;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/29 12:19
 */
public class ClassFieldAccessor implements PropertyAccessor {
    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class[]{Class.class, Field.class,ResolvableType.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        if(target instanceof ResolvableType){
            return hasField(((ResolvableType)target).getRawClass(), name);
        }
        if(target instanceof Class){
            return hasField((Class<?>) target, name);
        }
        if(target instanceof Field){
            return hasField(((Field)target).getType(), name);
        }
        return false;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        Class<?> aClass;
        if(target instanceof ResolvableType){
            aClass = ((ResolvableType) target).getRawClass();
        } else if(target instanceof Class){
            aClass = (Class<?>) target;
        } else if(target instanceof Field){
            aClass = ((Field)target).getType();
        } else {
            aClass = null;
        }

        if (aClass != null){
            ResolvableType fieldType = getFieldType(aClass, name);
            return new TypedValue(fieldType);
        }
        throw new AccessException("ClassFieldAccessor The value '"+target+"' cannot be resolved");
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        throw new UnsupportedOperationException("Should not be called on an ClassFieldAccessor");
    }


    private boolean hasField(Class<?> aClass, String fieldName){
        Field[] fields = ClassUtils.getAllFields(aClass);
        for (Field field : fields) {
            if(field.getName().equals(fieldName)){
                return true;
            }
        }
        return false;
    }

    private ResolvableType getFieldType(Class<?> aClass, String fieldName) throws AccessException {
        Field[] fields = ClassUtils.getAllFields(aClass);
        for (Field field : fields) {
            if(field.getName().equals(fieldName)){
                return ResolvableType.forField(field);
            }
        }
        throw new AccessException("field named '"+fieldName+"' does not exist in "+aClass);
    }
}
