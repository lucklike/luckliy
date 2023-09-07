package com.luckyframework.common;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.MapEntryMixTogetherException;
import com.luckyframework.exception.MapUtilsOPException;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.util.Assert;

import java.util.*;

/**
 * MAP工具类
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/6/11 下午11:15
 */
@SuppressWarnings("all")
public abstract class MapUtils {

    private final static String ARRAY_IDENTIFICATION = "^[\\S\\s]+?\\[[0-9]\\d*\\]$";
    private final static String ARRAY_INDEX = "\\[[0-9]\\d*\\]";
    private final static String SEPARATOR = ".";

    /**
     * 得到key链最末端的key对指定的对象
     *
     * @param source 源Map
     * @param keys   key链
     * @return
     */
    public static Object get(Map<String, Object> source, String keys) {
        return toGet(source, keys, keys);
    }

    /**
     * 移除key链最末端的key指定的对象
     *
     * @param source 源Map
     * @param keys   key链
     * @return
     */
    public static Object remove(Map<String, Object> source, String keys) {
        Assert.notNull(source, "source map is null");
        Assert.notNull(keys, "key is null");
        if (!containsKey(source, keys)) {
            return null;
        }
        if (isArrayKey(keys)) {
            int x = keys.lastIndexOf("[");
            int index = Integer.parseInt(keys.substring(x + 1, keys.length() - 1));
            String key = keys.substring(0, x);
            Object tempObject = get(source, key);
            if (tempObject instanceof List) {
                return ((List) tempObject).remove(index);
            }
            if (tempObject.getClass().isArray()) {
                Object result = ((Object[]) tempObject)[index];
                for (int i = index; i < ((Object[]) tempObject).length - 1; i++) {
                    ((Object[]) tempObject)[i] = ((Object[]) tempObject)[i + 1];
                }
                return result;
            }
            Object result = null;
            Iterator iterator = ((Set) tempObject).iterator();
            int i = 0;
            while (iterator.hasNext()) {
                result = iterator.next();
                if (i == index) {
                    iterator.remove();
                    break;
                }
                i++;
            }
            return result;

        } else {
            int index = keys.lastIndexOf(SEPARATOR);
            if (index != -1) {
                String key = keys.substring(0, index);
                String removeKey = keys.substring(index + 1);
                Map<String, Object> tempMap = (Map) get(source, key);
                return tempMap.remove(removeKey);
            } else {
                return source.remove(keys);
            }

        }
    }

