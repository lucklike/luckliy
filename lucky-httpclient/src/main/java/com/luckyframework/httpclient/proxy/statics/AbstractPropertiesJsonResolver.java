package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Objects;

/**
 * 抽象的PropertiesJsonResolver，提供一些公共方法
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/24 19:27
 */
public abstract class AbstractPropertiesJsonResolver implements StaticParamResolver {


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
     * 获取Json属性对应的参数信息
     *
     * @param context    静态参数注解上下文
     * @param expression 表达式
     * @param separation 分隔符
     * @return Json属性对应的参数信息
     */
    protected ParamInfo getPropertyParamInfo(StaticParamAnnContext context, String expression, String separation) {
        TempPair<String, Object> pair = getKVPair(context, expression, separation);
        if (Objects.nonNull(pair.getTwo())) {
            return new ParamInfo(pair.getOne(), pair.getTwo());
        }
        return null;
    }
}
