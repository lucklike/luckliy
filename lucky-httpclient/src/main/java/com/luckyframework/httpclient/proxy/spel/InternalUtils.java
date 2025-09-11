package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class InternalUtils {


    /**
     * 获取所有内部变量名称
     *
     * @return 所有内部变量名称
     */
    public static List<String> getInternalVarName(Class<?> clazz) {
        // 内部变量名
        List<String> internalParamNameList = new ArrayList<>();
        List<Field> fields = ClassUtils.getAllStaticFieldOrder(clazz);
        for (Field field : fields) {
            if (!Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            if (field.getType() != String.class) {
                continue;
            }

            Object value = FieldUtils.getValue(null, field);
            if (value instanceof String) {
                internalParamNameList.add((String) value);
            }
        }
        return internalParamNameList;
    }
}
