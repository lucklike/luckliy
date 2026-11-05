package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.generalapi.ai.tool.Param;
import com.luckyframework.httpclient.generalapi.ai.tool.ParamDesc;
import com.luckyframework.httpclient.generalapi.ai.tool.Required;
import com.luckyframework.httpclient.generalapi.ai.tool.Tool;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Schema生成测试夹具：一套覆盖类型映射、命名、嵌套与异常场景的 {@code @Tool} 方法。
 * <pre>
 *     本夹具的注解全部来自 ai.tool 包，与 com.luckyframework.reflect.Param、
 *     lucky-validator 的 @Required 无 import 交叉。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class SchemaToolHolder {

    /**
     * 温度单位，enum按常量声明顺序输出
     */
    public enum Unit {
        C, F
    }

    /**
     * spec里的golden样例：两个基本类型参数，city必填
     */
    @Tool(desc = "查询指定城市的实时天气")
    public String get_weather(@Param(desc = "城市名称，如“北京”", required = true) String city,
                              @Param(desc = "预报天数，默认 1") int days) {
        return city + ":" + days;
    }

    /**
     * 显式命名覆盖参数名
     */
    @Tool(name = "renamed", desc = "参数名被显式覆盖")
    public String rename(@Param(name = "cityName", desc = "城市名", required = true) String city) {
        return city;
    }

    /**
     * 缺desc的@Tool，注册期应当失败
     */
    @Tool
    public String withoutDescription(String city) {
        return city;
    }

    /**
     * 工具名默认取方法名
     */
    @Tool(desc = "名字取自方法名")
    public void defaultToolName(String a) {
    }

    /**
     * 逐类型映射断言的入口
     */
    @Tool(desc = "类型映射全集")
    public void types(String text,
                      int intValue,
                      long longValue,
                      BigInteger bigInteger,
                      double doubleValue,
                      BigDecimal bigDecimal,
                      boolean flag,
                      Unit unit,
                      List<String> tags,
                      String[] names,
                      Date day,
                      Optional<String> maybe,
                      @Param(ignore = true) String secret) {
    }

    /**
     * 显式覆盖类型名
     */
    @Tool(desc = "显式类型")
    public void explicitType(@Param(type = "number", desc = "按字符串接收但声明为数字") String value) {
    }

    /**
     * 单层嵌套POJO
     */
    @Tool(desc = "查询天气的结构化入参")
    public void nested(@Required Query query) {
    }

    /**
     * 只靠 {@link Param} 也能声明必填，与 {@link Required} 等效
     */
    @Tool(desc = "只使用@Param声明必填")
    public void paramOnlyRequired(@Param(name = "day", desc = "日期", required = true) String day) {
    }

    /**
     * 自引用类型
     */
    @Tool(desc = "自引用")
    public void selfReference(SelfNode node) {
    }

    /**
     * 相互引用类型
     */
    @Tool(desc = "相互引用")
    public void mutualReference(MutualA node) {
    }

    /**
     * 超过嵌套深度上限
     */
    @Tool(desc = "深度超限")
    public void tooDeep(Level1 level) {
    }

    /**
     * 无法映射的类型
     */
    @Tool(desc = "不可映射")
    public void unmappable(InputStream stream) {
    }

    /**
     * 裸集合没有元素类型信息
     */
    @Tool(desc = "裸集合")
    @SuppressWarnings("rawtypes")
    public void rawCollection(List list) {
    }

    /**
     * 嵌套对象夹具：city必填、unit为枚举选填
     */
    public static class Query {
        @Required
        @ParamDesc("城市名称")
        private String city;
        private Unit unit;
    }

    public static class SelfNode {
        private String name;
        private SelfNode next;
    }

    public static class MutualA {
        private MutualB other;
    }

    public static class MutualB {
        private MutualA back;
    }

    public static class Level1 {
        private Level2 child;
    }

    public static class Level2 {
        private Level3 child;
    }

    public static class Level3 {
        private Level4 child;
    }

    public static class Level4 {
        private Level5 child;
    }

    public static class Level5 {
        private String value;
    }
}