    /**
     * 判断key链是否指向源Map中的一个具体的对象
     *
     * @param source 源Map
     * @param keys   key链
     * @return
     */
    public static boolean containsKey(Map<String, Object> source, String keys) {
        Assert.notNull(source, "source map is null");
        Assert.notNull(keys, "key is null");
        try {
            return get(source, keys) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static void put(Map<String, Object> source, String keys, Object value) {
        Assert.notNull(source, "source map is null");
        Assert.notNull(keys, "key is null");
        //keys在原Map中已经存在
        if (containsKey(source, keys)) {
            if (value instanceof Map) {
                weakFusionMap((Map) get(source, keys), (Map) value);
            } else {
                remove(source, keys);
                put(source, keys, value);
            }

        } else {
            if (isArrayKey(keys)) {
                int i = keys.lastIndexOf("[");
                int tempListIndex = Integer.parseInt(keys.substring(i + 1, keys.length() - 1));
                String previousKey = keys.substring(0, i);
                if (containsKey(source, previousKey)) {
                    Object obj = get(source, previousKey);
                    if (obj instanceof List) {
                        if (tempListIndex < 0) {
                            throw new MapUtilsOPException("原Map中与'" + previousKey + "'对应的集合元素的长度为 " + ((List) obj).size() + " ,而提供的索引为[" + tempListIndex + "],这将导致索引越界异常！");
                        }
                        listPaddin((List) obj, tempListIndex, value);
                    } else if (obj.getClass().isArray()) {
                        if (tempListIndex < 0) {
                            throw new MapUtilsOPException("原Map中与'" + previousKey + "'对应的数组元素的长度为 " + ((Object[]) obj).length + " ,而提供的索引为[" + tempListIndex + "],这将导致索引越界异常！");
                        }
                        Object[] newArray = arrayPaddin((Object[]) obj, tempListIndex, value);
                        remove(source, previousKey);
                        put(source, previousKey, newArray);
                    } else {
                        throw new MapUtilsOPException("原Map中与'" + previousKey + "'对应元素的类型错误[" + obj.getClass() + "]，无法添加元素！");
                    }
                } else {

                    if (tempListIndex == 0) {
                        List<Object> tempList = new ArrayList<>();
                        tempList.add(value);
                        put(source, previousKey, tempList);
                    } else {
                        List<Object> tempList = new ArrayList<>(tempListIndex + 1);
                        listPaddin(tempList, tempListIndex, value);
                        put(source, previousKey, tempList);
                    }
                }
            } else {
                if (!keys.contains(SEPARATOR)) {
                    source.put(keys, value);
                } else {
                    Map<String, Object> tempMap = new HashMap<>();
                    int i = keys.lastIndexOf(SEPARATOR);
                    String previousKey = keys.substring(0, i);
                    String tempMapKey = keys.substring(i + 1);
                    tempMap.put(tempMapKey, value);
                    put(source, previousKey, tempMap);
                }
            }
        }
    }

    /**
     * 像key链最末端的那个key指定的Map中put一个键值对,如果key链中的某一环
     * 不存在或者对应的对象不是Map将会报错
     *
     * @param source 源Map
     * @param keys   key链
     * @param key    key值
     * @param value  值
     * @return
     */
    public static Object put(Map<String, Object> source, String keys, String key, Object value) {
        if (containsKey(source, keys)) {
            Object tempObj = get(source, keys);
            if (tempObj instanceof Map) {
                return ((Map) tempObj).put(key, value);
            } else {
                throw new MapUtilsOPException("原Map的keys[" + keys + "]所指的对象[" + tempObj.getClass().getName() + "]不是Map类型，无法执行put操作");
            }
        } else {
            throw new MapUtilsOPException("原中不存在keys[" + keys + "],无法执行put操作");
        }
    }

    private static Object toGet(Map<String, Object> source, String key, String completeKey) {
        Assert.notNull(source, "source map is null");
        Assert.notNull(key, "key is null");
        if (!key.contains(SEPARATOR)) {
            return getEntry(source, key);
        }
        int index = key.indexOf(SEPARATOR);
        String tempKey = key.substring(0, index);
        String leftKey = key.substring(index + 1);
        Object tempObject = getEntry(source, tempKey);
        if (tempObject instanceof Map) {
            return toGet((Map<String, Object>) tempObject, leftKey, completeKey);
        }
        String type = tempObject == null ? "null" : tempObject.getClass().getName();
        throw new MapUtilsOPException("在解析'" + completeKey + "'时出错，其中'" + completeKey.substring(0, completeKey.length() - leftKey.length()) +
                "'部分对应值的类型为'" + type + "'，无法继续解析！");
    }

    /**
     * 融合两个Map<br/>
     * 1.inputMap中存在souecMap没有的key时，这些key以及对应的value会被直接put到sourceMap中<br/>
     * 2.当inputMap和sourceMap存在相同的key时<br/>
     * 2.1 key对应的value为简单java类型时(基本类型以及其包装类型)，inputMap的value直接put到sourceMap中<br/>
     * 2.2 除去2.1的情况，两个value的类型不兼容时会抛出异常<br/>
     * 2.3 当value类型为Collection时，input集合会覆盖source集合<br/>
     * 2.4 当value的类型为Map时，会重复上面的流程<br/>
     *
     * @param source 原Map
     * @param input  输入的Map
     */
    public static <K, V> void weakFusionMap(Map<K, V> source, Map<K, V> input) {
        if (input == null || input.isEmpty()) {
            return;
        }
        for (Map.Entry<K, V> entry : input.entrySet()) {
            K inputEntryKey = entry.getKey();
            V inputEntryValue = entry.getValue();
            //当前key在source中不存在，直接put
            if (!source.containsKey(inputEntryKey)) {
                source.put(inputEntryKey, inputEntryValue);
            } else {
                V sourceEntryValue = source.get(inputEntryKey);
                //inputEntry == null ,不做任何操作
                if (inputEntryValue == null) {
                    continue;
                }
                //sourceEntry == null ,直接用inputEntryValue替换掉
                if (sourceEntryValue == null) {
                    source.put(inputEntryKey, inputEntryValue);
                    continue;
                }

                Class<?> sourceEntryClass = sourceEntryValue.getClass();
                Class<?> inputEntryClass = inputEntryValue.getClass();

                // 两个都是简单基本类型，则使用inputEntrytValue替代sourceEntryValue
                if (ClassUtils.isSimpleBaseType(inputEntryClass) && ClassUtils.isSimpleBaseType(sourceEntryClass)) {
                    source.put(inputEntryKey, inputEntryValue);
                    continue;
                }

                //类型不兼容
                if (!sourceEntryClass.isAssignableFrom(inputEntryClass) && !inputEntryClass.isAssignableFrom(sourceEntryClass)) {
                    throw new MapEntryMixTogetherException("Map元素融合异常，'" + inputEntryKey + "' => sourceEntry:[" + sourceEntryClass.getName() + "] , inputEntry:[" + inputEntryClass.getName() + "]");
                }

                //两个都是Map
                if ((sourceEntryValue instanceof Map) && (inputEntryValue instanceof Map)) {
                    weakFusionMap((Map) sourceEntryValue, (Map) inputEntryValue);
                }
                //两个都是集合，则用将input集合替代source集合
                else if ((sourceEntryValue instanceof Collection) && (inputEntryValue instanceof Collection)) {
                    source.put(inputEntryKey, (V) inputEntryValue);
                } else {
                    source.put(inputEntryKey, inputEntryValue);
                }
            }
        }
    }

    /**
     * 使用下标表达式获取Map中数组或集合中的元素
     * eg: {@code list[0],array[2]}
     *
     * @param source
     * @param key
     * @return
     */
    private static Object getEntry(Map<String, Object> source, String key) {
        if (isArrayKey(key)) {
            TempPair<String, Integer[]> pair = splitArrayKey(key);
            String realKey = pair.getOne();
            Object quasiCollectiveObject = source.get(realKey);
            if (quasiCollectiveObject == null) {
                throw new MapUtilsOPException("获取Map元素失败! '" + key + "'中'" + realKey + "'对应的元素为null");
            }
            if (quasiCollectiveObject instanceof List) {
                return getListEntry((List) quasiCollectiveObject, key);
            }
            if (quasiCollectiveObject instanceof Set) {
                return getListEntry(new ArrayList<>((Set) quasiCollectiveObject), key);
            }
            if (quasiCollectiveObject.getClass().isArray()) {
                return getListEntry(Arrays.asList((Object[]) quasiCollectiveObject), key);
            }
            throw new MapUtilsOPException("获取Map元素失败，元素类型错误! '" + key + "'中'" + realKey + "'对应的元素「" + quasiCollectiveObject + "」(" + quasiCollectiveObject.getClass().getName() + ")既不是数组也不是集合");
        }
        return source.get(key);
    }

    /**
     * 获取List元素
     * list[2][3]
     *
     * @param list     List集合
     * @param arrayKey 数组key
     * @return
     */
    private static Object getListEntry(List<Object> list, String arrayKey) {
        TempPair<String, Integer[]> pair = splitArrayKey(arrayKey);
        Integer[] indexArray = pair.getTwo();
        String key = pair.getOne();
        Object tepmObject = null;
        List<Object> tempList = list;
        for (int i = 0; i < indexArray.length; i++) {
            int index = indexArray[i];
            key = key + "[" + index + "]";
            try {
                tepmObject = tempList.get(index);
            } catch (Exception e) {
                throw new MapUtilsOPException("获取集合属性失败！'" + arrayKey + "'中'" + key + "'部分的索引超出范围!", e);
            }

            if (i == indexArray.length - 1) {
                break;
            }
            //元素为null，抛异常
            if (tepmObject == null) {
                throw new MapUtilsOPException("获取集合属性失败！'" + arrayKey + "'中'" + key + "'部分对应的值为null.");
            }
            //元素为List
            if (tepmObject instanceof List) {
                tempList = (List) tepmObject;
            }
            //元素为Set，转化为List
            else if (tepmObject instanceof Set) {
                tempList = new ArrayList<>((Set) tepmObject);
            }
            //元素为数组，转化为List
            else if (tepmObject.getClass().isArray()) {
                tempList = Arrays.asList((Object[]) tepmObject);
            }
            //元素既不是数组又不是集合，抛异常
            else {
                throw new MapUtilsOPException("获取集合元素失败，元素类型错误! '" + arrayKey + "'中'" + key + "'部分对应的元素「" + tepmObject + "」(" + tepmObject.getClass().getName() + ")既不是数组也不是集合");
            }
        }
        return tepmObject;
    }


    /**
     * 拆分数组Key
     *
     * @param arrayKey 数组key
     * @return one :真实key two：元素索引组数
     */
    private static TempPair<String, Integer[]> splitArrayKey(String arrayKey) {
        List<String> indexList = Regular.getArrayByExpression(arrayKey, ARRAY_INDEX);
        Integer[] indexs = new Integer[indexList.size()];
        int i = 0;
        for (String indexStr : indexList) {
            indexs[i++] = Integer.parseInt(indexStr.substring(1, indexStr.length() - 1));
            arrayKey = arrayKey.replaceFirst(ARRAY_INDEX, "");
        }
        return TempPair.of(arrayKey, indexs);
    }

    private static void listPaddin(List<Object> list, int index, Object entry) {
        if (index < 0) {
            throw new RuntimeException("List索引异常：Index:" + index);
        }
        if (index >= 0 && index < list.size()) {
            list.set(index, entry);
        } else {
            int addNum = index - (list.size() - 1);
            for (int i = 0; i < addNum - 1; i++) {
                list.add(null);
            }
            list.add(entry);
        }
    }

    private static Object[] arrayPaddin(Object[] array, int index, Object entry) {
        if (index < 0) {
            throw new RuntimeException("数组索引异常：Index:" + index);
        }
        if (index >= 0 && index < array.length) {
            array[index] = entry;
        } else {
            Object[] newArray = new Object[index + 1];
            for (int i = 0; i < array.length; i++) {
                newArray[i] = array[i];
            }
            newArray[index] = entry;
            array = newArray;
        }
        return array;
    }

    /**
     * 将实体转化为Map
     *
     * @param entity 实体
     * @return 转化后的Map
     */
    public static Map<String, Object> entityToMap(Object entity) {
        try {
            return ConversionUtils.conversion(entity, new SerializationTypeToken<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new MapUtilsOPException(e);
        }
    }

    /**
     * 判断一个key是否为数组key
     *
     * @param key
     * @return
     */
    private static boolean isArrayKey(String key) {
        return Regular.check(key, ARRAY_IDENTIFICATION);
    }

    private static TempPair<String, Integer> getArrayKeyInfo(String arrayKey) {
        arrayKey = arrayKey.trim();
        int index = arrayKey.indexOf("[");
        String key = arrayKey.substring(0, index);
        Integer arrayIndex = Integer.parseInt((arrayKey.substring(index + 1, arrayKey.length() - 1)).trim());
        return TempPair.of(key, arrayIndex);
    }


    public static void main(String[] args) {
        Map<String, Object> map = new HashMap<>();
        put(map, "lucky.datasource.defaultDB.url", "jdbc.url");
        put(map, "lucky.datasource.defaultDB.driver", "jdbc.driver");
        put(map, "lucky.datasource.defaultDB.root", "root");
        put(map, "lucky.datasource.defaultDB.password", "123456");
        put(map, "lucky.import[0]", "data.json");
        put(map, "lucky.import[1]", "source.properties");
        put(map, "lucky.import[2][0]", "1-test");
        put(map, "lucky.import[2][1]", "2-test");
        String[] strArr = {"one", "two"};
        put(map, "lucky.import[2][2]", strArr);
        put(map, "lucky.import[2][2][3]", "three");
        Object a = get(map, "lucky.import[2][2]");
        System.out.println(Arrays.toString((Object[]) a));
        System.out.println(map);

        ConfigurationMap cmap = new ConfigurationMap();
        cmap.addProperty("1.2", "one");
        System.out.println(cmap.get("1.2"));
        System.out.println(cmap.getProperty("1.2"));

    }

}
