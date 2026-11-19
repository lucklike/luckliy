package com.luckyframework.idea.debug.invoke

import com.intellij.openapi.module.ModuleManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 调试参数收集的 fixture 测试。
 *
 * 覆盖运行期最容易出错的三个边界：`@Param` 名称覆盖与编译期参数名回退、
 * 数组/可变参数/枚举的 kind、泛型与类型变量的擦除结果。
 */
class DebugParameterCollectorTest : BasePlatformTestCase() {

    private val collector = DebugParameterCollector()

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/luckyframework/reflect/Param.java",
            """
            package com.luckyframework.reflect;

            public @interface Param {
                String value() default "";
            }
            """.trimIndent()
        )
        // MockJdk 不含 java.lang/java.util，补上测试用到的类型，
        // 否则 PsiClassType.resolve() 返回 null，类型名会退化为短名
        myFixture.addFileToProject("java/lang/String.java", "package java.lang; public class String {}")
        myFixture.addFileToProject("java/lang/Number.java", "package java.lang; public abstract class Number {}")
        myFixture.addFileToProject("java/util/List.java", "package java.util; public interface List<E> {}")
        myFixture.addFileToProject("java/util/Map.java", "package java.util; public interface Map<K, V> {}")
        myFixture.addFileToProject("java/io/InputStream.java", "package java.io; public abstract class InputStream {}")
    }

    fun testParamNameUsesAnnotationAndFallsBackToSourceName() {
        configure(
            """
            package p;

            import com.luckyframework.reflect.Param;

            class Api {
                void save(@Param("uid") String token, String plain, @Param("") String empty) {
                }
            }
            """
        )

        val parameters = collect("save")

        assertEquals(listOf("uid", "plain", "empty"), parameters.map { it.name })
    }

    fun testArrayAndVarargKinds() {
        configure(
            """
            package p;

            class Api {
                void accept(String[] names, byte[] bytes, int[] numbers, String... more) {
                }
            }
            """
        )

        val parameters = collect("accept")

        assertEquals("java.lang.String[]", parameters[0].typeName)
        assertEquals(DebugParameterKind.LIST, parameters[0].kind)
        assertEquals("byte[]", parameters[1].typeName)
        assertEquals(DebugParameterKind.BYTES, parameters[1].kind)
        assertEquals("int[]", parameters[2].typeName)
        assertEquals(DebugParameterKind.LIST, parameters[2].kind)
        assertEquals("java.lang.String[]", parameters[3].typeName)
        assertEquals(DebugParameterKind.LIST, parameters[3].kind)
    }

    fun testEnumConstants() {
        configure(
            """
            package p;

            class Api {
                enum Color { RED, GREEN }

                void paint(Color color) {
                }
            }
            """
        )

        val parameter = collect("paint").single()

        assertEquals("p.Api.Color", parameter.typeName)
        assertEquals(DebugParameterKind.ENUM, parameter.kind)
        assertEquals(listOf("RED", "GREEN"), parameter.enumConstants)
    }

    fun testGenericTypesAreErased() {
        configure(
            """
            package p;

            import java.util.List;
            import java.util.Map;

            class Api {
                <T> void generic(T value, List<String> strings, Map<String, Integer> map,
                                  List<? extends Number> numbers, T[] values) {
                }
            }
            """
        )

        val parameters = collect("generic")

        assertEquals("java.lang.Object", parameters[0].typeName)
        assertEquals(DebugParameterKind.OBJECT, parameters[0].kind)
        assertEquals("java.util.List", parameters[1].typeName)
        assertEquals(DebugParameterKind.LIST, parameters[1].kind)
        assertEquals("java.util.Map", parameters[2].typeName)
        assertEquals(DebugParameterKind.MAP, parameters[2].kind)
        assertEquals("java.util.List", parameters[3].typeName)
        assertEquals(DebugParameterKind.LIST, parameters[3].kind)
        assertEquals("java.lang.Object[]", parameters[4].typeName)
        assertEquals(DebugParameterKind.LIST, parameters[4].kind)
    }

    fun testTargetFactoryBuildsFromCollectedParameters() {
        configure(
            """
            package p;

            import com.luckyframework.reflect.Param;

            class Api {
                void save(@Param("uid") String token) {
                }
            }
            """
        )

        val method = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiMethod::class.java)
            .first { it.name == "save" }
        val module = ModuleManager.getInstance(project).modules.first()
        val target = DebugTargetFactory.of(module, method)
            ?: error("DebugTargetFactory returned null")

        assertEquals("p.Api", target.className)
        assertEquals("save", target.methodName)
        assertEquals("save(java.lang.String)", target.signature)
        assertEquals(listOf("uid"), target.parameters.map { it.name })
    }

    fun testUnsupportedStreamParameter() {
        configure(
            """
            package p;

            import java.io.InputStream;

            class Api {
                void read(InputStream stream) {
                }
            }
            """
        )

        val parameter = collect("read").single()

        assertEquals(DebugParameterKind.UNSUPPORTED, parameter.kind)
        assertTrue(parameter.unsupportedReason?.contains("流类型") == true)
    }

    private fun configure(text: String) {
        myFixture.configureByText("Api.java", text.trimIndent())
    }

    private fun collect(methodName: String): List<DebugParameter> {
        val method = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiMethod::class.java)
            .first { it.name == methodName }
        return collector.collect(method)
    }
}
