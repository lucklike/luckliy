package com.luckyframework.diff;

import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 差异描述对象
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/5/20 00:25
 */
public class Difference<T> {

    private Difference() {
    }

    /**
     * 原先没有新出现的 -> 需要新增的
     */
    private final List<T> insertList = new ArrayList<>(8);

    /**
     * 原先有新的没有了 -> 需要删除的
     */
    private final List<T> deleteList = new ArrayList<>(8);

    /**
     * 原来有现在也有，但是存在差异 -> 需要更新的
     */
    private final List<DiffInfo<T>> updateDiffInfoList = new ArrayList<>(8);


    /**
     * 判断两个对象是否存在差异
     *
     * @param obj1         对象一
     * @param obj2         对象二
     * @param filterFields 差异对比中需要过滤的属性
     * @param <T>          需要进行差异比较的类型
     * @return 存在差异(true)/不存在差异(false)
     */
    public static <T> boolean isExistDiff(T obj1, T obj2, String... filterFields) {
        if (obj1 == obj2) {
            return false;
        }
        if (obj1 == null || obj2 == null) {
            return true;
        }
        if (obj1.equals(obj2)) {
            return false;
        }

        Class<?> objClass = obj1.getClass();
        if (ClassUtils.isSimpleBaseType(objClass)) {
            return String.valueOf(obj1).equals(String.valueOf(obj2));
        }
        Set<String> filterFieldSet = new HashSet<>(Arrays.asList(filterFields));
        for (Field field : ClassUtils.getAllFields(objClass)) {
            String fieldName = field.getName();
            if (filterFieldSet.contains(fieldName)) {
                continue;
            }
            if (isExistDiff(FieldUtils.getValue(obj1, field), FieldUtils.getValue(obj2, field))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对两个实体集合进行差异分析并返回一个差异对象实例
     *
     * @param oldCollection   旧的实体集合
     * @param newCollection   新的实体集合
     * @param idGenerator     实体ID生成器
     * @param fieldFieldNames 差异对比中需要过滤的属性
     * @param <T>             需要进行差异比较的类型
     * @return 差异对象实例
     */
    public static <T> Difference<T> diffAnalysis(Collection<T> oldCollection, Collection<T> newCollection, Function<T, String> idGenerator, String... fieldFieldNames) {

        Map<String, T> oldMap = objectListGroup(oldCollection, idGenerator);
        Map<String, T> newMap = objectListGroup(newCollection, idGenerator);

        Difference<T> diff = new Difference<>();
        for (Map.Entry<String, T> oldEntry : oldMap.entrySet()) {
            String oldKey = oldEntry.getKey();
            T oldValue = oldEntry.getValue();
            // 新集合中有，老集合中也有的，这部分的需要进行差异分析
            if (newMap.containsKey(oldKey)) {
                T newValue = newMap.get(oldKey);
                if (isExistDiff(oldValue, newValue, fieldFieldNames)) {
                    diff.addUpdate(oldValue, newValue);
                }
            }
            // 老集合中有，新集合中没有 -> 这部分是需要delete的
            else {
                diff.addDelete(oldValue);
            }
        }

        for (Map.Entry<String, T> newEntry : newMap.entrySet()) {
            // 新集合中有，老集合中没有 -> 这部分是需要insert的
            if (!oldMap.containsKey(newEntry.getKey())) {
                diff.addInsert(newEntry.getValue());
            }
        }

        return diff;
    }

    /**
     * 对两个实体数组进行差异分析并返回一个差异对象实例
     *
     * @param oldArray        旧的实体数组
     * @param newArray        新的实体数组
     * @param idGenerator     实体ID生成器
     * @param fieldFieldNames 差异对比中需要过滤的属性
     * @param <T>             需要进行差异比较的类型
     * @return 差异对象实例
     */
    public static <T> Difference<T> diffAnalysis(T[] oldArray, T[] newArray, Function<T, String> idGenerator, String... fieldFieldNames) {
        return diffAnalysis(Arrays.asList(oldArray), Arrays.asList(newArray), idGenerator, fieldFieldNames);
    }

    /**
     * 将一个实体集合转化为【ID-实体】类型的Map
     *
     * @param collection  实体集合
     * @param idGenerator 实体ID生成器
     * @param <T>         实体类型
     * @return 【ID-实体】类型的Map
     */
    private static <T> Map<String, T> objectListGroup(Collection<T> collection, Function<T, String> idGenerator) {
        Map<String, T> idMap = new LinkedHashMap<>(collection.size());
        for (T obj : collection) {
            idMap.put(idGenerator.apply(obj), obj);
        }
        return idMap;
    }

    /**
     * 添加一个需要【新增】的实体
     *
     * @param item 实体对象
     */
    private void addInsert(T item) {
        this.insertList.add(item);
    }

    /**
     * 添加一个需要【删除】的实体
     *
     * @param item 实体对象
     */
    private void addDelete(T item) {
        this.deleteList.add(item);
    }

    /**
     * 添加一个需要【更新】的实体
     *
     * @param oldItem 旧实体对象
     * @param newItem 新实体对象
     */
    private void addUpdate(T oldItem, T newItem) {
        this.updateDiffInfoList.add(new DiffInfo<>(oldItem, newItem));
    }

    /**
     * 获取所有需要【新增】的实体集合
     *
     * @return 所有需要【新增】的实体集合
     */
    public List<T> getInsertList() {
        return Collections.unmodifiableList(this.insertList);
    }

    /**
     * 获取所有需要【删除】的实体集合
     *
     * @return 所有需要【删除】的实体集合
     */
    public List<T> getDeleteList() {
        return Collections.unmodifiableList(this.deleteList);
    }

    /**
     * 获取所有需要【更新】的新旧实体信息集合
     *
     * @return 所有需要【更新】的新旧实体信息集合
     */
    public List<DiffInfo<T>> getUpdateInfoList() {
        return Collections.unmodifiableList(this.updateDiffInfoList);
    }

    /**
     * 获取所有需要【更新】的实体集合
     *
     * @return 所有需要【更新】的实体集合
     */
    public List<T> getNewUpdateEntityList() {
        return getUpdateInfoList().stream().map(DiffInfo::getNewEntity).collect(Collectors.toList());
    }

    /**
     * 是否有需要【新增】的
     *
     * @return 有(true)/无(false)
     */
    public boolean hasInsert() {
        return !this.insertList.isEmpty();
    }

    /**
     * 是否有需要【删除】的
     *
     * @return 有(true)/无(false)
     */
    public boolean hasDelete() {
        return !this.deleteList.isEmpty();
    }

    /**
     * 是否有需要【更新】的
     *
     * @return 有(true)/无(false)
     */
    public boolean hasUpdate() {
        return !this.updateDiffInfoList.isEmpty();
    }

    /**
     * 对那些需要【新增】的实体进行处理
     *
     * @param diffHandler 差异处理器
     */
    public void insertHandler(DiffHandler<T> diffHandler) {
        if (hasInsert()) {
            diffHandler.diffHandle(this.insertList);
        }
    }

    /**
     * 对那些需要【删除】的实体进行处理
     *
     * @param diffHandler 差异处理器
     */
    public void deleteHandler(DiffHandler<T> diffHandler) {
        if (hasDelete()) {
            diffHandler.diffHandle(this.deleteList);
        }
    }

    /**
     * 对那些需要【更新】的新实体进行处理
     *
     * @param diffHandler 差异处理器
     */
    public void updateHandler(DiffHandler<T> diffHandler) {
        if (hasUpdate()) {
            diffHandler.diffHandle(getNewUpdateEntityList());
        }
    }

    /**
     * 【差异】处理方法
     *
     * @param updateHandler 差异详情处理器
     */
    public void updateHandler(DiffInfoHandler<T> updateHandler) {
        if (hasUpdate()) {
            updateHandler.diffInfoHandle(this.updateDiffInfoList);
        }
    }

    public static class DiffInfo<T> {
        private final T oldEntity;
        private final T newEntity;


        public DiffInfo(T oldEntity, T newEntity) {
            this.oldEntity = oldEntity;
            this.newEntity = newEntity;
        }

        public T getOldEntity() {
            return oldEntity;
        }

        public T getNewEntity() {
            return newEntity;
        }


        public void initDiffDesc() {

        }
    }

}
