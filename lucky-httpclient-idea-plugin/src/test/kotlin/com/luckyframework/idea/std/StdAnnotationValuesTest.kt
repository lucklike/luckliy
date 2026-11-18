package com.luckyframework.idea.std

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile

/**
 * 合并注解属性解析：直接注解、`@ApiConfig` 回退、组合注解与 `@AliasFor` 覆盖。
 */
class StdAnnotationValuesTest : StdFixtureTestCase() {

    fun testConfigIdFromDirectAnnotation() {
        val psiClass = configureClass(
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {
                void getUserInfo();
            }
            """
        )

        assertEquals("GiteeApi", StdAnnotationValues.stdClientConfigId(psiClass))
    }

    fun testConfigIdFallsBackToApiConfig() {
        val psiClass = configureClass(
            """
            import com.luckyframework.httpclient.proxy.configapi.ApiConfig;
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @ApiConfig("SharedApi")
            @StdHttpClient
            public interface GiteeApi {
            }
            """
        )

        assertEquals("SharedApi", StdAnnotationValues.stdClientConfigId(psiClass))
    }

    fun testConfigIdWithoutExplicitValueReturnsNull() {
        val psiClass = configureClass(
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient
            public interface GiteeApi {
            }
            """
        )

        assertNull(StdAnnotationValues.stdClientConfigId(psiClass))
    }

    fun testConfigIdFromCompositeAnnotationAlias() {
        myFixture.addFileToProject(
            "MyApi.java",
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;
            import org.springframework.core.annotation.AliasFor;

            @StdHttpClient
            public @interface MyApi {

                @AliasFor(annotation = StdHttpClient.class, attribute = "configId")
                String value() default "";
            }
            """
        )
        val psiClass = configureClass(
            """
            @MyApi("CompositeApi")
            public interface GiteeApi {
            }
            """
        )

        assertEquals("CompositeApi", StdAnnotationValues.stdClientConfigId(psiClass))
        assertTrue(
            StdAnnotationValues.carriesStdHttpClient(
                psiClass.modifierList!!.annotations.single()
            )
        )
    }

    fun testApiIdFromApi() {
        val psiClass = configureClass(
            """
            import com.luckyframework.httpclient.proxy.configapi.Api;
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient
            public interface GiteeApi {

                @Api("user-info")
                void getUserInfo();
            }
            """
        )

        assertEquals("user-info", StdAnnotationValues.apiId(psiClass.methods.single()))
    }

    fun testApiIdWithoutApiReturnsNull() {
        val psiClass = configureClass(
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient
            public interface GiteeApi {
                void getUserInfo();
            }
            """
        )

        assertNull(StdAnnotationValues.apiId(psiClass.methods.single()))
    }

    fun testConfigIdAttributeDetection() {
        val psiClass = configureClass(
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {
            }
            """
        )
        val annotation = psiClass.modifierList!!.annotations.single()

        assertTrue(StdAnnotationValues.isConfigIdAttribute(annotation, "configId"))
        assertFalse(StdAnnotationValues.isConfigIdAttribute(annotation, "path"))
    }

    private fun configureClass(text: String): PsiClass {
        myFixture.configureByText("Api.java", text.trimIndent())
        return (myFixture.file as PsiJavaFile).classes.single()
    }
}
