package com.luckyframework.httpclient.proxy.spel;

import org.junit.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link ValueSpaceConstant}与{@link MethodSpaceConstant}的空间列表缓存测试
 * <pre>
 *     重点验证缓存内部反射结果之后：
 *     1.仍然包含全部内置空间常量
 *     2.外部空间注册后立即生效
 *     3.每次返回的是相互独立的列表，调用方修改不会污染缓存
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-11-17
 */
public class SpacesCacheTest {

    /**
     * 变量空间列表包含全部内置常量
     */
    @Test
    public void testValueSpacesContainAllConstants() {
        List<String> spaces = ValueSpaceConstant.getSpaces();

        assertEquals(countStringConstants(ValueSpaceConstant.class) + ValueSpaceConstant.EXTERNAL_SPACES.size(),
                spaces.size());
        assertTrue(spaces.contains(ValueSpaceConstant.PARAM_CONTEXT_SPACE));
        assertTrue(spaces.contains(ValueSpaceConstant.METHOD_CONTEXT_ARGS_SPACE));
        assertTrue(spaces.contains(ValueSpaceConstant.REQUEST_SPACE));
        assertTrue(spaces.contains(ValueSpaceConstant.RESPONSE_SPACE));
    }

    /**
     * 函数空间列表包含全部内置常量
     */
    @Test
    public void testMethodSpacesContainAllConstants() {
        List<String> spaces = MethodSpaceConstant.getSpaces();

        assertEquals(countStringConstants(MethodSpaceConstant.class) + MethodSpaceConstant.EXTERNAL_SPACES.size(),
                spaces.size());
        assertTrue(spaces.contains(MethodSpaceConstant.COMMON_FUNCTION_SPACE));
        assertTrue(spaces.contains(MethodSpaceConstant.SERIALIZATION_FUNCTION_SPACE));
        assertTrue(spaces.contains(MethodSpaceConstant.CIPHER_FUNCTION_SPACE));
    }

    /**
     * 重复调用时内容保持一致
     */
    @Test
    public void testRepeatedCallsReturnSameContent() {
        assertEquals(ValueSpaceConstant.getSpaces(), ValueSpaceConstant.getSpaces());
        assertEquals(MethodSpaceConstant.getSpaces(), MethodSpaceConstant.getSpaces());
    }

    /**
     * 每次返回独立列表，调用方修改不会污染下一次的结果
     */
    @Test
    public void testGetSpacesReturnsIndependentList() {
        List<String> valueSpaces = ValueSpaceConstant.getSpaces();
        int valueSize = valueSpaces.size();
        valueSpaces.clear();
        assertEquals(valueSize, ValueSpaceConstant.getSpaces().size());

        List<String> methodSpaces = MethodSpaceConstant.getSpaces();
        int methodSize = methodSpaces.size();
        methodSpaces.clear();
        assertEquals(methodSize, MethodSpaceConstant.getSpaces().size());
    }

    /**
     * 外部空间注册后立即生效，移除后立即失效
     */
    @Test
    public void testAddExternalSpaceTakesEffectImmediately() {
        String valueSpace = "__Test::ValueExternalSpace__";
        String methodSpace = "__Test::MethodExternalSpace__";
        int valueSize = ValueSpaceConstant.getSpaces().size();
        int methodSize = MethodSpaceConstant.getSpaces().size();

        ValueSpaceConstant.addExternalSpace(valueSpace);
        MethodSpaceConstant.addExternalSpace(methodSpace);
        try {
            List<String> valueSpaces = ValueSpaceConstant.getSpaces();
            List<String> methodSpaces = MethodSpaceConstant.getSpaces();
            assertEquals(valueSize + 1, valueSpaces.size());
            assertEquals(methodSize + 1, methodSpaces.size());
            assertTrue(valueSpaces.contains(valueSpace));
            assertTrue(methodSpaces.contains(methodSpace));
        } finally {
            ValueSpaceConstant.EXTERNAL_SPACES.remove(valueSpace);
            MethodSpaceConstant.EXTERNAL_SPACES.remove(methodSpace);
        }

        assertFalse(ValueSpaceConstant.getSpaces().contains(valueSpace));
        assertFalse(MethodSpaceConstant.getSpaces().contains(methodSpace));
    }

    private long countStringConstants(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()))
                .filter(field -> field.getType() == String.class)
                .count();
    }
}
