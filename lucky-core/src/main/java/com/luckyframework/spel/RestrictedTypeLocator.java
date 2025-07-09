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

    protected List<Class<?>> whiteList = new ArrayList<>();
    protected List<Class<?>> blackList = new ArrayList<>();

    protected Model model = Model.BLACK_WHITE_LIST;
    protected Compare compare = Compare.EQUALS;

    public void setWhiteList(List<Class<?>> whiteList) {
        this.whiteList = whiteList;
    }

    public void setBlackList(List<Class<?>> blackList) {
        this.blackList = blackList;
    }

    public void addWhiteList(Class<?>... whiteList) {
        this.whiteList.addAll(Arrays.asList(whiteList));
    }

    public void addBlackList(Class<?>... blackList) {
        this.blackList.addAll(Arrays.asList(blackList));
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public void setCompare(Compare compare) {
        this.compare = compare;
    }

    public void merge(RestrictedTypeLocator restrictedTypeLocator) {
        whiteList.addAll(restrictedTypeLocator.whiteList);
        blackList.addAll(restrictedTypeLocator.blackList);
        restrictedTypeLocator.getImportPrefixes().forEach(this::registerImport);
        model = restrictedTypeLocator.model;
        compare = restrictedTypeLocator.compare;
    }

    @Override
    public Class<?> findType(String typeName) throws EvaluationException {
        Class<?> type = super.findType(typeName);
        checkType(type);
        return type;
    }

    private void checkType(Class<?> type) {
        if (Objects.requireNonNull(model) == Model.BLACK_WHITE_LIST) {
            checkBlackList(type);
            checkWhiteList(type);
        } else {
            throw new SecurityException("Access to types is forbidden");
        }
    }


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

    private void checkBlackList(Class<?> type) {
        for (Class<?> blackClass : this.blackList) {
            if (compare(blackClass, type)) {
                throw new SecurityException("Access to type " + type.getName() + " is not allowed");
            }
        }
    }

    private boolean compare(Class<?> config, Class<?> input) {
        return compare == Compare.EQUALS ? config.equals(input) : config.isAssignableFrom(input);
    }

    public enum Model {
        BLACK_WHITE_LIST,
        ALL_PROHIBITED
    }

    public enum Compare {
        EQUALS,
        EXTEND,
    }
}
