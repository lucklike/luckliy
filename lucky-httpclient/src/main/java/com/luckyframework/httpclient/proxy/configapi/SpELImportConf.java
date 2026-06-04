package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.spel.ClassStaticElement;
import com.luckyframework.httpclient.proxy.spel.SpELVariate;

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

    /** Root级别动态变量（变量名和值都经过SpEL运算后导入） */
    private Map<String, Object> root = new LinkedHashMap<>();

    /** 普通级别动态变量（变量名和值都经过SpEL运算后导入，使用时需加#前缀） */
    private Map<String, Object> val = new LinkedHashMap<>();

    /** Root级别字面量变量（直接导入，不进行SpEL运算） */
    private Map<String, Object> rootLit = new LinkedHashMap<>();

    /** 普通级别字面量变量（直接导入，不进行SpEL运算，使用时需加#前缀） */
    private Map<String, Object> varLit = new LinkedHashMap<>();

    /** 需要导入静态方法的类列表（类的public static方法会注册为SpEL函数） */
    private List<Class<?>> classes = new ArrayList<>();

    /** 需要导入的包路径列表（简化SpEL表达式中的类名引用） */
    private List<String> pack = new ArrayList<>();

    // ==================== Getter & Setter ====================

    /**
     * 获取Root级别动态变量配置
     * @return Root动态变量Map
     */
    public Map<String, Object> getRoot() {
        return root;
    }

    /**
     * 设置Root级别动态变量配置
     * @param root Root动态变量Map
     */
    public void setRoot(Map<String, Object> root) {
        this.root = root;
    }

    /**
     * 获取普通级别动态变量配置
     * @return 普通动态变量Map
     */
    public Map<String, Object> getVal() {
        return val;
    }

    /**
     * 设置普通级别动态变量配置
     * @param val 普通动态变量Map
     */
    public void setVal(Map<String, Object> val) {
        this.val = val;
    }

    /**
     * 获取Root级别字面量变量配置
     * @return Root字面量变量Map
     */
    public Map<String, Object> getRootLit() {
        return rootLit;
    }

    /**
     * 设置Root级别字面量变量配置
     * @param rootLit Root字面量变量Map
     */
    public void setRootLit(Map<String, Object> rootLit) {
        this.rootLit = rootLit;
    }

    /**
     * 获取普通级别字面量变量配置
     * @return 普通字面量变量Map
     */
    public Map<String, Object> getVarLit() {
        return varLit;
    }

    /**
     * 设置普通级别字面量变量配置
     * @param varLit 普通字面量变量Map
     */
    public void setVarLit(Map<String, Object> varLit) {
        this.varLit = varLit;
    }

    /**
     * 获取需要导入静态方法的类列表
     * @return 类列表
     */
    public List<Class<?>> getClasses() {
        return classes;
    }

    /**
     * 设置需要导入静态方法的类列表
     * @param classes 类列表
     */
    public void setClasses(List<Class<?>> classes) {
        this.classes = classes;
    }

    /**
     * 获取需要导入的包路径列表
     * @return 包路径列表
     */
    public List<String> getPack() {
        return pack;
    }

    /**
     * 设置需要导入的包路径列表
     * @param pack 包路径列表
     */
    public void setPack(List<String> pack) {
        this.pack = pack;
    }

    /**
     * 将所有配置导入到SpEL运行时上下文中
     * 导入顺序：静态方法 -> 字面量变量 -> 动态变量（SpEL求值后） -> 包路径
     *
     * @param context SpEL运行时上下文
     */
    public void importSpELRuntime(Context context) {
        SpELVariate contextVar = context.getContextVar();

        // 1. 导入类的静态方法作为函数
        for (Class<?> clazz : classes) {
            contextVar.addFunctions(ClassStaticElement.create(clazz).getAllStaticMethods());
            contextVar.addHook(clazz);
        }

        // 2. 导入字面量变量
        contextVar.addRootVariables(rootLit);
        contextVar.addVariables(varLit);

        // 3. 导入Root动态变量（SpEL求值后）
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            String key = context.parseExpression(entry.getKey(), String.class);
            Object value = context.getParsedValue(entry.getValue());
            contextVar.addRootVariable(key, value);
        }

        // 4. 导入普通动态变量（SpEL求值后）
        for (Map.Entry<String, Object> entry : val.entrySet()) {
            String key = context.parseExpression(entry.getKey(), String.class);
            Object value = context.getParsedValue(entry.getValue());
            contextVar.addVariable(key, value);
        }

        // 5. 导入包路径
        contextVar.addPackages(pack);
    }
}