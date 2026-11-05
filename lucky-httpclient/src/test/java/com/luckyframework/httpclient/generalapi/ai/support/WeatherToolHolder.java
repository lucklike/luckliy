package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.generalapi.ai.tool.Param;
import com.luckyframework.httpclient.generalapi.ai.tool.Tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 端到端验收夹具：与 {@link AiFixtures#weatherToolSpec()} 语义一致的注解版工具，
 * 用来证明注解生成与编程式登记两条来源在注册表内是同一数据结构。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class WeatherToolHolder {

    /**
     * 每次执行收到的参数，按调用顺序记录
     */
    private final List<Map<String, Object>> invocations = new ArrayList<>();

    public List<Map<String, Object>> invocations() {
        return invocations;
    }

    /**
     * 与回环样本的调用标识、参数文本一一对应的工具实现
     */
    @Tool(desc = "查询城市天气")
    public Map<String, Object> get_weather(@Param(desc = "城市名", required = true) String city) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("city", city);
        invocations.add(arguments);

        Map<String, Object> weather = new LinkedHashMap<>();
        weather.put("temp", 12);
        weather.put("weather", "晴");
        return weather;
    }
}
