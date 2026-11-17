package com.luckyframework.httpclient.proxy.spel;

import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * {@link MethodSpaceMethodResolver} 的单元测试
 * <pre>
 *     重点验证「方法名使用ClassValue缓存」之后，函数解析的优先级与结果保持不变：
 *     对象自身方法 > 命名空间函数 > 普通变量函数 > Map目标中的函数
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-11-17
 */
public class MethodSpaceMethodResolverTest {

    private static final String NAMESPACE_FUNCTION = "namespaceTestFunction";
    private static final String FLAT_FUNCTION = "flatTestFunction";
    private static final String INSTANCE_FUNCTION = "instanceTestFunction";
    private static final String INHERITED_FUNCTION = "inheritedTestFunction";
    private static final String INTERFACE_FUNCTION = "interfaceTestFunction";

    private static final List<TypeDescriptor> NO_ARGS = Collections.emptyList();

    private final EvaluationContext context = new StandardEvaluationContext();

    /**
     * 对象自身存在同名方法时必须让路，交给Spring默认的反射方法解析器处理
     */
    @Test
    public void testTargetObjectOwnMethodTakesPrecedence() throws Exception {
        MethodSpaceMethodResolver resolver = new MethodSpaceMethodResolver(MethodSpaceConstant.getSpaces());
        EvaluationContext functionContext = contextWithNamespaceFunction();

        assertNull("对象自身方法应让路",
                resolver.resolve(functionContext, new OwnMethodTarget(), NAMESPACE_FUNCTION, NO_ARGS));
        assertNotNull("函数本身仍然可被解析，用于证明让路不是没找到",
                resolver.resolve(functionContext, new Object(), NAMESPACE_FUNCTION, NO_ARGS));
    }

    /**
     * 方法定义在父类时同样需要让路，验证缓存包含完整的继承链
     */
    @Test
    public void testInheritedMethodTakesPrecedence() throws Exception {
        MethodSpaceMethodResolver resolver = new MethodSpaceMethodResolver();
        EvaluationContext emptyContext = new StandardEvaluationContext();

        assertNull(resolver.resolve(emptyContext, new ChildTarget(), INHERITED_FUNCTION, NO_ARGS));
    }

    /**
     * 方法来自接口实现时同样需要让路
     */
    @Test
    public void testInterfaceMethodTakesPrecedence() throws Exception {
        MethodSpaceMethodResolver resolver = new MethodSpaceMethodResolver();
        EvaluationContext emptyContext = new StandardEvaluationContext();

        assertNull(resolver.resolve(emptyContext, new InterfaceTarget(), INTERFACE_FUNCTION, NO_ARGS));
    }

    /**
     * 命名空间中的静态函数可以被解析，并且能正确执行
     */
    @Test
    public void testFunctionResolvedFromNamespaceSpace() throws Exception {
        MethodSpaceMethodResolver resolver = new MethodSpaceMethodResolver(MethodSpaceConstant.getSpaces());

        MethodExecutor executor = resolver.resolve(
                contextWithNamespaceFunction(), new Object(), NAMESPACE_FUNCTION, NO_ARGS);

        assertNotNull(executor);
        assertEquals("namespace-result", executor.execute(context, null).getValue());
    }

    /**
     * 无命名空间的扁平函数可以被解析
     */
    @Test
    public void testFunctionResolvedFromFlatVariable() throws Exception {
        StandardEvaluationContext flatContext = new StandardEvaluationContext();
        Map<String, Object> variables = new HashMap<>();
        variables.put(FLAT_FUNCTION, FlatFunctionTarget.class.getMethod(FLAT_FUNCTION));
        flatContext.setVariables(variables);

        MethodSpaceMethodResolver resolver = new MethodSpaceMethodResolver();
        MethodExecutor executor = resolver.resolve(flatContext, new Object(), FLAT_FUNCTION, NO_ARGS);

        assertNotNull(executor);
        assertEquals("flat-result", executor.execute(context, null).getValue());
    }

    /**
     * targetObject本身是Map时，可以从Map中取出静态函数执行
     */
    @Test
    public void testFunctionResolvedFromMapTarget() throws Exception {
        Map<String, Object> target = new HashMap<>();
        target.put(NAMESPACE_FUNCTION, MapFunctionTarget.class.getMethod(NAMESPACE_FUNCTION));

        MethodSpaceMethodResolver resolver = new MethodSpaceMethodResolver();
        MethodExecutor executor = resolver.resolve(new StandardEvaluationContext(), target, NAMESPACE_FUNCTION, NO_ARGS);

        assertNotNull(executor);
        assertEquals("map-result", executor.execute(context, null).getValue());
    }

