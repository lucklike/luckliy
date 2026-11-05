package com.luckyframework.httpclient.generalapi.ai.tool;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.SchemaToolHolder;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link ToolSchemaGenerator} 的断言：类型映射表、参数名与描述来源、嵌套展开与三类注册期失败。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class ToolSchemaGeneratorTest {

    private static final String GOLDEN_SCHEMA = "ai/tool/schema-get-weather.json";

    private static Method method(String name) {
        for (Method method : SchemaToolHolder.class.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalStateException("Test fixture method not found: " + name);
    }

    private static Map<String, Object> properties(ToolSpec spec) {
        return castMap(spec.getParameters().get("properties"));
    }

    private static Map<String, Object> property(ToolSpec spec, String name) {
        return castMap(properties(spec).get(name));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object node) {
        return (Map<String, Object>) node;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object node) {
        return (List<Object>) node;
    }

    private static Object required(ToolSpec spec) {
        return spec.getParameters().get("required");
    }

    // ------------------------------------------------------------------------------------
    // 2.3 参数名与描述来源
    // ------------------------------------------------------------------------------------

    @Test
    public void goldenSchemaOfTwoPrimitiveParameters() {
        ToolSpec spec = ToolSchemaGenerator.generateSpec(method("get_weather"));

        assertEquals("get_weather", spec.getName());
        assertEquals("查询指定城市的实时天气", spec.getDescription());
        assertEquals("city,days", String.join(",", properties(spec).keySet()));

        Map<String, Object> actual = AiJsonUtils.fromJson(AiJsonUtils.toJson(spec.getParameters()),
                new SerializationTypeToken<Map<String, Object>>() {
                });
        assertEquals(AiFixtures.jsonResource(GOLDEN_SCHEMA), actual);
    }

    @Test
    public void explicitNameOverridesBothPlaces() {
        ToolSpec spec = ToolSchemaGenerator.generateSpec(method("rename"));

        assertEquals(Collections.singletonList("cityName"), list(required(spec)));
        assertFalse("properties里不应再出现反射得到的原名", properties(spec).containsKey("city"));
        assertEquals("string", property(spec, "cityName").get("type"));
    }

    @Test
    public void missingDescriptionFailsTheRegistration() {
        try {
            ToolSchemaGenerator.generateSpec(method("withoutDescription"));
            fail("描述缺失时注册必须失败");
        } catch (ToolRegistryException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("withoutDescription"));
        }
    }

    @Test
    public void toolNameDefaultsToMethodName() {
        assertEquals("defaultToolName", ToolSchemaGenerator.generateSpec(method("defaultToolName")).getName());
    }

    @Test
    public void syntheticParameterNameIsRejected() {
        try {
            ToolSchemaGenerator.checkParameterName("arg0", SchemaToolHolder.class, "get_weather", 1);
            fail("合成参数名不可得时必须抛异常");
        } catch (ToolRegistryException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(SchemaToolHolder.class.getName()));
            assertTrue(e.getMessage(), e.getMessage().contains("get_weather"));
            assertTrue(e.getMessage(), e.getMessage().contains("position 1"));
        }
    }

    // ------------------------------------------------------------------------------------
    // 2.2 类型映射表
    // ------------------------------------------------------------------------------------

    @Test
    public void javaTypesAreMappedToJsonSchemaTypes() {
        ToolSpec spec = ToolSchemaGenerator.generateSpec(method("types"));

        assertEquals("string", property(spec, "text").get("type"));
        assertEquals("integer", property(spec, "intValue").get("type"));
        assertEquals("integer", property(spec, "longValue").get("type"));
        assertEquals("integer", property(spec, "bigInteger").get("type"));
        assertEquals("number", property(spec, "doubleValue").get("type"));
        assertEquals("number", property(spec, "bigDecimal").get("type"));
        assertEquals("boolean", property(spec, "flag").get("type"));
        assertEquals("string", property(spec, "day").get("type"));

        Map<String, Object> unit = property(spec, "unit");
        assertEquals("string", unit.get("type"));
        assertEquals(Arrays.asList("C", "F"), list(unit.get("enum")));

        Map<String, Object> tags = property(spec, "tags");
        assertEquals("array", tags.get("type"));
        assertEquals("string", castMap(tags.get("items")).get("type"));

        Map<String, Object> names = property(spec, "names");
        assertEquals("array", names.get("type"));
        assertEquals("string", castMap(names.get("items")).get("type"));

        // Optional按包裹类型处理且不进required
        assertEquals("string", property(spec, "maybe").get("type"));
        assertFalse("没有任何必填声明时不应出现required键", spec.getParameters().containsKey("required"));

        assertFalse("ignore的参数不进入schema", properties(spec).containsKey("secret"));
    }

    @Test
    public void explicitTypeAttributeWins() {
        ToolSpec spec = ToolSchemaGenerator.generateSpec(method("explicitType"));
        Map<String, Object> value = property(spec, "value");
        assertEquals("number", value.get("type"));
        assertEquals("按字符串接收但声明为数字", value.get("description"));
    }

    @Test
    public void unmappableTypeFailsWithoutProducingASchema() {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                ToolSchemaGenerator.generateSpec(method("unmappable"));
                fail("InputStream无法映射，注册必须失败");
            } catch (ToolRegistryException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("java.io.InputStream"));
            }
        }
    }

    @Test
    public void rawCollectionHasNoElementType() {
        try {
            ToolSchemaGenerator.generateSpec(method("rawCollection"));
            fail("裸List没有元素类型，注册必须失败");
        } catch (ToolRegistryException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("java.util.List"));
        }
    }

    // ------------------------------------------------------------------------------------
    // 2.4 嵌套展开、引用检测与深度上限
    // ------------------------------------------------------------------------------------

    @Test
    public void nestedObject() {
        ToolSpec spec = ToolSchemaGenerator.generateSpec(method("nested"));

        assertEquals(Collections.singletonList("query"), list(required(spec)));
        Map<String, Object> query = property(spec, "query");
        assertEquals("object", query.get("type"));
        assertEquals(Collections.singletonList("city"), list(query.get("required")));

        Map<String, Object> city = castMap(castMap(query.get("properties")).get("city"));
        assertEquals("string", city.get("type"));
        assertEquals("城市名称", city.get("description"));

        Map<String, Object> unit = castMap(castMap(query.get("properties")).get("unit"));
        assertEquals("string", unit.get("type"));
        assertEquals(Arrays.asList("C", "F"), list(unit.get("enum")));
    }

    @Test
    public void requiredShorthandEqualsParamAttribute() {
        ToolSpec shorthand = ToolSchemaGenerator.generateSpec(method("nested"));
        ToolSpec explicit = ToolSchemaGenerator.generateSpec(method("paramOnlyRequired"));
        assertEquals("@Required与@Param(required=true)落在同一层",
                list(required(shorthand)).size(), list(required(explicit)).size());
        assertEquals(Collections.singletonList("day"), list(required(explicit)));
    }

    @Test
    public void selfReference() {
        try {
            ToolSchemaGenerator.generateSpec(method("selfReference"));
            fail("自引用类型必须报错");
        } catch (ToolRegistryException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("SelfNode"));
            assertTrue("异常里要给出引用链", e.getMessage().contains("->"));
        }
    }

    @Test
    public void mutualReference() {
        try {
            ToolSchemaGenerator.generateSpec(method("mutualReference"));
            fail("相互引用类型必须报错");
        } catch (ToolRegistryException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("MutualA"));
            assertTrue(e.getMessage(), e.getMessage().contains("MutualB"));
        }
    }

    @Test
    public void depthLimit() {
        try {
            ToolSchemaGenerator.generateSpec(method("tooDeep"));
            fail("超过深度上限必须报错");
        } catch (ToolRegistryException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(String.valueOf(ToolSchemaGenerator.MAX_DEPTH)));
            assertTrue(e.getMessage(), e.getMessage().contains("Level5"));
        }
    }

    @Test
    public void schemaIsGeneratedOncePerMethod() {
        Method method = method("get_weather");
        ConfigurationMap first = ToolSchemaGenerator.generateParameters(method);
        assertSame("生成结果按Method缓存", first, ToolSchemaGenerator.generateParameters(method));
    }
}
