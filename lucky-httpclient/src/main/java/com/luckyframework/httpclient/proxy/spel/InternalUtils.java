package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class InternalUtils {


    /**
     * 获取所有内部变量名称
     *
     * @return 所有内部变量名称
     */
    public static Set<String> getInternalVarName(Class<?> clazz) {
        // 内部变量名
        Set<String> internalParamNameSet = new HashSet<>();
        Field[] fields = ClassUtils.getAllFields(clazz);
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Object value = FieldUtils.getValue(null, field);
            if (value instanceof String) {
                internalParamNameSet.add((String) value);
            }
        }
        return internalParamNameSet;
    }
}