    /**
     * 实例方法不能被当作函数解析
     */
    @Test
    public void testInstanceMethodIsNotTreatedAsFunction() throws Exception {
        StandardEvaluationContext instanceContext = new StandardEvaluationContext();
        Map<String, Object> variables = new HashMap<>();
        variables.put(INSTANCE_FUNCTION, InstanceFunctionTarget.class.getMethod(INSTANCE_FUNCTION));
        instanceContext.setVariables(variables);

        MethodSpaceMethodResolver resolver = new MethodSpaceMethodResolver();
        assertNull(resolver.resolve(instanceContext, new Object(), INSTANCE_FUNCTION, NO_ARGS));
    }

    /**
     * 同一目标类型反复解析时结果保持稳定，覆盖缓存首次构建与后续命中两条路径
     */
    @Test
    public void testRepeatedResolveKeepsSameResult() throws Exception {
        MethodSpaceMethodResolver resolver = new MethodSpaceMethodResolver(MethodSpaceConstant.getSpaces());
        EvaluationContext functionContext = contextWithNamespaceFunction();
        OwnMethodTarget target = new OwnMethodTarget();

        for (int i = 0; i < 10; i++) {
            assertNull(resolver.resolve(functionContext, target, NAMESPACE_FUNCTION, NO_ARGS));
            assertNotNull(resolver.resolve(functionContext, new Object(), NAMESPACE_FUNCTION, NO_ARGS));
        }
    }

    /**
     * 不同类型的目标对象交替解析时互不干扰
     */
    @Test
    public void testDifferentTargetTypesDoNotInterfere() throws Exception {
        MethodSpaceMethodResolver resolver = new MethodSpaceMethodResolver(MethodSpaceConstant.getSpaces());
        EvaluationContext functionContext = contextWithNamespaceFunction();

        assertNotNull("普通Object上不存在同名方法，应命中函数",
                resolver.resolve(functionContext, new Object(), NAMESPACE_FUNCTION, NO_ARGS));
        assertNull("同名方法存在时应让路",
                resolver.resolve(functionContext, new OwnMethodTarget(), NAMESPACE_FUNCTION, NO_ARGS));
        assertNull("未知函数名不应被误解析",
                resolver.resolve(functionContext, new Object(), INHERITED_FUNCTION, NO_ARGS));
        assertNull("父类方法存在时应让路",
                resolver.resolve(functionContext, new ChildTarget(), INHERITED_FUNCTION, NO_ARGS));
    }

    private EvaluationContext contextWithNamespaceFunction() throws Exception {
        StandardEvaluationContext namespaceContext = new StandardEvaluationContext();
        Map<String, Object> namespaceMap = new HashMap<>();
        namespaceMap.put(NAMESPACE_FUNCTION, NamespaceFunctionTarget.class.getMethod(NAMESPACE_FUNCTION));
        Map<String, Object> variables = new HashMap<>();
        variables.put(MethodSpaceConstant.COMMON_FUNCTION_SPACE, namespaceMap);
        namespaceContext.setVariables(variables);
        return namespaceContext;
    }

    public static class OwnMethodTarget {
        public String namespaceTestFunction() {
            return "own-result";
        }
    }

    public static class ParentTarget {
        public String inheritedTestFunction() {
            return "parent-result";
        }
    }

    public static class ChildTarget extends ParentTarget {
    }

    public interface InterfaceTargetInterface {
        String interfaceTestFunction();
    }

    public static class InterfaceTarget implements InterfaceTargetInterface {
        @Override
        public String interfaceTestFunction() {
            return "interface-result";
        }
    }

    public static class NamespaceFunctionTarget {
        public static String namespaceTestFunction() {
            return "namespace-result";
        }
    }

    public static class FlatFunctionTarget {
        public static String flatTestFunction() {
            return "flat-result";
        }
    }

    public static class MapFunctionTarget {
        public static String namespaceTestFunction() {
            return "map-result";
        }
    }

    public static class InstanceFunctionTarget {
        public String instanceTestFunction() {
            return "instance-result";
        }
    }
}
