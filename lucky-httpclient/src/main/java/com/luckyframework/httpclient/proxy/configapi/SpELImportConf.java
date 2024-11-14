package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import com.luckyframework.httpclient.proxy.spel.VarScope;
import com.luckyframework.httpclient.proxy.spel.StaticClassEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SpEL变量声明函数导入相关配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/12 01:32
 */
public class SpELImportConf {

    private Map<String, Object> root = new LinkedHashMap<>();
    private Map<String, Object> val = new LinkedHashMap<>();

    /* 字面量，不会进行SpEL运算 */
    private Map<String, Object> rootLit = new LinkedHashMap<>();
    private Map<String, Object> varLit = new LinkedHashMap<>();

    private List<Class<?>> classes = new ArrayList<>();
    private List<String> pack = new ArrayList<>();

    public Map<String, Object> getRoot() {
        return root;
    }

    public void setRoot(Map<String, Object> root) {
        this.root = root;
    }

    public Map<String, Object> getVal() {
        return val;
    }

    public void setVal(Map<String, Object> val) {
        this.val = val;
    }


    public Map<String, Object> getRootLit() {
        return rootLit;
    }

    public void setRootLit(Map<String, Object> rootLit) {
        this.rootLit = rootLit;
    }

    public Map<String, Object> getVarLit() {
        return varLit;
    }

    public void setVarLit(Map<String, Object> varLit) {
        this.varLit = varLit;
    }

    public List<Class<?>> getClasses() {
        return classes;
    }

    public void setClasses(List<Class<?>> classes) {
        this.classes = classes;
    }

    public List<String> getPack() {
        return pack;
    }

    public void setPack(List<String> pack) {
        this.pack = pack;
    }

    public void importSpELRuntime(Context context, VarScope varScope) {
        MapRootParamWrapper contextVar = context.getContextVar();
        for (Class<?> clazz : classes) {
            StaticClassEntry classEntry = StaticClassEntry.create(clazz);
            contextVar.addVariables(classEntry.getAllStaticMethods());

            // 导入变量
            StaticClassEntry.Variable variables = classEntry.getVariablesByScope(varScope);
            // 导入字面量
            contextVar.addRootVariables(variables.getRootVarLitMap());
            contextVar.addVariables(variables.getVarLitMap());

            // 导入Root变量
            variables.getRootVarMap().forEach((k, v) -> {
                String key = context.parseExpression(k);
                Object value = context.getParsedValue(v);
                contextVar.addRootVariable(key, value);
            });

            // 导入普通变量
            variables.getVarMap().forEach((k, v) -> {
                String key = context.parseExpression(k);
                Object value = context.getParsedValue(v);
                contextVar.addVariable(key, value);
            });
        }

        contextVar.addRootVariables(rootLit);
        contextVar.addVariables(varLit);

        for (Map.Entry<String, Object> entry : root.entrySet()) {
            String key = context.parseExpression(entry.getKey(), String.class);
            Object value = context.getParsedValue(entry.getValue());
            contextVar.addRootVariable(key, value);
        }

        for (Map.Entry<String, Object> entry : val.entrySet()) {
            String key = context.parseExpression(entry.getKey(), String.class);
            Object value = context.getParsedValue(entry.getValue());
            contextVar.addVariable(key, value);
        }
        contextVar.importPackage(pack.toArray(new String[0]));
    }
}
