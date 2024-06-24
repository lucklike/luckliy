package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempPair;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 抽象的PropertiesJsonResolver，
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/24 19:27
 */
public abstract class AbstractPropertiesJsonResolver implements StaticParamResolver {

    private final Pattern EASY_KEY = Pattern.compile("'[\\S\\s]+?'");

    /**
     * 判断是否是简单key
     *
     * @param key key
     * @return true/false
     */
    private boolean isEasyKey(String key) {
        return key.startsWith("'") && key.endsWith("'");
    }

    /**
     * 获取简单key
     *
     * @param key key
     * @return 简单key
     */
    private String getEasyKey(String key) {
        return key.substring(1, key.length() - 1);
    }

    /**
     * 添加对象
     *
     * @param configMap ConfigurationMap
     * @param name      参数名
     * @param value     参数值
     */
    protected void addObject(ConfigurationMap configMap, String name, Object value) {
        String[] keys = keyList(name);

        if (isEasyKey(name)) {
            configMap.put(getEasyKey(name), value);
        } else {
            configMap.addProperty(name, value);
        }
    }

    /**
     * 获取参数名和参数值所组成的Pair
     *
     * @param context    静态参数注解上下文
     * @param expression 表达式
     * @param separation 分隔符
     * @return 参数名和参数值所组成的Pair
     */
    protected TempPair<String, Object> getKVPair(StaticParamAnnContext context, String expression, String separation) {
        int index = expression.indexOf(separation);
        if (index == -1) {
            throw new IllegalArgumentException("Wrong static parameter expression: '" + expression + "'. Please use the correct separator: '" + separation + "'");
        }
        String name = context.parseExpression(expression.substring(0, index), String.class);
        Object value = context.parseExpression(expression.substring(index + 1));
        return TempPair.of(name, value);
    }

    /**
     * 将表达式解析为对象并添加到ConfigurationMap中
     *
     * @param context    静态参数注解上下文
     * @param configMap  ConfigurationMap对象
     * @param expression 表达式
     * @param separation 分隔符
     */
    protected void addObjectByExpression(StaticParamAnnContext context, ConfigurationMap configMap, String expression, String separation) {
        TempPair<String, Object> pair = getKVPair(context, expression, separation);
        if (Objects.nonNull(pair.getTwo())) {
            addObject(configMap, pair.getOne(), pair.getTwo());
        }
    }

    private String[] keyList(String key) {
        TempPair<String[], List<String>> pair = StringUtils.regularCut(key, EASY_KEY);
        String[] one = pair.getOne();
        List<String> two = pair.getTwo();
        String[] keys = new String[one.length + two.size()];
        for (int i = 0, j = 0; i < one.length; i++, j += 2) {
            keys[j] = one[i];
        }
        for (int i = 0, j = 1; i < two.size(); i++, j += 2) {
            keys[j] = two.get(i);
        }
        return keys;
    }
}
