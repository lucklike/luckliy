package com.luckyframework.httpclient.generalapi.ai.tool;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.AnnotationToolHolder;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * 注解入口的注册与执行断言：{@link ToolRegistry#register(Object)} 扫描{@link Tool}方法生成声明，
 * 并把该方法本身绑定为执行实现；同时断言执行结果的序列化规则。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class AnnotationToolRegistrationTest {

    private static final String GOLDEN_SCHEMA = "ai/tool/schema-get-weather.json";

    private AnnotationToolHolder holder;
    private ToolRegistry registry;

    @Before
    public void setUp() {
        holder = new AnnotationToolHolder();
        registry = new ToolRegistry().register(holder);
    }

    private Object execute(String toolName, Map<String, Object> arguments) throws Throwable {
        Optional<ToolExecutor> executor = registry.findExecutor(toolName);
        assertTrue("执行实现应当随注解一起绑定: " + toolName, executor.isPresent());
        return executor.get().execute(arguments);
    }

    private static Map<String, Object> args(Object... keyValues) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            arguments.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return arguments;
    }

    // ------------------------------------------------------------------------------------
    // 2.5 声明生成与执行绑定
    // ------------------------------------------------------------------------------------

    @Test
    public void annotationGeneratedSpecMatchesTheGoldenSchema() {
        ToolSpec spec = registry.getSpec("get_weather");

        assertEquals("get_weather", spec.getName());
        assertEquals("查询指定城市的实时天气", spec.getDescription());
        Map<String, Object> actual = AiJsonUtils.fromJson(AiJsonUtils.toJson(spec.getParameters()),
                new SerializationTypeToken<Map<String, Object>>() {
                });
        assertEquals(AiFixtures.jsonResource(GOLDEN_SCHEMA), actual);
    }

    @Test
    public void registeredToolIsExecutableThroughItsAnnotatedMethod() throws Throwable {
        assertEquals("北京晴，1天", execute("get_weather", args("city", "北京", "days", 1)));
        assertEquals(Collections.singletonList("get_weather"), holder.executed());
    }

    @Test
    public void pojoArgumentIsBoundFromTheArgumentMap() throws Throwable {
        Map<String, Object> location = args("city", "北京", "unit", "C");
        assertEquals("北京/C", execute("location", args("location", location)));
    }

    @Test
    public void listingAndCallingDoNotRepeatTheReflectionScan() throws Throwable {
        ToolSpec first = registry.getSpec("get_weather");
        for (int i = 0; i < 3; i++) {
            List<ToolSpec> specs = registry.listSpecs();
            assertTrue(specs.contains(first));
        }
        assertSame("声明在注册期生成一次后即复用", first, registry.getSpec("get_weather"));
        assertTrue("列举不会执行任何工具方法", holder.executed().isEmpty());

        execute("get_weather", args("city", "北京", "days", 1));
        assertEquals("执行一次只调用一次目标方法", 1, holder.executed().size());
    }

    // ------------------------------------------------------------------------------------
    // 2.6 执行结果的序列化规则
    // ------------------------------------------------------------------------------------

    @Test
    public void stringResultIsUsedAsIs() throws Throwable {
        ToolResult result = ToolResult.success(execute("get_weather", args("city", "北京", "days", 1)));
        assertEquals("北京晴，1天", result.getContent());
        assertFalse("字符串结果不该再包一层JSON", result.getContent().startsWith("\""));
    }

    @Test
    public void objectResultIsSerializedAsJson() throws Throwable {
        ToolResult result = ToolResult.success(execute("weather", Collections.emptyMap()));
        assertEquals("{\"temp\":12,\"weather\":\"晴\"}", result.getContent());
    }

    @Test
    public void mapAndCollectionResultsAreSerializedAsJson() throws Throwable {
        assertEquals("{\"city\":\"北京\",\"temp\":12}",
                ToolResult.success(execute("mapResult", args("city", "北京"))).getContent());
        assertEquals("[\"晴\",\"风\"]",
                ToolResult.success(execute("listResult", Collections.emptyMap())).getContent());
    }

    @Test
    public void primitiveResultUsesItsText() throws Throwable {
        assertEquals("6", ToolResult.success(execute("count", args("factor", 3))).getContent());
    }

    @Test
    public void declarationOnlyToolIsListedButHasNoExecutor() {
        ToolRegistry declarationOnly = new ToolRegistry().register(AiFixtures.weatherToolSpec());

        assertEquals(1, declarationOnly.listSpecs().size());
        assertTrue("无执行实现的工具照样出现在报文的工具列表里",
                declarationOnly.getSpec("get_weather").toDeclaration().toString().contains("get_weather"));
        assertFalse(declarationOnly.findExecutor("get_weather").isPresent());

        ToolResult failure = ToolResult.noExecutor("get_weather");
        assertFalse(failure.isSuccess());
        assertTrue(failure.getContent(), failure.getContent().contains("get_weather"));
    }

    @Test
    public void nullHolderFailsTheRegistration() {
        try {
            new ToolRegistry().register((Object) null);
            org.junit.Assert.fail("holder为null时注册必须失败");
        } catch (ToolRegistryException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Tool holder cannot be null"));
        }
    }
}
