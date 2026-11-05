package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.generalapi.ai.tool.Param;
import com.luckyframework.httpclient.generalapi.ai.tool.Required;
import com.luckyframework.httpclient.generalapi.ai.tool.Tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 注解注册测试夹具：一组覆盖各种返回值形状与参数形状的 {@code @Tool} 方法，
 * 并统计每个方法被真实执行的次数，用于断言「多次列举/多轮调用不重复反射」。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class AnnotationToolHolder {

    /**
     * 每个工具方法被执行的次数，按调用顺序记录方法名
     */
    private final List<String> executed = new ArrayList<>();

    public List<String> executed() {
        return executed;
    }

    /**
     * 与 golden schema 样例同名的两个基本类型参数
     */
    @Tool(desc = "查询指定城市的实时天气")
    public String get_weather(@Param(desc = "城市名称，如“北京”", required = true) String city,
                              @Param(desc = "预报天数，默认 1") int days) {
        executed.add("get_weather");
        return city + "晴，" + days + "天";
    }

    /**
     * 返回POJO，结果内容应为JSON文本
     */
    @Tool(desc = "返回一个对象")
    public Weather weather() {
        executed.add("weather");
        return new Weather(12, "晴");
    }

    /**
     * 返回Map，结果内容应为JSON文本
     */
    @Tool(desc = "返回一个Map")
    public Map<String, Object> mapResult(@Param(desc = "城市名", required = true) String city) {
        executed.add("mapResult");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("city", city);
        result.put("temp", 12);
        return result;
    }

    /**
     * 返回集合，结果内容应为JSON文本
     */
    @Tool(desc = "返回一个集合")
    public List<String> listResult() {
        executed.add("listResult");
        return Arrays.asList("晴", "风");
    }

    /**
     * 返回基本类型，结果内容为其文本
     */
    @Tool(desc = "返回一个数字")
    public int count(@Param(desc = "倍数") int factor) {
        executed.add("count");
        return 2 * factor;
    }

    /**
     * POJO参数：模型给的参数对象要能绑定回方法入参
     */
    @Tool(desc = "结构化入参")
    public String location(@Required Location location) {
        executed.add("location");
        return location.getCity() + "/" + location.getUnit();
    }

    /**
     * 天气结果对象
     */
    public static class Weather {

        private final int temp;
        private final String weather;

        public Weather(int temp, String weather) {
            this.temp = temp;
            this.weather = weather;
        }

        public int getTemp() {
            return temp;
        }

        public String getWeather() {
            return weather;
        }
    }

    /**
     * 结构化入参对象
     */
    public static class Location {

        private String city;
        private String unit;

        public String getCity() {
            return city;
        }

        public String getUnit() {
            return unit;
        }
    }
}
