package com.luckyframework.spel;

import com.luckyframework.common.ContainerUtils;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.support.StandardTypeLocator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 限制类型的定位器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/7/9 10:27
 */
public class RestrictedTypeLocator extends StandardTypeLocator {

    /**
     * 类型白名单
     */
    protected List<Class<?>> whiteList = new ArrayList<>();

    /**
     * 类型黑名单
     */
    protected List<Class<?>> blackList = new ArrayList<>();

    /**
     * 类型限制模型，默认使用黑白名单
     */
    protected Model model = Model.BLACK_WHITE_LIST;

    /**
     * 类型限制比较算法，默认通过{@link Class#equals(Object)}方法进行比较
     */
    protected Compare compare = Compare.EQUALS;

    /**
     * 设置类型限制模型
     *
     * @param model 类型限制模型
     */
    public void setModel(Model model) {
        this.model = model;
    }

    /**
     * 设置类型限制比较算法
     *
     * @param compare 类型限制比较算法
     */
    public void setCompare(Compare compare) {
        this.compare = compare;
    }

    /**
     * 设置类型白名单
     *
     * @param whiteList 类型白名单
     */
    public void setWhiteList(List<Class<?>> whiteList) {
        this.whiteList = whiteList;
    }

    /**
     * 设置类型黑名单
     *
     * @param blackList 类型黑名单
     */
    public void setBlackList(List<Class<?>> blackList) {
        this.blackList = blackList;
    }

    /**
     * 添加类型白名单
     *
     * @param whiteList 类型白名单
     */
    public void addWhiteList(Class<?>... whiteList) {
        this.whiteList.addAll(Arrays.asList(whiteList));
    }

    /**
     * 添加类型黑名单
     *
     * @param blackList 类型黑名单
     */
    public void addBlackList(Class<?>... blackList) {
        this.blackList.addAll(Arrays.asList(blackList));
    }

    /**
     * 配置合并，将传入的定位器的配置与当前配置进行合并
     *
     * @param restrictedTypeLocator 另外一个类型定位器
     */
    public void mergeConfig(RestrictedTypeLocator restrictedTypeLocator) {
        whiteList.addAll(restrictedTypeLocator.whiteList);
        blackList.addAll(restrictedTypeLocator.blackList);
        restrictedTypeLocator.getImportPrefixes().forEach(this::registerImport);
        model = restrictedTypeLocator.model;
        compare = restrictedTypeLocator.compare;
    }

    /**
     * 类型查找
     *
     * @param typeName the type to locate
     * @return 查找结果
     * @throws EvaluationException 可能出现的异常
     */
    @Override
    public Class<?> findType(String typeName) throws EvaluationException {
        Class<?> type = super.findType(typeName);
        checkType(type);
        return type;
    }

    /**
     * 根据类型校验模型对类型进行校验
     *
     * @param type 待校验的类型
     */
    private void checkType(Class<?> type) {
        if (Objects.requireNonNull(model) == Model.BLACK_WHITE_LIST) {
            checkBlackList(type);
            checkWhiteList(type);
        } else {
            throw new SecurityException("Access to types is forbidden");
        }
    }


    /**
     * 白名单校验
     *
     * @param type 待校验的类型
     */
    private void checkWhiteList(Class<?> type) {
        if (ContainerUtils.isEmptyCollection(whiteList)) {
            return;
        }
        for (Class<?> whiteClass : whiteList) {
            if (compare(whiteClass, type)) {
                return;
            }
        }
        throw new SecurityException("Access to type " + type.getName() + " is not allowed");
    }

    /**
     * 黑名单校验
     *
     * @param type 待校验的类型
     */
    private void checkBlackList(Class<?> type) {
        for (Class<?> blackClass : this.blackList) {
            if (compare(blackClass, type)) {
                throw new SecurityException("Access to type " + type.getName() + " is not allowed");
            }
        }
    }

    /**
     * 类型比较
     *
     * @param config 配置的类型
     * @param input  输入的额类型
     * @return 比较结果
     */
    private boolean compare(Class<?> config, Class<?> input) {
        return compare == Compare.EQUALS ? config.equals(input) : config.isAssignableFrom(input);
    }

    /**
     * 类型限制模型
     */
    public enum Model {

        /**
         * 黑白名单模型
         */
        BLACK_WHITE_LIST,

        /**
         * 全部禁用
         */
        ALL_PROHIBITED
    }

    /**
     * 类型限制比较算法
     */
    public enum Compare {
        /**
         * 通过{@link Class#equals(Object)}方法进行比较
         */
        EQUALS,

        /**
         * 通过{@link Class#isAssignableFrom(Class)}方法进行比较
         */
        EXTEND,
    }
}
