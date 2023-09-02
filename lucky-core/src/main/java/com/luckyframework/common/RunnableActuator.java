package com.luckyframework.common;

import com.luckyframework.reflect.AnnotationUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 条件执行器，封装了很多判断条件，并且支持链式调用。
 * 可以用此类来消除if语句
 * 例如：
 * <pre>{@code
 * // 示例代码一：
 * if(StringUtils.hasLength(str) && "xyz".equals(str)){
 *      System.out.println(str");
 * }
 * // 等价写法 -->
 * delayCompute()
 * .isNotEmptyString(str)
 * .isEquals(str, "xyz")
 * .allTrueRun(System.out::println);
 *
 * //示例代码二：
 * if(x<0 || X>100){
 *     System.out.println("不是一个合法的成绩");
 * }
 * // 等价写法 -->
 * delayCompute()
 * .isTrue(x<0)
 * .isTrue(x>100)
 * .oneTrueRun(()-> System.out.println("不是一个合法的成绩"))
 *
 * // 示例代码三：
 * if(x<0){
 *     System.out.println("x为负数");
 * } else if (x>0){
 *     System.out.println("x为正数")
 * } else {
 *     System.out.println("x为0")
 * }
 * // 等价写法 -->
 * delayCompute()
 * .isTrue(x>0)
 * .lastTrueRun(() -> System.out.println("x为负数"))
 * .isTrue(x<0)
 * .lastTrueRun(() -> System.out.println("x为正数"))
 * .allFalseRun(() -> System.out.println("x为0"))
 * }</pre>
 *
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/1/4 18:21
 */
public class RunnableActuator {

    private final Map<String, Result> nameResultMap = new LinkedHashMap<>();
    private final Map<Integer, Result> indexResultMap = new LinkedHashMap<>();

    private final ExecutionStrategy strategy;

    private RunnableActuator(ExecutionStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * 如果满足条件则执行给定的逻辑，否则不执行
     *
     * @param b        是否满足条件
     * @param runnable 给定的逻辑
     */
    public static void trueIsRunning(boolean b, Runnable runnable) {
        if (b) {
            runnable.run();
        }
    }

    /**
     * 如果不满足条件则执行给定的逻辑，否则不执行
     *
     * @param b        是否满足条件
     * @param runnable 给定的逻辑
     */
    public static void falseIsRunning(boolean b, Runnable runnable) {
        if (!b) {
            runnable.run();
        }
    }

    /**
     * 如果条件不满足则抛出异常
     *
     * @param b     是否满足条件
     * @param exMag 异常信息
     */
    public static void falseThrow(boolean b, String exMag) {
        if (!b) {
            throw new IllegalArgumentException(exMag);
        }
    }

    /**
     * 如果条件满足则抛出异常
     *
     * @param b     是否满足条件
     * @param exMag 异常信息
     */
    public static void trueThrow(boolean b, String exMag) {
        if (b) {
            throw new IllegalArgumentException(exMag);
        }
    }

    /**
     * 如果主流程执行出现异常则执行异常流程
     *
     * @param mainRunnable           主流程
     * @param exceptionAfterRunnable 异常流程
     */
    public static void exAfterRunning(Runnable mainRunnable, Runnable exceptionAfterRunnable) {
        try {
            mainRunnable.run();
        } catch (Throwable e) {
            exceptionAfterRunnable.run();
        }
    }

    /**
     * 条件实时计算
     *
     * @return RunnableActuator对象
     */
    public static RunnableActuator realTimeCompute() {
        return runnableActuator(cer -> true);
    }

    /**
     * 条件延时计算
     *
     * @return RunnableActuator对象
     */
    public static RunnableActuator delayCompute() {
        return runnableActuator(cer -> false);
    }

    public static RunnableActuator runnableActuator(ExecutionStrategy strategy) {
        return new RunnableActuator(strategy);
    }

    private Result getResult(boolean result) {
        return result
                ? new Result(RunStatusEnum.FINISH_TRUE, () -> true)
                : new Result(RunStatusEnum.FINISH_FALSE, () -> false);
    }

    /**
     * 获取默认名称
     *
     * @return 默认名称
     */
    private String getDefaultName() {
        return "index[" + this.nameResultMap.size() + "]";
    }

    /**
     * 添加一个结果对象
     *
     * @param name   名称
     * @param result 结果枚举
     */
    private void addResult(String name, Result result) {
        if (nameResultMap.containsKey(name)) {
            throw new IllegalArgumentException("The conditional result whose name is '" + name + "' already exists and cannot be added repeatedly.");
        }
        this.nameResultMap.put(name, result);
        this.indexResultMap.put(nameResultMap.size() - 1, result);
    }

    /**
     * 添加一个结果对象，使用默认的名称[index[size()]]
     *
     * @param result 结果枚举
     */
    private void addResult(Result result) {
        addResult(getDefaultName(), result);
    }

    /**
     * 添加一个布尔结果
     *
     * @param name   名称
     * @param result 结果
     */
    private void addBoolean(String name, boolean result) {
        addResult(name, getResult(result));
    }

    /**
     * 添加一个结果，使用默认的名称[index[size()]]
     *
     * @param result 结果
     */
    private void addBoolean(boolean result) {
        addBoolean(getDefaultName(), result);
    }

    /**
     * 添加一个{@link Result TRUE}
     *
     * @param name 名称
     */
    private void addResultTrue(String name) {
        addBoolean(name, true);
    }

    /**
     * 添加一个{@link Result TRUE}，使用默认的名称[index[size()]]
     */
    private void addResultTrue() {
        addResultTrue(getDefaultName());
    }

    /**
     * 添加一个{@link Result FALSE}
     *
     * @param name 名称
     */
    private void addResultFalse(String name) {
        addBoolean(name, false);
    }

    /**
     * 添加一个{@link Result FALSE}，使用默认的名称[index[size()]]
     */
    private void addResultFalse() {
        addResultFalse(getDefaultName());
    }

    /**
     * 添加一个{@link Result NOT_RUNNING}
     *
     * @param name            名称
     * @param booleanSupplier boolean结果获取器
     */
    private void addResultNotRunning(String name, Supplier<Boolean> booleanSupplier) {
        addResult(name, new Result(RunStatusEnum.NOT_RUNNING, booleanSupplier));
    }

    /**
     * 添加一个{@link Result NOT_RUNNING}，使用默认的名称[index[size()]]
     *
     * @param booleanSupplier boolean结果获取器
     */
    private void addResultNotRunning(Supplier<Boolean> booleanSupplier) {
        addResultNotRunning(getDefaultName(), booleanSupplier);
    }

    /**
     * 是否执行条件方法
     *
     * @return 是否执行条件方法
     */
    private boolean isExeConditionFunction() {
        return strategy.isExecution(new CurrentEnvResult(this));
    }

    /**
     * 是否为真
     *
     * @param name                    名称
     * @param conditionResultSupplier 条件结果Supplier
     * @return 当前RunnableActuator
     */
    public RunnableActuator isTrue(String name, Supplier<Boolean> conditionResultSupplier) {
        if (isExeConditionFunction()) {
            addBoolean(name, conditionResultSupplier.get());
        } else {
            addResultNotRunning(name, conditionResultSupplier);
        }
        return this;
    }

    /**
     * 是否为真，使用默认的名称[index[size()]]
     *
     * @param conditionResultSupplier 条件结果Supplier
     * @return 当前RunnableActuator
     */
    public RunnableActuator isTrue(Supplier<Boolean> conditionResultSupplier) {
        return isTrue(getDefaultName(), conditionResultSupplier);
    }

    /**
     * 是否为真
     *
     * @param name            名称
     * @param conditionResult 条件结果
     * @return 当前RunnableActuator
     */
    public RunnableActuator isTrue(String name, boolean conditionResult) {
        return isTrue(name, () -> conditionResult);
    }

    /**
     * 是否为真，使用默认的名称[index[size()]]
     *
     * @param conditionResult 条件结果
     * @return 当前RunnableActuator
     */
    public RunnableActuator isTrue(boolean conditionResult) {
        return isTrue(getDefaultName(), conditionResult);
    }

    /**
     * 是否为假
     *
     * @param name                    名称
     * @param conditionResultSupplier 条件结果Supplier
     * @return 当前RunnableActuator
     */
    public RunnableActuator isFalse(String name, Supplier<Boolean> conditionResultSupplier) {
        if (isExeConditionFunction()) {
            addBoolean(name, !conditionResultSupplier.get());
        } else {
            addResultNotRunning(name, conditionResultSupplier);
        }
        return this;
    }

    /**
     * 是否为假，使用默认的名称[index[size()]]
     *
     * @param conditionResultSupplier 条件结果Supplier
     * @return 当前RunnableActuator
     */
    public RunnableActuator isFalse(Supplier<Boolean> conditionResultSupplier) {
        return isFalse(getDefaultName(), conditionResultSupplier);
    }

    /**
     * 是否为假
     *
     * @param name            名称
     * @param conditionResult 条件结果
     * @return 当前RunnableActuator
     */
    public RunnableActuator isFalse(String name, boolean conditionResult) {
        return isFalse(name, () -> conditionResult);
    }

    /**
     * 是否为假，使用默认的名称[index[size()]]
     *
     * @param conditionResult 条件结果
     * @return 当前RunnableActuator
     */
    public RunnableActuator isFalse(boolean conditionResult) {
        return isFalse(getDefaultName(), conditionResult);
    }

    /**
     * 是否执行出现异常
     *
     * @param name         名称
     * @param mainRunnable 主流程
     * @return 当前RunnableActuator
     */
    public RunnableActuator isException(String name, Runnable mainRunnable) {
        if (isExeConditionFunction()) {
            try {
                mainRunnable.run();
                addResultTrue(name);
            } catch (Throwable e) {
                addResultFalse(name);
            }
        } else {
            addResultNotRunning(name, () -> {
                try {
                    mainRunnable.run();
                    return true;
                } catch (Throwable e) {
                    return false;
                }
            });
        }
        return this;
    }

    /**
     * 是否执行出现异常，使用默认的名称[index[size()]]
     *
     * @param mainRunnable 主流程
     * @return 当前RunnableActuator
     */
    public RunnableActuator isException(Runnable mainRunnable) {
        return isException(getDefaultName(), mainRunnable);
    }

    /**
     * 是否执行没有出现异常
     *
     * @param name         名称
     * @param mainRunnable 主流程
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotException(String name, Runnable mainRunnable) {
        if (isExeConditionFunction()) {
            try {
                mainRunnable.run();
                addResultFalse(name);
            } catch (Throwable e) {
                addResultTrue(name);
            }
        } else {
            addResultNotRunning(name, () -> {
                try {
                    mainRunnable.run();
                    return false;
                } catch (Throwable e) {
                    return true;
                }
            });
        }
        return this;
    }

    /**
     * 是否执行没有出现异常，使用默认的名称[index[size()]]
     *
     * @param mainRunnable 主流程
     * @return 当前RunnableActuator当前
     */
    public RunnableActuator isNotException(Runnable mainRunnable) {
        return isNotException(getDefaultName(), mainRunnable);
    }

    /**
     * 是否为null
     *
     * @param name 名称
     * @param obj  待校验对象
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNull(String name, Object obj) {
        return isTrue(name, () -> Objects.isNull(obj));
    }

    /**
     * 是否为null，使用默认的名称[index[size()]]
     *
     * @param obj 待校验对象
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNull(Object obj) {
        return isNull(getDefaultName(), obj);
    }

    /**
     * 是否不为null
     *
     * @param name 名称
     * @param obj  待校验对象
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotNull(String name, Object obj) {
        return isFalse(name, () -> Objects.isNull(obj));
    }

    /**
     * 是否不为null，使用默认的名称[index[size()]]
     *
     * @param obj 待校验对象
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotNull(Object obj) {
        return isNotNull(getDefaultName(), obj);
    }

    /**
     * 判断两个对象是否相等
     *
     * @param name 名称
     * @param obj1 待判断的对象一
     * @param obj2 待判断的对象二
     * @param <T>  待判断的对象类型
     * @return 当前RunnableActuator
     */
    public <T> RunnableActuator isEquals(String name, T obj1, T obj2) {
        return isTrue(name, () -> {
            if (obj1 == null && obj2 == null) {
                return true;
            }
            if (obj1 == null || obj2 == null) {
                return false;
            }
            return obj1.equals(obj2);
        });
    }

    /**
     * 判断两个对象是否相等, 使用默认的名称[index[size()]]
     *
     * @param obj1 待判断的对象一
     * @param obj2 待判断的对象二
     * @param <T>  待判断的对象类型
     * @return 当前RunnableActuator
     */
    public <T> RunnableActuator isEquals(T obj1, T obj2) {
        return isEquals(getDefaultName(), obj1, obj2);
    }

    /**
     * 判断两个对象是否不相等
     *
     * @param name 名称
     * @param obj1 待判断的对象一
     * @param obj2 待判断的对象二
     * @param <T>  待判断的对象类型
     * @return 当前RunnableActuator
     */
    public <T> RunnableActuator isNotEquals(String name, T obj1, T obj2) {
        return isFalse(name, () -> {
            if (obj1 == null && obj2 == null) {
                return true;
            }
            if (obj1 == null || obj2 == null) {
                return false;
            }
            return obj1.equals(obj2);
        });
    }

    /**
     * 判断两个对象是否不相等, 使用默认的名称[index[size()]]
     *
     * @param obj1 待判断的对象一
     * @param obj2 待判断的对象二
     * @param <T>  待判断的对象类型
     * @return 当前RunnableActuator
     */
    public <T> RunnableActuator isNotEquals(T obj1, T obj2) {
        return isNotEquals(getDefaultName(), obj1, obj2);
    }

    /**
     * 是否是空字符串
     *
     * @param name 名称
     * @param str  待校验字符串
     * @return 当前RunnableActuator
     */
    public RunnableActuator isEmptyString(String name, String str) {
        return isFalse(name, () -> StringUtils.hasLength(str));
    }

    /**
     * 是否是空字符串，使用默认的名称[index[size()]]
     *
     * @param str 待校验字符串
     * @return 当前RunnableActuator
     */
    public RunnableActuator isEmptyString(String str) {
        return isEmptyString(getDefaultName(), str);
    }

    /**
     * 是否不是空字符串
     *
     * @param name 名称
     * @param str  待校验字符串
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotEmptyString(String name, String str) {
        return isTrue(name, () -> StringUtils.hasLength(str));
    }

    /**
     * 是否不是空字符串，使用默认的名称[index[size()]]
     *
     * @param str 待校验字符串
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotEmptyString(String str) {
        return isNotEmptyString(getDefaultName(), str);
    }

    /**
     * 是否是空数组
     *
     * @param name  名称
     * @param array 待校验数组
     * @return 当前RunnableActuator
     */
    public RunnableActuator isEmptyArray(String name, Object[] array) {
        return isTrue(name, () -> ContainerUtils.isEmptyArray(array));
    }

    /**
     * 是否是空数组，使用默认的名称[index[size()]]
     *
     * @param array 待校验数组
     * @return 当前RunnableActuator
     */
    public RunnableActuator isEmptyArray(Object[] array) {
        return isEmptyArray(getDefaultName(), array);
    }

    /**
     * 是否不是空数组
     *
     * @param name  名称
     * @param array 待校验数组
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotEmptyArray(String name, Object[] array) {
        return isFalse(name, () -> ContainerUtils.isEmptyArray(array));
    }

    /**
     * 是否不是空数组，使用默认的名称[index[size()]]
     *
     * @param array 待校验数组
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotEmptyArray(Object[] array) {
        return isNotEmptyArray(getDefaultName(), array);
    }

    /**
     * 是否是空集合
     *
     * @param name       名称
     * @param collection 待校验集合
     * @return 当前RunnableActuator
     */
    public RunnableActuator isEmptyCollection(String name, Collection<?> collection) {
        return isTrue(name, () -> ContainerUtils.isEmptyCollection(collection));
    }

    /**
     * 是否是空集合，使用默认的名称[index[size()]]
     *
     * @param collection 待校验集合
     * @return 当前RunnableActuator
     */
    public RunnableActuator isEmptyCollection(Collection<?> collection) {
        return isEmptyCollection(getDefaultName(), collection);
    }

    /**
     * 是否不是空集合
     *
     * @param name       名称
     * @param collection 待校验集合
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotEmptyCollection(String name, Collection<?> collection) {
        return isFalse(name, () -> ContainerUtils.isEmptyCollection(collection));
    }

    /**
     * 是否不是空集合，使用默认的名称[index[size()]]
     *
     * @param collection 待校验集合
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotEmptyCollection(Collection<?> collection) {
        return isNotEmptyCollection(getDefaultName(), collection);
    }

    /**
     * 是否是一个可迭代对象
     *
     * @param name   名称
     * @param object 待校验对象
     * @return 当前RunnableActuator
     */
    public RunnableActuator isIterable(String name, Object object) {
        return isTrue(name, () -> ContainerUtils.isIterable(object));
    }

    /**
     * 是否是一个可迭代对象，使用默认的名称[index[size()]]
     *
     * @param object 待校验对象
     * @return 当前RunnableActuator
     */
    public RunnableActuator isIterable(Object object) {
        return isIterable(getDefaultName(), object);
    }

    /**
     * 是否不是一个可迭代对象
     *
     * @param name   名称
     * @param object 待校验对象
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotIterable(String name, Object object) {
        return isFalse(name, () -> ContainerUtils.isIterable(object));
    }

    /**
     * 是否不是一个可迭代对象，使用默认的名称[index[size()]]
     *
     * @param object 待校验对象
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotIterable(Object object) {
        return isNotIterable(getDefaultName(), object);
    }

    /**
     * 是否是一个空Map
     *
     * @param name 名称
     * @param map  待校验Map
     * @return 当前RunnableActuator
     */
    public RunnableActuator isEmptyMap(String name, Map<?, ?> map) {
        return isTrue(name, () -> ContainerUtils.isEmptyMap(map));
    }

    /**
     * 是否是一个空Map，使用默认的名称[index[size()]]
     *
     * @param map 待校验Map
     * @return 当前RunnableActuator
     */
    public RunnableActuator isEmptyMap(Map<?, ?> map) {
        return isEmptyMap(getDefaultName(), map);
    }

    /**
     * 是否不是一个空Map
     *
     * @param name 名称
     * @param map  待校验Map
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotEmptyMap(String name, Map<?, ?> map) {
        return isFalse(name, () -> ContainerUtils.isEmptyMap(map));
    }

    /**
     * 是否不是一个空Map，使用默认的名称[index[size()]]
     *
     * @param map 待校验Map
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotEmptyMap(Map<?, ?> map) {
        return isNotEmptyMap(getDefaultName(), map);
    }

    /**
     * 判断一个元素是否在集合中
     *
     * @param name       名称
     * @param collection 集合
     * @param element    元素
     * @return 当前RunnableActuator
     */
    public RunnableActuator contains(String name, Collection<?> collection, Object element) {
        return isTrue(name, () -> !ContainerUtils.isEmptyCollection(collection) && collection.contains(element));
    }

    /**
     * 判断一个元素是否在集合中，使用默认的名称[index[size()]]
     *
     * @param collection 集合
     * @param element    元素
     * @return 当前RunnableActuator
     */
    public RunnableActuator contains(Collection<?> collection, Object element) {
        return contains(getDefaultName(), collection, element);
    }

    /**
     * 判断一个元素是否不在集合中
     *
     * @param name       名称
     * @param collection 集合
     * @param element    元素
     * @return 当前RunnableActuator
     */
    public RunnableActuator notContains(String name, Collection<?> collection, Object element) {
        return isFalse(name, () -> !ContainerUtils.isEmptyCollection(collection) && collection.contains(element));
    }

    /**
     * 判断一个元素是否不在集合中，使用默认的名称[index[size()]]
     *
     * @param collection 集合
     * @param element    元素
     * @return 当前RunnableActuator
     */
    public RunnableActuator notContains(Collection<?> collection, Object element) {
        return notContains(getDefaultName(), collection, element);
    }

    /**
     * 判断一个Key在Map中是否存在
     *
     * @param name 名称
     * @param map  Map
     * @param key  待校验的Key
     * @return 当前RunnableActuator
     */
    public RunnableActuator containsKey(String name, Map<?, ?> map, Object key) {
        return isTrue(name, () -> !ContainerUtils.isEmptyMap(map) && map.containsKey(key));
    }

    /**
     * 判断一个Key在Map中是否存在，使用默认的名称[index[size()]]
     *
     * @param map  Map
     * @param key  待校验的Key
     * @return 当前RunnableActuator
     */
    public RunnableActuator containsKey(Map<?, ?> map, Object key) {
        return containsKey(getDefaultName(), map, key);
    }

    /**
     * 判断一个Key在Map中是否不存在
     *
     * @param name 名称
     * @param map  Map
     * @param key  待校验的Key
     * @return 当前RunnableActuator
     */
    public RunnableActuator notContainsKey(String name, Map<?, ?> map, Object key) {
        return isFalse(name, () -> !ContainerUtils.isEmptyMap(map) && map.containsKey(key));
    }

    /**
     * 判断一个Key在Map中是否不存在，使用默认的名称[index[size()]]
     *
     * @param map  Map
     * @param key  待校验的Key
     * @return 当前RunnableActuator
     */
    public RunnableActuator notContainsKey(Map<?, ?> map, Object key) {
        return notContainsKey(getDefaultName(), map, key);
    }

    /**
     * 判断一个Value在Map中是否存在
     *
     * @param name 名称
     * @param map  Map
     * @param value  待校验的Value
     * @return 当前RunnableActuator
     */
    public RunnableActuator containsValue(String name, Map<?, ?> map, Object value) {
        return isTrue(name, () -> !ContainerUtils.isEmptyMap(map) && map.containsValue(value));
    }

    /**
     * 判断一个Value在Map中是否存在，使用默认的名称[index[size()]]
     *
     * @param map  Map
     * @param value  待校验的Value
     * @return 当前RunnableActuator
     */
    public RunnableActuator containsValue(Map<?, ?> map, Object value) {
        return containsValue(getDefaultName(), map, value);
    }

    /**
     * 判断一个Value在Map中是否不存在
     *
     * @param name 名称
     * @param map  Map
     * @param value  待校验的Value
     * @return 当前RunnableActuator
     */
    public RunnableActuator notContainsValue(String name, Map<?, ?> map, Object value) {
        return isFalse(name, () -> !ContainerUtils.isEmptyMap(map) && map.containsValue(value));
    }

    /**
     * 判断一个Value在Map中是否不存在，使用默认的名称[index[size()]]
     *
     * @param map  Map
     * @param value  待校验的Value
     * @return 当前RunnableActuator
     */
    public RunnableActuator notContainsValue(Map<?, ?> map, Object value) {
        return notContainsValue(getDefaultName(), map, value);
    }

    /**
     * 当object是一个Class时校验它是否为baseClass的一个子类
     * 当object不是一个Class时校验object是否为baseClass的子类对象
     *
     * @param name      名称
     * @param baseClass 基类Class
     * @param object    待检验的对象
     * @return 当前RunnableActuator
     */
    public RunnableActuator isAssignableFrom(String name, Class<?> baseClass, Object object) {
        if (object instanceof Class) {
            return isTrue(name, () -> baseClass.isAssignableFrom((Class<?>) object));
        }
        return isTrue(name, () -> object != null && baseClass.isAssignableFrom(object.getClass()));
    }

    /**
     * 使用默认的名称[index[size()]]
     * 当object是一个Class时校验它是否为baseClass的一个子类
     * 当object不是一个Class时校验object是否为baseClass的子类对象
     *
     * @param baseClass 基类Class
     * @param object    待检验的对象
     * @return 当前RunnableActuator
     */
    public RunnableActuator isAssignableFrom(Class<?> baseClass, Object object) {
        return isAssignableFrom(getDefaultName(), baseClass, object);
    }

    /**
     * 当object是一个Class时校验它是否不是baseClass的一个子类
     * 当object不是一个Class时校验object是否不是baseClass的子类对象
     *
     * @param name      名称
     * @param baseClass 基类Class
     * @param object    待检验的对象
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotAssignableFrom(String name, Class<?> baseClass, Object object) {
        if (object instanceof Class) {
            return isFalse(name, () -> baseClass.isAssignableFrom((Class<?>) object));
        }
        return isFalse(name, () -> object != null && baseClass.isAssignableFrom(object.getClass()));
    }

    /**
     * 使用默认的名称[index[size()]]
     * 当object是一个Class时校验它是否不是baseClass的一个子类
     * 当object不是一个Class时校验object是否不是baseClass的子类对象
     *
     * @param baseClass 基类Class
     * @param object    待检验的对象
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotAssignableFrom(Class<?> baseClass, Object object) {
        return isNotAssignableFrom(getDefaultName(), baseClass, object);
    }

    /**
     * 判断某个元素是否在给定的数组中
     *
     * @param name  名称
     * @param array 数组
     * @param entry 元素
     * @return 当前RunnableActuator
     */
    public RunnableActuator inArray(String name, Object[] array, Object entry) {
        return isTrue(name, () -> ContainerUtils.inArrays(array, entry));
    }

    /**
     * 判断某个元素是否在给定的数组中，使用默认的名称[index[size()]]
     *
     * @param array 数组
     * @param entry 元素
     * @return 当前RunnableActuator
     */
    public RunnableActuator inArray(Object[] array, Object entry) {
        return inArray(getDefaultName(), array, entry);
    }

    /**
     * 判断某个元素是否不在给定的数组中
     *
     * @param name  名称
     * @param array 数组
     * @param entry 元素
     * @return 当前RunnableActuator
     */
    public RunnableActuator notInArray(String name, Object[] array, Object entry) {
        return isFalse(name, () -> ContainerUtils.inArrays(array, entry));
    }

    /**
     * 判断某个元素是否在给定的数组中，使用默认的名称[index[size()]]
     *
     * @param array 数组
     * @param entry 元素
     * @return 当前RunnableActuator
     */
    public RunnableActuator notInArray(Object[] array, Object entry) {
        return notInArray(getDefaultName(), array, entry);
    }

    /**
     * 判断某个注解元素是否被给定的注解标注
     *
     * @param name           名称
     * @param typeMetadata   注解元素
     * @param annotationName 注解的全类名
     * @return 当前RunnableActuator
     */
    public RunnableActuator isAnnotated(String name, AnnotatedTypeMetadata typeMetadata, String annotationName) {
        return isTrue(name, () -> typeMetadata.isAnnotated(annotationName));
    }

    /**
     * 判断某个注解元素是否被给定的注解标注，使用默认的名称[index[size()]]
     *
     * @param typeMetadata   注解元素
     * @param annotationName 注解的全类名
     * @return 当前RunnableActuator
     */
    public RunnableActuator isAnnotated(AnnotatedTypeMetadata typeMetadata, String annotationName) {
        return isAnnotated(getDefaultName(), typeMetadata, annotationName);
    }

    /**
     * 判断某个注解元素是否没有被给定的注解标注
     *
     * @param name           名称
     * @param typeMetadata   注解元素
     * @param annotationName 注解的全类名
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotAnnotated(String name, AnnotatedTypeMetadata typeMetadata, String annotationName) {
        return isFalse(name, () -> typeMetadata.isAnnotated(annotationName));
    }

    /**
     * 判断某个注解元素是否没有被给定的注解标注，使用默认的名称[index[size()]]
     *
     * @param typeMetadata   注解元素
     * @param annotationName 注解的全类名
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotAnnotated(AnnotatedTypeMetadata typeMetadata, String annotationName) {
        return isNotAnnotated(getDefaultName(), typeMetadata, annotationName);
    }

    /**
     * 判断某个注解元素是否被给定的注解标注
     *
     * @param name             名称
     * @param annotatedElement 注解元素
     * @param annotationClass  注解Class
     * @return 当前RunnableActuator
     */
    public RunnableActuator isAnnotated(String name, AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass) {
        return isTrue(name, () -> AnnotationUtils.isAnnotated(annotatedElement, annotationClass));
    }

    /**
     * 判断某个注解元素是否被给定的注解标注，使用默认的名称[index[size()]]
     *
     * @param annotatedElement 注解元素
     * @param annotationClass  注解Class
     * @return 当前RunnableActuator
     */
    public RunnableActuator isAnnotated(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass) {
        return isAnnotated(getDefaultName(), annotatedElement, annotationClass);
    }

    /**
     * 判断某个注解元素是否没有给定的注解标注
     *
     * @param name             名称
     * @param annotatedElement 注解元素
     * @param annotationClass  注解Class
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotAnnotated(String name, AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass) {
        return isFalse(name, () -> AnnotationUtils.isAnnotated(annotatedElement, annotationClass));
    }

    /**
     * 判断某个注解元素是否没有给定的注解标注，使用默认的名称[index[size()]]
     *
     * @param annotatedElement 注解元素
     * @param annotationClass  注解Class
     * @return 当前RunnableActuator
     */
    public RunnableActuator isNotAnnotated(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass) {
        return isNotAnnotated(getDefaultName(), annotatedElement, annotationClass);
    }

    /**
     * 执行策略满足时，执行某一段特定的逻辑
     *
     * @param strategy 执行策略
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator run(ExecutionStrategy strategy, Runnable runnable) {
        if (strategy.isExecution(new CurrentEnvResult(this))) {
            runnable.run();
        }
        return this;
    }

    /**
     * 最后一个条件为true时，执行一段特定的逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator lastTrueRun(Runnable runnable) {
        return run(new LastIsTrueRunning(), runnable);
    }

    /**
     * 最后一个条件为false时，执行一段特定的逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator lastFalseRun(Runnable runnable) {
        return run(new LastIsFalseRunning(), runnable);
    }

    /**
     * 指定名称对应的条件全为true时，执行一段特定的逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator nameAllTrueRun(Runnable runnable, String... names) {
        return run(new SpecifiedNameAllTrueIsRunning(names), runnable);
    }

    /**
     * 指定名称对应的条件中只要有一个为true时，执行一段特定的逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator nameOneTrueRun(Runnable runnable, String... names) {
        return run(new SpecifiedNameOnlyOneTrueIsRunning(names), runnable);
    }

    /**
     * 指定名称对应的条件全为false时，执行一段特定的逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator nameAllFalseRun(Runnable runnable, String... names) {
        return run(new SpecifiedNameAllFalseIsRunning(names), runnable);
    }

    /**
     * 指定名称对应的条件中只要有一个为false时，执行一段特定的逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator nameOneFalseRun(Runnable runnable, String... names) {
        return run(new SpecifiedNameOnlyOneFalseIsRunning(names), runnable);
    }

    /**
     * 指定索引对应的条件全为true时，执行一段特定的逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator indexAllTrueRun(Runnable runnable, int... index) {
        return run(new SpecifiedIndexAllTrueIsRunning(index), runnable);
    }

    /**
     * 指定索引对应的条件中只要有一个为true时，执行一段特定的逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator indexOneTrueRun(Runnable runnable, int... index) {
        return run(new SpecifiedIndexOnlyOneTrueIsRunning(index), runnable);
    }

    /**
     * 指定索引对应的条件全为false时，执行一段特定的逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator indexAllFalseRun(Runnable runnable, int... index) {
        return run(new SpecifiedIndexAllFalseIsRunning(index), runnable);
    }

    /**
     * 指定索引对应的条件中只要有一个为false时，执行一段特定的逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator indexOneFalseRun(Runnable runnable, int... index) {
        return run(new SpecifiedIndexOnlyOneFalseIsRunning(index), runnable);
    }

    /**
     * 所有条件全部为true时，执行某一段逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator allTrueRun(Runnable runnable) {
        return run(new AllTrueIsRunning(), runnable);
    }

    /**
     * 所有条件中只要有一个为true时，执行某一段逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator oneTrueRun(Runnable runnable) {
        return run(new OnlyOneTrueIsRunning(), runnable);
    }

    /**
     * 所有条件全部为false时，执行某一段逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator allFalseRun(Runnable runnable) {
        return run(new AllFalseIsRunning(), runnable);
    }

    /**
     * 所有条件中只要有一个为false时，执行某一段逻辑
     *
     * @param runnable 执行逻辑
     * @return 当前RunnableActuator
     */
    public RunnableActuator oneFalseRun(Runnable runnable) {
        return run(new OnlyOneFalseIsRunning(), runnable);
    }

    /**
     * 运行状态枚举
     */
    public enum RunStatusEnum {
        /**
         * 完成 -- 结果为true
         */
        FINISH_TRUE,
        /**
         * 完成 -- 结果为false
         */
        FINISH_FALSE,
        /**
         * 未运行状态
         */
        NOT_RUNNING;
    }

    public static class Result {

        private final Supplier<Boolean> booleanSupplier;

        private RunStatusEnum status;

        private Boolean booleanResult;

        public Result(RunStatusEnum status, Supplier<Boolean> booleanSupplier) {
            this.status = status;
            this.booleanSupplier = booleanSupplier;
        }

        public RunStatusEnum getStatus() {
            return status;
        }

        public Supplier<Boolean> getBooleanSupplier() {
            return booleanSupplier;
        }

        public synchronized boolean getBoolean() {
            if (this.status == RunStatusEnum.NOT_RUNNING) {
                this.booleanResult = getBooleanSupplier().get();
                this.status = this.booleanResult ? RunStatusEnum.FINISH_TRUE : RunStatusEnum.FINISH_FALSE;
            }
            return this.booleanResult;
        }
    }

    public static class CurrentEnvResult {
        private final Map<String, Result> nameResultMap;
        private final Map<Integer, Result> indexResultMap;

        CurrentEnvResult(RunnableActuator actuator) {
            this.nameResultMap = Collections.unmodifiableMap(actuator.nameResultMap);
            this.indexResultMap = Collections.unmodifiableMap(actuator.indexResultMap);
        }

        public Result getResult(String name) {
            return this.nameResultMap.get(name);
        }

        public Result getResult(int index) {
            return this.indexResultMap.get(index);
        }

        public Result getLastResult() {
            return nameResultMap.isEmpty() ? null : getResult(this.nameResultMap.size() - 1);
        }

        public Iterator<Result> getResultIterator() {
            return this.nameResultMap.values().iterator();
        }

        public Iterable<Result> getResultIterable() {
            return new Iterable<Result>() {
                @Override
                @NonNull
                public Iterator<Result> iterator() {
                    return getResultIterator();
                }
            };
        }

        public Result getLastEffectiveResult() {
            int index = this.nameResultMap.size() - 1;
            for (int i = index; i >= 0; i--) {
                Result result = this.indexResultMap.get(i);
                if (result.getStatus() != RunStatusEnum.NOT_RUNNING) {
                    return result;
                }
            }
            return null;
        }
    }


}
