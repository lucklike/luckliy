package com.luckyframework.bean.factory;

import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

/**
 * 用来描述依赖
 * @author fk7075
 * @version 1.0.0
 * @date 2021/3/13 上午10:06
 */
public class BeanReference {

    /** 依赖ID*/
    private String beanName;
    /** 注入方式*/
    private Autowire autowire;
    /** 依赖类型*/
    private ResolvableType type;
    /** 是否是懒加载*/
    private boolean isLazy = false;
    /** 是否为必须*/
    private boolean required = true;

    /**
     * 私有构造器
     */
    private BeanReference(){}

    /**
     * 构建一个基于类型的Bean引用
     * @param type 引用的类型
     * @param required 是否为必须
     * @return bean引用
     */
    public static BeanReference builderType(@NonNull ResolvableType type, boolean required){
        BeanReference br = new BeanReference();
        br.setAutowire(Autowire.BY_TYPE);
        br.setType(type);
        br.setRequired(required);
        return br;
    }

    /**
     * 构建一个基于类型的Bean引用
     * @param type 引用的类型
     * @param required 是否为必须
     * @return bean引用
     */
    public static BeanReference builderType(@NonNull Class<?> type, boolean required){
        return builderType(ResolvableType.forRawClass(type),required);
    }


    /**
     * 构建一个基于类型且必须注入的Bean引用
     * @param type 引用的类型
     * @return bean引用
     */
    public static BeanReference builderType(@NonNull Class<?> type){
        return builderType(ResolvableType.forRawClass(type));
    }

    /**
     * 构建一个基于类型且必须注入的Bean引用
     * @param type 引用的类型
     * @return bean引用
     */
    public static BeanReference builderType(@NonNull ResolvableType type){
        return builderType(type,true);
    }

    /**
     * 构建一个自动选择名称优先的Bean引用
     * @param beanName bean的名称
     * @param type     引用的类型
     * @param required 是否为必须
     * @return bean引用
     */
    public static BeanReference builderAutoNameFirst(@NonNull String beanName,@NonNull ResolvableType type,boolean required){
        BeanReference br = new BeanReference();
        br.setAutowire(Autowire.AUTO_NAME_FIRST);
        br.setBeanName(beanName);
        br.setType(type);
        br.setRequired(required);
        return br;
    }

    /**
     * 构建一个自动选择类型优先的Bean引用
     * @param beanName bean的名称
     * @param type     引用的类型
     * @param required 是否为必须
     * @return bean引用
     */
    public static BeanReference builderAutoTypeFirst(@NonNull String beanName,@NonNull ResolvableType type,boolean required){
        BeanReference br = new BeanReference();
        br.setAutowire(Autowire.AUTO_TYPE_FIRST);
        br.setBeanName(beanName);
        br.setType(type);
        br.setRequired(required);
        return br;
    }

    /**
     * 构建一个基于名称的Bean引用
     * @param beanName bean的名称
     * @param required 是否为必须
     * @return bean引用
     */
    public static BeanReference builderName(@NonNull String beanName, boolean required){
        BeanReference br = new BeanReference();
        br.setAutowire(Autowire.BY_NAME);
        br.setBeanName(beanName);
        br.setRequired(required);
        return br;
    }

    /**
     * 构建一个基于名称且必须注入的Bean引用
     * @param beanName bean的名称
     * @return bean引用
     */
    public static BeanReference builderName(@NonNull String beanName){
        return builderName(beanName,true);
    }

    /**
     * 构建一个基于@Value注解的环境值引用
     * @param valueName 环境变量名${name}
     * @param valueType 转化类型
     * @return 指定类型的环境变量
     */
    public static BeanReference builderValue(@NonNull String valueName, @NonNull ResolvableType valueType){
        BeanReference br = new BeanReference();
        br.setAutowire(Autowire.BY_VALUE);
        br.setRequired(false);
        br.setBeanName(valueName);
        br.setType(valueType);
        return br;
    }

    /**
     * 构建一个基于@Value注解的环境值引用
     * @param valueName 环境变量名${name}
     * @param valueType 转化类型
     * @return 指定类型的环境变量
     */
    public static BeanReference builderValue(@NonNull String valueName, @NonNull Class<?>valueType){
        BeanReference br = new BeanReference();
        br.setAutowire(Autowire.BY_VALUE);
        br.setRequired(false);
        br.setBeanName(valueName);
        br.setType(ResolvableType.forRawClass(valueType));
        return br;
    }

    /**
     * Bean引用的名称
     * @return beanName
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * 设置Bean引用的名称
     * @param beanName Bean引用的名称
     */
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /**
     * 设置引用类型
     * @param autowire 引用类型
     */
    void setAutowire(Autowire autowire) {
        this.autowire = autowire;
    }

    /**
     * Bean引用的类型
     * @return beanType
     */
    public ResolvableType getType() {
        return type;
    }

    /**
     * 设置Bean引用的类型
     * @param type Bean引用的类型
     */
    public void setType(ResolvableType type) {
        this.type = type;
    }

    /**
     * 是否为必须值
     * 1.Y -> 当在IOC容器中没有该引用对应的Bean时会抛出异常
     * 2.N -> 当在IOC容器中没有该引用对应的Bean时注入null
     * @return true/false
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * 设置是否为必须值
     * @param required 是否为必须值
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * 该引用是类型引用吗？
     * @return true/false
     */
    public boolean isByType(){
        return this.autowire == Autowire.BY_TYPE;
    }

    /**
     * 该引用是环境变量引用吗？
     * @return true/false
     */
    public boolean isByValue(){
        return this.autowire == Autowire.BY_VALUE;
    }

    /**
     * 该引用是名称引用吗？
     * @return true/false
     */
    public boolean isByName(){
        return this.autowire == Autowire.BY_NAME;
    }

    /**
     * 该引用是自动引用单名称优先？
     * @return true/false
     */
    public boolean isAutoNameFirst(){
        return this.autowire == Autowire.AUTO_NAME_FIRST;
    }

    /**
     * 该引用是自动引用类型称优先？
     * @return true/false
     */
    public boolean isAutoTypeFirst(){
        return this.autowire == Autowire.AUTO_TYPE_FIRST;
    }

    /** 是否懒加载*/
    public boolean isLazy() {
        return isLazy;
    }

    /** 设置懒加载*/
    public void setLazy(boolean lazy) {
        isLazy = lazy;
    }
}
