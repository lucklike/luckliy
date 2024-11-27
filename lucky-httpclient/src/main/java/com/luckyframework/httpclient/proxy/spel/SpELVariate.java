package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.exception.CtrlMapValueModifiedException;
import com.luckyframework.httpclient.proxy.context.Context;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * SpEL变量
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/21 21:23
 */
public class SpELVariate {

    /**
     * 普通变量（变量+函数）
     */
    private final VarCtrlMap var;

    /**
     * Root变量
     */
    private final RootVarCtrlMap root;

    /**
     * 要导入的包
     */
    private final List<String> packs;

    /**
     * 回调管理器
     */
    private final HookManager hookManager;

    /**
     * SpEL变量构造器
     */
    public SpELVariate() {
        this(null);
    }

    /**
     * SpEL变量构造器
     */
    public SpELVariate(Context context) {
        this.root = new RootVarCtrlMap(context);
        this.var = new VarCtrlMap(context);
        this.packs = new ArrayList<>();
        this. hookManager = new HookManager();
    }

    //----------------------------------------------------------------------------
    //                             Getter
    //----------------------------------------------------------------------------

    public VarCtrlMap getVar() {
        return var;
    }

    public RootVarCtrlMap getRoot() {
        return root;
    }

    public List<String> getPacks() {
        return packs;
    }

    //----------------------------------------------------------------------------
    //                              Root Variable
    //----------------------------------------------------------------------------

    /**
     * 添加一个Root变量
     *
     * @param name  变量名
     * @param value 变量值
     */
    public void addRootVariable(String name, Object value) {
        try {
            root.put(name, value);
        } catch (CtrlMapValueModifiedException e) {
            throw new AddSpELVariableException(e, "You are trying to define or modify a Lucky built-in Root SpEL variable: '{}', which is not allowed.", name);
        }
    }

    /**
     * 添加一组Root变量
     *
     * @param rootVariables Root变量组
     */
    public void addRootVariables(Map<String, Object> rootVariables) {
        rootVariables.forEach(this::addRootVariable);
    }

    /**
     * 移除某个Root变量
     *
     * @param name 变量名
     */
    public void removeRootVariable(String name) {
        root.remove(name);
    }

    /**
     * 是否存在该名称的Root变量
     *
     * @param name 带校验的变量名
     * @return 是否存在该名称的Root变量
     */
    public boolean hasRootVariable(String name) {
        return root.existenceOrNot(name);
    }

    //----------------------------------------------------------------------------
    //                               Variable
    //----------------------------------------------------------------------------

    /**
     * 添加一个普通变量
     *
     * @param name  变量名
     * @param value 变量值
     */
    public void addVariable(String name, Object value) {
        try {
            var.put(name, value);
        } catch (CtrlMapValueModifiedException e) {
            throw new AddSpELVariableException(e, "You are trying to define or modify a Lucky built-in SpEL variable or function: '{}', which is not allowed.", name);
        }
    }

    /**
     * 添加一组普通变量
     *
     * @param rootVariables 普通变量组
     */
    public void addVariables(Map<String, Object> rootVariables) {
        rootVariables.forEach(this::addVariable);
    }

    /**
     * 移除某个普通变量
     *
     * @param name 变量名
     */
    public void removeVariable(String name) {
        var.remove(name);
    }

    /**
     * 是否存在该名称的普通变量
     *
     * @param name 带校验的变量名
     * @return 是否存在该名称的普通变量
     */
    public boolean hasVariable(String name) {
        return var.existenceOrNot(name);
    }


    //----------------------------------------------------------------------------
    //                               Function
    //----------------------------------------------------------------------------


    /**
     * 添加一个函数
     *
     * @param name     函数名
     * @param function 函数
     */
    public void addFunction(String name, Method function) {
        addVariable(name, function);
    }

    /**
     * 添加一组函数
     *
     * @param functionMap 函数组
     */
    public void addFunctions(Map<String, Object> functionMap) {
        addVariables(functionMap);
    }

    /**
     * 移除某个函数
     *
     * @param name 函数名
     */
    public void removeFunction(String name) {
        removeVariable(name);
    }

    /**
     * 是否存在该名称的函数
     *
     * @param name 带校验的函数名
     * @return 是否存在该名称的函数
     */
    public boolean hasFunction(String name) {
        return hasVariable(name);
    }

    //----------------------------------------------------------------------------
    //                               Package
    //----------------------------------------------------------------------------

    /**
     * 添加一个包
     *
     * @param packageName 包名
     */
    public void addPackage(String packageName) {
        packageName = packageName.trim();
        if (!this.packs.contains(packageName)) {
            this.packs.add(packageName);
        }
    }

    /**
     * 添加一个包
     *
     * @param clazz 包下的某个类的Class
     */
    public void addPackage(Class<?> clazz) {
        addPackage(clazz.getPackage().getName());
    }

    /**
     * 添加一组包
     *
     * @param packageNames 包集合
     */
    public void addPackages(Collection<String> packageNames) {
        packageNames.forEach(this::addPackage);
    }

    /**
     * 添加一组包
     *
     * @param packageNames 包集合
     */
    public void addPackages(String[] packageNames) {
        Stream.of(packageNames).forEach(this::addPackage);
    }


    /**
     * 添加一组包
     *
     * @param classes 类集合
     */
    public void addPackagesByClasses(Collection<Class<?>> classes) {
        classes.forEach(this::addPackage);
    }

    /**
     * 添加一组包
     *
     * @param classes 类集合
     */
    public void addPackagesByClasses(Class<?>[] classes) {
        Stream.of(classes).forEach(this::addPackage);
    }

    /**
     * 移除某个包
     *
     * @param packageName 包名
     */
    public void removePackage(String packageName) {
        this.packs.remove(packageName);
    }

    /**
     * 移除某个包
     *
     * @param clazz 包下的某个类的Class
     */
    public void removePackage(Class<?> clazz) {
        removePackage(clazz.getPackage().getName());
    }

    //----------------------------------------------------------------------------
    //                               Hook
    //----------------------------------------------------------------------------

    public void addHook(Class<?> clazz) {
        this.hookManager.addHookGroup(clazz);
    }

    public void useHook(Lifecycle lifecycle, Context context) {
        this.hookManager.useHook(lifecycle, context);
    }
}
