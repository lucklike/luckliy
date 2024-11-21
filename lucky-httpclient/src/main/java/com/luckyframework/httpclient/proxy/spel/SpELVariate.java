package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.CtrlMap;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
     * 抛异常的变量修改验证器
     */
    private static final CtrlMap.ModifiedVerifier<String> ERR_VERIFIER = k -> !InternalParamName.getAllInternalParamName().contains(k);

    /**
     * 忽略修改的变量修改验证器
     */
    private static final CtrlMap.ModifiedVerifier<String> IGNORE_VERIFIER = k -> !ProhibitCoverEnum.isMatch(k);

    /**
     * 普通变量（变量+函数）
     */
    private final Map<String, Object> var;

    /**
     * Root变量
     */
    private final Map<String, Object> root;

    /**
     * 要导入的包
     */
    private final List<String> packs;

    /**
     * SpEL变量构造器
     */
    public SpELVariate() {
        this.var = createCtrlMap();
        this.root = createCtrlMap();
        this.packs = new ArrayList<>();
    }

    //----------------------------------------------------------------------------
    //                             Getter
    //----------------------------------------------------------------------------

    public Map<String, Object> getVar() {
        return var;
    }

    public Map<String, Object> getRoot() {
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
        root.put(name, value);
    }

    /**
     * 添加一组Root变量
     *
     * @param rootVariables Root变量组
     */
    public void addRootVariables(Map<String, Object> rootVariables) {
        root.putAll(rootVariables);
    }

    /**
     * 移除某个Root变量
     *
     * @param name 变量名
     */
    public void removeRootVariable(String name) {
        root.remove(name);
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
        var.put(name, value);
    }

    /**
     * 添加一组普通变量
     *
     * @param rootVariables 普通变量组
     */
    public void addVariables(Map<String, Object> rootVariables) {
        var.putAll(rootVariables);
    }

    /**
     * 移除某个普通变量
     *
     * @param name 变量名
     */
    public void removeVariable(String name) {
        var.remove(name);
    }

    //----------------------------------------------------------------------------
    //                               Function
    //----------------------------------------------------------------------------


    /**
     * 添加一个函数
     *
     * @param name  函数名
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

    //----------------------------------------------------------------------------
    //                               Package
    //----------------------------------------------------------------------------

    /**
     * 添加一个包
     *
     * @param packageName  包名
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
     * @param clazz  包下的某个类的Class
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

    /**
     * 创建受控Map
     *
     * @return 受控Map
     */
    private CtrlMap<String, Object> createCtrlMap() {
        return new CtrlMap<>(new ConcurrentHashMap<>(64), ERR_VERIFIER, IGNORE_VERIFIER);
    }
}
