package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.FlatBean;
import com.luckyframework.common.ObjectUtils;
import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * 抽象的PropertiesJsonResolver，提供一些公共方法
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/24 19:27
 */
public abstract class AbstractPropertiesJsonResolver implements StaticParamResolver {


    /**
     * 把一组「参数名+分隔符+参数值」形式的表达式解析成一个JSON结构参数。
     * <pre>
     *     每条表达式交由 {@link #getPropertyParamInfo} 解析，没有值的表达式直接跳过；
     *     有效结果依次写入同一个 {@link FlatBean}，由FlatBean负责把 {@code a.b.c}、{@code [0].name}
     *     这类路径展开成嵌套的对象/数组。
     *     容器类型只取决于第一条有效表达式的参数名：以数组下标开头时用List，否则用Map，
     *     所以同一组表达式需保持同构；全部表达式都没有值时，返回的参数信息里FlatBean为null。
     * </pre>
     *
     * @param context   静态参数注解上下文，负责表达式里SpEL的解析
     * @param expArray  「参数名+分隔符+参数值」形式的表达式数组
     * @param separator 表达式中参数名与参数值的分隔符
     * @return 只含一个元素的列表：元素名为空串，值为合并全部表达式后得到的FlatBean
     */
    protected List<ParamInfo> parserExpression(StaticParamAnnContext context, String[] expArray, String separator) {
        // 容器不在一开始就创建，等第一条有效表达式出现时才能确定用List还是Map
        FlatBean<?> flatBean = null;
        for (String expression : expArray) {
            ParamInfo propertyParamInfo = getPropertyParamInfo(context, expression, separator);
            if (propertyParamInfo == null) {
                // 表达式没有解析出值，不参与JSON结构的拼装
                continue;
            }
            // 参数名即FlatBean可识别的路径，如 a.b.c 与 [0].name
            String key = String.valueOf(propertyParamInfo.getName());
            Object value = propertyParamInfo.getValue();

            if (flatBean == null) {
                if (ObjectUtils.firstIsArrayKey(key)) {
                    // 首个参数名是数组下标形态，整组表达式按数组组织
                    flatBean = FlatBean.of(new ArrayList<>());
                } else {
                    // 否则按对象组织，并用LinkedHashMap保住表达式的书写顺序
                    flatBean = FlatBean.of(new LinkedHashMap<>());
                }
            }

            // 按路径写入，途中缺失的对象/数组层由FlatBean补全
            flatBean.set(key, value);
        }
        // 一组表达式合并为一个JSON参数，因此返回值固定是单元素列表
        return Collections.singletonList(new ParamInfo("", flatBean));

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
     * 获取Json属性对应的参数信息
     *
     * @param context    静态参数注解上下文
     * @param expression 表达式
     * @param separation 分隔符
     * @return Json属性对应的参数信息
     */
    @Nullable
    protected ParamInfo getPropertyParamInfo(StaticParamAnnContext context, String expression, String separation) {
        TempPair<String, Object> pair = getKVPair(context, expression, separation);
        if (Objects.nonNull(pair.getTwo())) {
            return new ParamInfo(pair.getOne(), pair.getTwo());
        }
        return null;
    }
}
