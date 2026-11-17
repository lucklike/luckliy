package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.spel.LazyValue;
import org.junit.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link ValueSpacePropertyAccessor} 的单元测试
 * <pre>
 *     重点验证「canRead阶段记录定位快照、read阶段直接复用」之后：
 *     读取结果、命名空间优先级、LazyValue解包、只读语义都保持不变
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-11-17
 */
public class ValueSpacePropertyAccessorTest {

    private final StandardEvaluationContext context = new StandardEvaluationContext();

    /**
     * 能从变量空间中读到普通变量
     */
    @Test
    public void testReadValueFromNamespaceSpace() throws Exception {
        Map<String, Object> target = targetWithArgs(argsWith("userId", "u1"));
        ValueSpacePropertyAccessor accessor = newAccessor();

        assertTrue(accessor.canRead(context, target, "userId"));
        assertEquals("u1", accessor.read(context, target, "userId").getValue());
    }

    /**
     * 复现Spring的真实调用顺序（先canRead再read），结果必须一致
     */
    @Test
    public void testCanReadThenReadReturnsSameValue() throws Exception {
        Map<String, Object> target = targetWithArgs(argsWith("userId", "u1"));
        ValueSpacePropertyAccessor accessor = newAccessor();

        assertTrue(accessor.canRead(context, target, "userId"));
        assertEquals("u1", accessor.read(context, target, "userId").getValue());
        assertEquals("重复读取结果应保持一致", "u1", accessor.read(context, target, "userId").getValue());
    }

    /**
     * 交替读取不同变量时不能命中上一次的快照
     */
    @Test
    public void testInterleavedReadsDoNotUseStaleSnapshot() throws Exception {
        Map<String, Object> args = argsWith("first", "1");
        args.put("second", "2");
        Map<String, Object> target = targetWithArgs(args);
        ValueSpacePropertyAccessor accessor = newAccessor();

        assertTrue(accessor.canRead(context, target, "first"));
        assertTrue(accessor.canRead(context, target, "second"));
        assertEquals("2", accessor.read(context, target, "second").getValue());
        assertEquals("快照不匹配时必须重新定位", "1", accessor.read(context, target, "first").getValue());
    }

    /**
     * 交替读取不同目标对象时不能命中上一个目标的快照
     */
    @Test
    public void testReadFromDifferentTargetsDoesNotUseStaleSnapshot() throws Exception {
        Map<String, Object> firstTarget = targetWithArgs(argsWith("userId", "u1"));
        Map<String, Object> secondTarget = targetWithArgs(argsWith("userId", "u2"));
        ValueSpacePropertyAccessor accessor = newAccessor();

        assertTrue(accessor.canRead(context, firstTarget, "userId"));
        assertEquals("u2", accessor.read(context, secondTarget, "userId").getValue());
    }

    /**
     * 空间中的LazyValue要被解包成真实值
     */
    @Test
    public void testLazyValueUnwrapped() throws Exception {
        Map<String, Object> args = argsWith("lazyVar", LazyValue.of(() -> "lazy-value"));
        Map<String, Object> target = targetWithArgs(args);
        ValueSpacePropertyAccessor accessor = newAccessor();

        assertTrue(accessor.canRead(context, target, "lazyVar"));
        assertEquals("lazy-value", accessor.read(context, target, "lazyVar").getValue());
    }

    /**
     * 多个空间存在同名变量时，取空间列表中靠前的那个
     */
    @Test
    public void testNamespacePriorityFollowsListOrder() throws Exception {
        List<String> namespaces = Arrays.asList("namespaceOne", "namespaceTwo");
        Map<String, Object> target = new HashMap<>();
        target.put("namespaceOne", singleton("name", "from-one"));
        target.put("namespaceTwo", singleton("name", "from-two"));
        ValueSpacePropertyAccessor accessor = new ValueSpacePropertyAccessor(namespaces);

        assertTrue(accessor.canRead(context, target, "name"));
        assertEquals("from-one", accessor.read(context, target, "name").getValue());
        // 快照失效时走兜底路径，优先级同样要保持
        assertEquals("from-one", accessor.read(context, target, "name").getValue());
    }

    /**
     * 变量不存在时canRead为false，read返回null
     */
    @Test
    public void testCanReadFalseWhenNameMissing() throws Exception {
        Map<String, Object> target = targetWithArgs(argsWith("userId", "u1"));
        ValueSpacePropertyAccessor accessor = newAccessor();

        assertFalse(accessor.canRead(context, target, "missing"));
        assertNull(accessor.read(context, target, "missing").getValue());
    }

    /**
     * 只读语义保持不变
     */
    @Test
    public void testCanWriteFalseAndWriteThrows() throws Exception {
        Map<String, Object> target = targetWithArgs(argsWith("userId", "u1"));
        ValueSpacePropertyAccessor accessor = newAccessor();

        assertFalse(accessor.canWrite(context, target, "userId"));
        try {
            accessor.write(context, target, "userId", "new-value");
            fail("write方法不允许被调用");
        } catch (UnsupportedOperationException expected) {
            // 预期异常
        }
    }

    /**
     * 访问器仍然只对Map类型生效
     */
    @Test
    public void testOnlyAppliesToMap() {
        assertArrayEquals(new Class[]{Map.class}, newAccessor().getSpecificTargetClasses());
    }

    private ValueSpacePropertyAccessor newAccessor() {
        return new ValueSpacePropertyAccessor(ValueSpaceConstant.getSpaces());
    }

    private Map<String, Object> targetWithArgs(Map<String, Object> args) {
        Map<String, Object> target = new HashMap<>();
        target.put(ValueSpaceConstant.METHOD_CONTEXT_ARGS_SPACE, args);
        return target;
    }

    private Map<String, Object> argsWith(String name, Object value) {
        Map<String, Object> args = new HashMap<>();
        args.put(name, value);
        return args;
    }

    private Map<String, Object> singleton(String name, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(name, value);
        return map;
    }
}
