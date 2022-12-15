//package com.luckyframework.environment;
//
//import com.luckyframework.common.ContainerUtils;
//import com.luckyframework.common.MapUtils;
//import com.luckyframework.common.Regular;
//import com.luckyframework.exception.GetConfigurationInfoException;
//import org.apache.commons.jexl3.JexlContext;
//import org.apache.commons.jexl3.MapContext;
//import org.apache.commons.jexl3.internal.Engine;
//import org.springframework.util.Assert;
//
//import java.util.*;
//
///**
// * 基于Jexl的数据单元
// * @author fk7075
// * @version 1.0.0
// * @date 2021/7/18 下午5:55
// */
//@SuppressWarnings("unchecked")
//public class JexlStorageUnit implements StorageUnit{
//
//    private final static String PREFIX = "$LUCKY";
//
//    private final JexlContext context = new MapContext();
//    private final Engine engine = new Engine();
//    private Map<String, Object> realMap;
//
//    public JexlStorageUnit(Map<String,?> data){
//        this.context.set(PREFIX,data);
//    }
//
//    @Override
//    public Object parsSingleExpression(String single$Expression) {
//        Object evaluate=null;
//        String prefix=single$Expression.substring(2,single$Expression.length()-1).trim();
//        prefix = prefix.contains(":")?prefix.substring(0,prefix.lastIndexOf(":")):prefix;
//        StringBuilder sb=new StringBuilder(PREFIX);
//
//        //先尝试将这个key作为整体来获取value，如果获取不到则使用分布获取
//        sb.append(".'").append(prefix).append("'");
//        evaluate =getValueByContext(sb.toString(), single$Expression);
//        if(evaluate == null){
//            sb=new StringBuilder(PREFIX);
//            String[] split = prefix.split("\\.");
//            for (String key : split) {
//                if(key.contains("-")){
//                    sb.append(".'").append(key).append("'");
//                }else{
//                    sb.append(".").append(key);
//                }
//            }
//            evaluate =getValueByContext(sb.toString(), single$Expression);
//        }
//        return (evaluate instanceof String) ? parsExpression(evaluate) : evaluate;
//    }
//
//    @Override
//    public Object parsExpression(Object $Expression) {
//        if($Expression instanceof String){
//            String prefix= (String) $Expression;
//            List<String> expression = Regular.getArrayByExpression(prefix, Regular.$_$);
//            if(ContainerUtils.isEmptyCollection(expression)){
//                return prefix;
//            }
//            if(expression.size()==1 && isExpression(prefix)){
//                return parsSingleExpression(prefix);
//            }
//            for (String $exp : expression) {
//                Object value = parsSingleExpression($exp);
//                Assert.notNull(value,"在解析表达式`"+$Expression+"`时出现异常：`"+$exp+"`的解析值为NULL");
//                if(value instanceof String){
//                    prefix=prefix.replace($exp,(String) parsExpression(value.toString()));
//                }else{
//                    prefix=prefix.replace($exp,value.toString());
//                }
//            }
//            return prefix;
//        }
//        return $Expression;
//    }
//
//    @Override
//    public Map<String, Object> getRealMap() {
//        if(realMap == null){
//            realMap = (Map<String, Object>) getRealMap(getOriginalMap());
//        }
//        return this.realMap;
//    }
//
//    @Override
//    public Map<String, Object> getOriginalMap() {
//        return (Map<String,Object>)context.get(PREFIX);
//    }
//
//    @Override
//    public void setProperties(String key, Object value) {
//        if(key.contains(".")){
//            String newKey = key.replaceAll("\\.",":");
//            MapUtils.put(getOriginalMap(),newKey,value);
//            MapUtils.put(getRealMap(),newKey,changeToReal(value));
//        }else{
//            getOriginalMap().put(key, value);
//            getRealMap().put(key,changeToReal(value));
//        }
//    }
//
//    @Override
//    public Object getRealValue(String key) {
//        return getRealMap().get(key);
//    }
//
//    @Override
//    public Object changeToReal(Object value){
//        if(value instanceof Collection){
//            return getRealCollection((Collection<?>) value);
//        }
//        //是数组
//        if(value.getClass().isArray()){
//            return getRealArray((Object[])value);
//        }
//        //是Map
//        if(value instanceof Map){
//            return getRealMap((Map<?,?>)value);
//        }
//        //是其他
//        return parsExpression(value);
//    }
//
//    private Object getRealArray(Object[] array) {
//        Object[] realArray = new Object[array.length];
//        for (int i = 0; i < array.length; i++) {
//            Object entry = array[i];
//            //是集合
//            if(entry instanceof Collection){
//                realArray[i] = getRealCollection((Collection<?>)entry);
//            }
//            //是数组
//            else if(entry.getClass().isArray()){
//                realArray[i] = getRealArray((Object[])entry);
//            }
//            //是Map
//            else if(entry instanceof Map){
//                realArray[i] = getRealMap((Map<?,?>)entry);
//            }
//            //是其他
//            else{
//                realArray[i] = parsExpression(entry);
//            }
//        }
//        return realArray;
//    }
//
//    private Object getRealCollection(Collection<?> collection) {
//        List<Object> reaList = new ArrayList<>(collection.size());
//        for (Object entry : collection) {
//            //是集合
//            if(entry instanceof Collection){
//                reaList.add(getRealCollection((Collection<?>)entry));
//            }
//            //是数组
//            else if(entry.getClass().isArray()){
//                reaList.add(getRealArray((Object[])entry));
//            }
//            //是Map
//            else if(entry instanceof Map){
//                reaList.add(getRealMap((Map<?,?>)entry));
//            }
//            //是其他
//            else{
//                reaList.add(parsExpression(entry));
//            }
//        }
//        return (collection instanceof List) ? reaList : new HashSet<>(reaList);
//    }
//
//    private Object getRealMap(Map<?,?> map){
//        Map<Object, Object> realMap = new HashMap<>(map.size());
//        for (Map.Entry<?, ?> entry : map.entrySet()) {
//            Object key = entry.getKey();
//            Object value = entry.getValue();
//            //是集合
//            if(value instanceof Collection){
//                realMap.put(key,getRealCollection((Collection<?>)value));
//            }
//            //是数组
//            else if(value.getClass().isArray()){
//                realMap.put(key,getRealArray((Object[])value));
//            }
//            //是Map
//            else if(value instanceof Map){
//                realMap.put(key,getRealMap((Map<?,?>)value));
//            }
//            //是其他
//            else{
//                realMap.put(key,parsExpression(value));
//            }
//        }
//        return realMap;
//    }
//
//    private Object getValueByContext(String key,String $prefix){
//        boolean haveDefaultValue = $prefix.contains(":");
//        Object defValue = haveDefaultValue ? $prefix.substring($prefix.lastIndexOf(":")+1,$prefix.length()-1) : null;
//        try {
//            Object result = engine.createExpression(key).evaluate(context);
//            if(result == null && haveDefaultValue){
//                result = defValue;
//            }
//            return result;
//        }catch (Exception e){
//            if(haveDefaultValue){
//                return defValue;
//            }
//            throw new GetConfigurationInfoException($prefix,e);
//        }
//    }
//}
