package com.luckyframework.idea.debug.invoke

import com.intellij.openapi.module.ModuleManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 调试目标的请求路径解析。
 *
 * 头部按「方法徽标 + 完整路径」展示，数据必须与侧边栏走同一套注解规则：
 * 请求方法沿元注解链读取，baseUrl 取类级 `url`，path 取方法级 `value`。
 */
class DebugTargetFactoryTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/proxy/annotations/RequestMethod.java",
            """
            package com.luckyframework.httpclient.proxy.annotations;

            public enum RequestMethod {
                GET, POST, NON
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/proxy/annotations/HttpRequest.java",
            """
            package com.luckyframework.httpclient.proxy.annotations;

            public @interface HttpRequest {
                String url() default "";
                String func() default "";
                RequestMethod method() default RequestMethod.NON;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/luckyframework/httpclient/proxy/annotations/Get.java",
            """
            package com.luckyframework.httpclient.proxy.annotations;

            @HttpRequest(method = RequestMethod.GET)
            public @interface Get {
                String value() default "";
            }
            """.trimIndent()
        )
    }

    fun testTargetCarriesRequestPathFromAnnotations() {
        val target = targetOf(
            """
            package p;

            import com.luckyframework.httpclient.proxy.annotations.Get;
            import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

            @HttpRequest(url = "http://localhost:8080")
            public interface Api {

                @Get("/users")
                String users();
            }
            """
        )

        assertEquals("GET", target.httpMethod)
        assertEquals("http://localhost:8080", target.baseUrl)
        assertEquals("/users", target.requestPath)
    }

    fun testTargetCarriesReturnTypeFields() {
        myFixture.addFileToProject(
            "p/PageResult.java",
            """
            package p;

            public class PageResult {
                /** 总数 */
                private long total;
                private String name;
            }
            """.trimIndent()
        )
        val target = targetOf(
            """
            package p;

            public interface Api {

                PageResult query();
            }
            """
        )

        val returnType = target.returnType ?: error("return type should be collected")
        assertEquals("result", returnType.name)
        assertEquals(DebugParameterKind.OBJECT, returnType.kind)
        assertEquals(listOf("total", "name"), returnType.fields.map { it.name })
        assertEquals("总数", returnType.fields.first().description)
    }

    fun testMethodWithoutHttpAnnotationKeepsRequestPathEmpty() {
        val target = targetOf(
            """
            package p;

            public interface Api {

                String legacy();
            }
            """
        )

        assertNull(target.httpMethod)
        assertNull(target.baseUrl)
        assertNull(target.requestPath)
    }

    private fun targetOf(text: String): DebugTarget {
        myFixture.configureByText("Api.java", text.trimIndent())
        val method = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiMethod::class.java)
            .first { !it.isConstructor }
        val module = ModuleManager.getInstance(project).modules.first()
        return DebugTargetFactory.of(module, method)
            ?: error("DebugTargetFactory returned null")
    }
}
