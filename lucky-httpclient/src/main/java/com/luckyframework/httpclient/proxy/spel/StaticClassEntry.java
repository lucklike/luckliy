package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.StringUtils;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 静态工具类实例，用于从某个工具类中提取出所有的公有静态方法实例
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/14 04:21
 */
public class StaticClassEntry {


    private static final Logger log = LoggerFactory.getLogger(StaticClassEntry.class);

    /**
     * 工具类Class
     */
    private Class<?> clazz;

    /**
     * 命名空间
     */
    private String namespace;

    public static StaticClassEntry create(String namespace, Class<?> clazz) {
        StaticClassEntry entry = new StaticClassEntry();
        if (!StringUtils.hasText(namespace)) {
            FunctionNamespace prefixAnn = AnnotationUtils.findMergedAnnotation(clazz, FunctionNamespace.class);
            if (prefixAnn != null && StringUtils.hasText(prefixAnn.value())) {
                namespace = prefixAnn.value();
            }
        }
        entry.setNamespace(namespace);
        entry.setClazz(clazz);
        return entry;
    }

    public static StaticClassEntry create(Class<?> clazz) {
        return create(null, clazz);
    }

    /**
     * 获取工具类的Class
     *
     * @return 工具类的Class
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * 设置工具类Class
     *
     * @param clazz 工具类Class
     */
    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * 获取方法的命名空间
     *
     * @return 方法名前缀
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * 设置方法的命名空间
     *
     * @param namespace 命名空间
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * 获取所有公有静态方法名和方法实例所组成的Map
     *
     * @return 所有公有静态方法名和方法实例所组成的Map
     */
    public Map<String, Object> getAllStaticMethods() {
        Assert.notNull(clazz, "clazz cannot be null");
        List<Method> allStaticMethod = ClassUtils.getAllStaticMethod(clazz);

        Map<String, Object> methodMap = new HashMap<>();
        for (Method method : allStaticMethod) {
            if (!ClassUtils.isPublicMethod(method)) {
                continue;
            }
            if (AnnotationUtils.isAnnotated(method, FunctionFilter.class)) {
                continue;
            }

            String methodName = getMethodName(method);
            if (methodMap.containsKey(methodName)) {
                throw new SpELFunctionRegisterException("There are several static methods named '{}' in class '{}', It is recommended to declare an alias for the method using the '@FunctionAlias' annotation.", methodName, method.getDeclaringClass().getName()).printException(log);
            }
            methodMap.put(methodName, method);
        }

        return methodMap;
    }

    /**
     * 获取指定作用域的所有变量
     *
     * @param varScope 作用域
     * @return 所有变量
     */
    public Variable getVariablesByScope(VarScope varScope) {
        Assert.notNull(clazz, "clazz cannot be null");
        Variable variable = new Variable();
        Field[] allFields = ClassUtils.getAllFields(clazz);
        for (Field field : allFields) {

            // 过滤调非静态属性
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            // 过滤掉未被@VarName系列注解标注以及作用域不满足的属性
            VarName varNameAnn = AnnotationUtils.findMergedAnnotation(field, VarName.class);
            if (varNameAnn == null) {
                continue;
            }

            if (varScope != null && varNameAnn.scope() != varScope) {
                continue;
            }

            String fieldName = getFieldName(field);
            Object fieldValue = FieldUtils.getValue(clazz, field);

            if (AnnotationUtils.isAnnotated(field, RootVar.class)) {
                if (varNameAnn.literal()) {
                    variable.addRootVarLit(fieldName, fieldValue);
                } else {
                    variable.addRootVar(fieldName, fieldValue);
                }
            } else if (AnnotationUtils.isAnnotated(field, Var.class)) {
                if (varNameAnn.literal()) {
                    variable.addVarLit(fieldName, fieldValue);
                } else {
                    variable.addVar(fieldName, fieldValue);
                }
            }
        }
        return variable;
    }

    /**
     * 获取所有作用域为METHOD的变量
     *
     * @return 所有作用域为METHOD变量
     */
    public Variable getMethodScopeVariables() {
        return getVariablesByScope(VarScope.METHOD);
    }

    /**
     * 获取所有作用域为CLASS的变量
     *
     * @return 所有作用域为CLASS变量
     */
    public Variable getClassScopeVariables() {
        return getVariablesByScope(VarScope.CLASS);
    }

    /**
     * 获取所有作用域的变量
     *
     * @return 所有变量
     */
    public Variable getAllVariables() {
        return getVariablesByScope(null);
    }

    /**
     * 获取方法名称（命名空间_+函数名）
     *
     * @param method 方法实例
     * @return 方法名称
     */
    private String getMethodName(Method method) {
        String methodName = FunctionAlias.MethodNameUtils.getMethodName(method);
        return StringUtils.hasText(namespace) ? namespace + "_" + methodName : methodName;
    }

    /**
     * 获取方法名称（命名空间_+属性名）
     *
     * @param field 属性实例
     * @return 属性名
     */
    private String getFieldName(Field field) {
        String fieldName = VarName.FieldNameUtils.getVarName(field);
        return StringUtils.hasText(namespace) ? namespace + "_" + fieldName : fieldName;
    }

    public static class Variable {
        private final Map<String, Object> rootVarMap = new ConcurrentHashMap<>(8);
        private final Map<String, Object> rootVarLitMap = new ConcurrentHashMap<>(8);
        private final Map<String, Object> varMap = new ConcurrentHashMap<>(8);
        private final Map<String, Object> varLitMap = new ConcurrentHashMap<>(8);

        public void addRootVar(String name, Object value) {
            rootVarMap.put(name, value);
        }

        public void addRootVarLit(String name, Object value) {
            rootVarLitMap.put(name, value);
        }

        public void addVar(String name, Object value) {
            varMap.put(name, value);
        }

        public void addVarLit(String name, Object value) {
            varLitMap.put(name, value);
        }

        public Map<String, Object> getRootVarMap() {
            return rootVarMap;
        }

        public Map<String, Object> getRootVarLitMap() {
            return rootVarLitMap;
        }

        public Map<String, Object> getVarMap() {
            return varMap;
        }

        public Map<String, Object> getVarLitMap() {
            return varLitMap;
        }
    }
}
