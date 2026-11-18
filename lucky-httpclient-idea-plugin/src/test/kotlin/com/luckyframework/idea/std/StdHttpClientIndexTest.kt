package com.luckyframework.idea.std

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile

/**
 * 索引构建：直接标注、组合注解传播、默认 configId、多文件与 PSI 失效。
 */
class StdHttpClientIndexTest : StdFixtureTestCase() {

    fun testDirectInterfaceIsIndexed() {
        addYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    url: https://gitee.com
            """
        )
        val psiClass = configureClass(
            """
            import com.luckyframework.httpclient.proxy.configapi.Api;
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {

                @Api("user-info")
                void getUserInfo();
            }
            """
        )

        val entry = StdHttpClientIndex.getInstance(project).entryOf(psiClass)

        assertNotNull(entry)
        assertEquals("GiteeApi", entry!!.configId)
        assertEquals(1, entry.configNodes.size)
        assertEquals("user-info", entry.methods.single().apiId)
    }

    fun testConfigIdDefaultsToSimpleName() {
        val psiClass = configureClass(
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient
            public interface GiteeApi {
            }
            """
        )

        assertEquals("GiteeApi", StdHttpClientIndex.getInstance(project).entryOf(psiClass)?.configId)
    }

    fun testCompositeAnnotationIsIndexed() {
        addCompositeAnnotation("MyApi", "")
        val psiClass = configureClass(
            """
            @MyApi("CompositeApi")
            public interface GiteeApi {
            }
            """
        )

        assertEquals("CompositeApi", StdHttpClientIndex.getInstance(project).entryOf(psiClass)?.configId)
    }

    fun testCompositeAnnotationDeclarationDefaultIsIndexed() {
        addCompositeAnnotation("MyApi", "DeclaredApi")
        val psiClass = configureClass(
            """
            @MyApi
            public interface GiteeApi {
            }
            """
        )

        assertEquals("DeclaredApi", StdHttpClientIndex.getInstance(project).entryOf(psiClass)?.configId)
    }

    fun testMissingConfigKeepsEntry() {
        val psiClass = configureClass(
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "NoConfigApi")
            public interface NoConfigApi {
            }
            """
        )

        val entry = StdHttpClientIndex.getInstance(project).entryOf(psiClass)

        assertNotNull(entry)
        assertTrue(entry!!.configNodes.isEmpty())
    }

    fun testConfigAddedAfterInterfaceIsVisible() {
        val psiClass = configureClass(
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "LateApi")
            public interface LateApi {
            }
            """
        )
        val index = StdHttpClientIndex.getInstance(project)
        assertTrue(index.entryOf(psiClass)!!.configNodes.isEmpty())

        addYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  LateApi:
                    url: https://late.example.com
            """
        )

        assertEquals(1, index.entryOf(psiClass)!!.configNodes.size)
    }

    fun testDuplicateConfigIdReturnsAllFiles() {
        addYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  SharedApi:
                    url: https://a.example.com
            """,
            "a.yml"
        )
        addYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  SharedApi:
                    url: https://b.example.com
            """,
            "b.yml"
        )
        val psiClass = configureClass(
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "SharedApi")
            public interface SharedApi {
            }
            """
        )

        val entry = StdHttpClientIndex.getInstance(project).entryOf(psiClass)!!

        assertEquals(2, entry.configNodes.size)
        assertEquals(1, StdHttpClientIndex.getInstance(project).entriesByConfigId("SharedApi").size)
    }

    fun testMethodNodesResolveToYamlKeys() {
        addYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    method-configs:
                      getUserInfo:
                        path: /user
            """
        )
        val psiClass = configureClass(
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {
                void getUserInfo();
            }
            """
        )

        val method = psiClass.methods.single()
        val nodes = StdHttpClientIndex.getInstance(project).methodNodesOf(method)

        assertEquals(1, nodes.size)
        assertEquals("getUserInfo", nodes.single().text.substringBefore(':'))
    }

    private fun addYaml(text: String, name: String = "config.yml") {
        myFixture.addFileToProject(name, text.trimIndent())
    }

    private fun addCompositeAnnotation(name: String, configId: String) {
        val value = if (configId.isEmpty()) "" else "configId = \"$configId\""
        myFixture.addFileToProject(
            "$name.java",
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;
            import org.springframework.core.annotation.AliasFor;

            @StdHttpClient($value)
            public @interface $name {

                @AliasFor(annotation = StdHttpClient.class, attribute = "configId")
                String value() default "";
            }
            """.trimIndent()
        )
    }

    private fun configureClass(text: String): PsiClass {
        myFixture.configureByText("Api.java", text.trimIndent())
        return (myFixture.file as PsiJavaFile).classes.single()
    }
}
