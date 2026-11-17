package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.spel.LazyValue;
import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.SpELRuntime;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * 使用真实{@link SpELRuntime}与{@link LuckyHttpClientEvaluationContextFactory}做的端到端回归
 * <pre>
 *     覆盖变量空间读取、命名空间函数、扁平函数、对象自身方法、LazyValue参数五条主链路，
 *     用于证明性能改造前后表达式的求值结果完全一致。
 *     注意：{@link ParamWrapper}直接交给表达式解析器，不会剥离{@code #{}}，
 *     所以这里的表达式不带包裹符号（包裹与剥离发生在Context层）。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-11-17
 */
public class SpelRuntimeIntegrationTest {

    private final SpELRuntime runtime = new SpELRuntime(new LuckyHttpClientEvaluationContextFactory());

    /**
     * 裸变量名从变量空间读取
     */
    @Test
    public void testBareVariableExpression() throws Exception {
        assertEquals("u1", runtime.getValueForType(wrapper("userId")));
    }

    /**
     * 变量空间中的LazyValue参数参与求值
     */
    @Test
    public void testLazyParameterExpression() throws Exception {
        assertEquals("lucky", runtime.getValueForType(wrapper("name")));
    }

    /**
     * 命名空间中的静态函数可以被调用
     */
    @Test
    public void testNamespaceFunctionExpression() throws Exception {
        assertEquals(Boolean.TRUE, runtime.getValueForType(wrapper("strEq(name, 'lucky')")));
    }

    /**
     * 无命名空间的扁平函数可以被调用
     */
    @Test
    public void testFlatFunctionExpression() throws Exception {
        assertEquals("ABC", runtime.getValueForType(wrapper("flatUpper('abc')")));
    }

    /**
     * 普通对象的方法仍然由Spring默认解析器执行
     */
    @Test
    public void testObjectMethodExpression() throws Exception {
        ParamWrapper paramWrapper = wrapper("#text.toUpperCase()");
        paramWrapper.addVariable("text", new TextFixture());
        assertEquals("HELLO", runtime.getValueForType(paramWrapper));
    }

    private ParamWrapper wrapper(String expression) throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("userId", LazyValue.of(() -> "u1"));
        args.put("name", LazyValue.of(() -> "lucky"));

        Map<String, Object> root = new HashMap<>();
        root.put(ValueSpaceConstant.METHOD_CONTEXT_ARGS_SPACE, args);

        Map<String, Object> commonFunctions = new HashMap<>();
        commonFunctions.put("strEq", FunctionFixture.class.getMethod("strEq", Object.class, Object.class));

        Map<String, Object> variables = new HashMap<>();
        variables.put(MethodSpaceConstant.COMMON_FUNCTION_SPACE, commonFunctions);
        variables.put("flatUpper", FunctionFixture.class.getMethod("flatUpper", String.class));

        return new ParamWrapper()
                .setExpression(expression)
                .setRootObject(root)
                .setVariables(variables);
    }

    public static class FunctionFixture {

        public static boolean strEq(Object left, Object right) {
            return Objects.equals(left, right);
        }

        public static String flatUpper(String value) {
            return value == null ? null : value.toUpperCase();
        }
    }

    public static class TextFixture {

        public String toUpperCase() {
            return "HELLO";
        }
    }
}
