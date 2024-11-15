package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.spel.var.VarScope;
import com.luckyframework.httpclient.proxy.spel.var.VarType;
import com.luckyframework.httpclient.proxy.spel.var.Variate;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            Namespace prefixAnn = AnnotationUtils.findMergedAnnotation(clazz, Namespace.class);
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
     * @param varScopes 作用域集合
     * @return 所有变量
     */
    public Variable getVariablesByScopes(VarScope... varScopes) {
        Assert.notNull(clazz, "clazz cannot be null");
        Variable variable = new Variable(namespace);
        Field[] allFields = ClassUtils.getAllFields(clazz);
        for (Field field : allFields) {

            // 过滤调非静态属性
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            // 过滤掉未被@Variate系列注解标注以及作用域不满足的属性
            Variate variateAnn = AnnotationUtils.findMergedAnnotation(field, Variate.class);
            if (variateAnn == null || ContainerUtils.notInArrays(varScopes, variateAnn.scope())) {
                continue;
            }

            // 注解配置
            boolean unfold = variateAnn.unfold();
            boolean literal = variateAnn.literal();
            VarType type = variateAnn.type();

            // 获取变量名和变量值
            String fieldName = Variate.FieldNameUtils.getVarName(field);
            Object fieldValue = FieldUtils.getValue(clazz, field);

            if (type == VarType.ROOT) {
                if (unfold) {
                    variable.addRootVariableMap(varUnfold(fieldName, fieldValue), literal);
                } else {
                    variable.addRootVariable(fieldName, fieldValue, literal);
                }
            } else if (type == VarType.NORMAL) {
                if (unfold) {
                    variable.addVariableMap(varUnfold(fieldName, fieldValue), literal);
                } else {
                    variable.addVariable(fieldName, fieldValue, literal);
                }
            }
        }
        return variable;
    }

    /**
     * 变量展开后的Map
     *
     * @param fieldName 属性名
     * @param obj       变量
     * @return 变量展开后的Map
     */
    private Map<String, Object> varUnfold(String fieldName, Object obj) {
        try {
            return ConversionUtils.conversion(obj, new SerializationTypeToken<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new VarUnfoldException(e, "An exception occurred when expanding the attribute '{}' of class '{}' to Map", fieldName, clazz.getName());
        }
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

    public static class Variable {
        private final String namespace;
        private final Map<String, Object> rootVarMap = new LinkedHashMap<>(8);
        private final Map<String, Object> rootVarLitMap = new LinkedHashMap<>(8);
        private final Map<String, Object> varMap = new LinkedHashMap<>(8);
        private final Map<String, Object> varLitMap = new LinkedHashMap<>(8);

        public Variable(String namespace) {
            this.namespace = namespace;
        }

        public void addRootVariable(String name, Object value, boolean literal) {
            if (literal) {
                addRootVarLit(name, value);
            } else {
                addRootVar(name, value);
            }
        }

        public void addVariable(String name, Object value, boolean literal) {
            if (literal) {
                addVarLit(name, value);
            } else {
                addVar(name, value);
            }
        }

        public void addRootVariableMap(Map<String, Object> varMap, boolean literal) {
            if (literal) {
                addRootVarLitMap(varMap);
            } else {
                addRootVarMap(varMap);
            }
        }

        public void addVariableMap(Map<String, Object> varMap, boolean literal) {
            if (literal) {
                addVarLitMap(varMap);
            } else {
                addVarMap(varMap);
            }
        }

        public void addRootVar(String name, Object value) {
            rootVarMap.put(name, value);
        }

        public void addRootVarMap(Map<String, Object> varMap) {
            rootVarMap.putAll(varMap);
        }

        public void addRootVarLit(String name, Object value) {
            rootVarLitMap.put(name, value);
        }

        public void addRootVarLitMap(Map<String, Object> varMap) {
            rootVarLitMap.putAll(varMap);
        }

        public void addVar(String name, Object value) {
            varMap.put(name, value);
        }

        public void addVarMap(Map<String, Object> varMap) {
            this.varMap.putAll(varMap);
        }

        public void addVarLit(String name, Object value) {
            varLitMap.put(name, value);
        }

        public void addVarLitMap(Map<String, Object> varMap) {
            varLitMap.putAll(varMap);
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

        public String getNamespace() {
            return namespace;
        }

        public boolean hasNamespace() {
            return StringUtils.hasText(namespace);
        }

        public void importToContext(Context context) {
            MapRootParamWrapper contextVar = context.getContextVar();
            if (hasNamespace()) {

                Map<String, Object> rootVarMap = new LinkedHashMap<>(getRootVarLitMap());
                Map<String, Object> varMap = new LinkedHashMap<>(getVarLitMap());

                contextVar.addRootVariable(namespace, rootVarMap);
                contextVar.addVariable(namespace, varMap);

                // 导入Root变量
                getRootVarMap().forEach((k, v) -> {
                    String key = context.parseExpression(k);
                    Object value = context.getParsedValue(v);
                    rootVarMap.put(key, value);
                });

                // 导入普通变量
                getVarMap().forEach((k, v) -> {
                    String key = context.parseExpression(k);
                    Object value = context.getParsedValue(v);
                    varMap.put(key, value);
                });

            } else {

                contextVar.addRootVariables(getRootVarLitMap());
                contextVar.addVariables(getVarLitMap());

                // 导入Root变量
                getRootVarMap().forEach((k, v) -> {
                    String key = context.parseExpression(k);
                    Object value = context.getParsedValue(v);
                    contextVar.addRootVariable(key, value);
                });

                // 导入普通变量
                getVarMap().forEach((k, v) -> {
                    String key = context.parseExpression(k);
                    Object value = context.getParsedValue(v);
                    contextVar.addVariable(key, value);
                });
            }
        }
    }
}
