package com.luckyframework.idea.spel.environment

import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.reference.SpelDeclaration
import com.luckyframework.idea.spel.yaml.SpringYamlPropertyGotoDeclarationHandler
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * 配置键 → Java 注解中 `${key}` 使用位置的反向跳转。
 */
class SpringPlaceholderNavigationTest : SpringEnvironmentFixtureTestCase() {

    fun testPropertyKeyNavigatesToAnnotation() {
        val api = myFixture.addFileToProject(
            "src/main/java/p/Api.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "http://${'$'}{api.host}/x")
    void call() {
    }
}
"""
        )
        myFixture.configureByText("application.properties", "api.host=example.com\n")
        val offset = myFixture.file.text.indexOf("api.host")

        val targets = SpringPropertyGotoDeclarationHandler().getGotoDeclarationTargets(
            myFixture.file.findElementAt(offset),
            offset,
            myFixture.editor
        )

        assertNotNull("配置键应能跳转到注解用法", targets)
        val declaration = targets!!.single() as SpelDeclaration
        assertEquals("注解属性", declaration.kindLabel)
        assertEquals("api.host", declaration.name)
        assertEquals(api.virtualFile, declaration.target?.containingFile?.virtualFile)
        assertTrue(
            "跳转目标应是注解中的占位符: ${declaration.target?.text}",
            declaration.target?.text?.contains("\${api.host}") == true
        )
    }

    /** 值位置不属于键，不提供本插件的跳转目标。 */
    fun testPropertyValueDoesNotNavigate() {
        myFixture.addFileToProject(
            "src/main/java/p/Api.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "http://${'$'}{api.host}/x")
    void call() {
    }
}
"""
        )
        myFixture.configureByText("application.properties", "api.host=example.com\n")
        val offset = myFixture.file.text.indexOf("example.com")

        val targets = SpringPropertyGotoDeclarationHandler().getGotoDeclarationTargets(
            myFixture.file.findElementAt(offset),
            offset,
            myFixture.editor
        )

        assertNull("值位置不应提供跳转目标", targets)
    }

    fun testYamlNestedKeyNavigatesToAnnotation() {
        val api = myFixture.addFileToProject(
            "src/main/java/p/Api.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "http://localhost:${'$'}{server.port}/x")
    void call() {
    }
}
"""
        )
        myFixture.configureByText("application.yml", "server:\n  port: 8080\n")
        val offset = myFixture.file.text.indexOf("port")
        val keyValue = PsiTreeUtil.findChildrenOfType(myFixture.file, YAMLKeyValue::class.java)
            .first { it.keyText == "port" }

        assertNotNull("应找到 YAML 叶子键", keyValue)
        val targets = SpringYamlPropertyGotoDeclarationHandler().getGotoDeclarationTargets(
            myFixture.file.findElementAt(offset),
            offset,
            myFixture.editor
        )

        assertNotNull("YAML 键应能跳转到注解用法", targets)
        val declaration = targets!!.single() as SpelDeclaration
        assertEquals("server.port", declaration.name)
        assertEquals(api.virtualFile, declaration.target?.containingFile?.virtualFile)
        assertTrue(
            "跳转目标应是注解中的占位符: ${declaration.target?.text}",
            declaration.target?.text?.contains("\${server.port}") == true
        )
    }

    /** 多个使用位置时返回全部目标，由平台弹出选择列表。 */
    fun testMultipleUsagesProduceMultipleTargets() {
        myFixture.addFileToProject(
            "src/main/java/p/Api.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "http://${'$'}{api.host}/a")
    void first() {
    }

    @HttpRequest(url = "http://${'$'}{api.host}/b")
    void second() {
    }
}
"""
        )
        myFixture.configureByText("application.properties", "api.host=example.com\n")
        val offset = myFixture.file.text.indexOf("api.host")

        val targets = SpringPropertyGotoDeclarationHandler().getGotoDeclarationTargets(
            myFixture.file.findElementAt(offset),
            offset,
            myFixture.editor
        )

        assertEquals("两处注解用法都应成为跳转目标", 2, targets?.size)
        assertTrue(
            "目标应指向不同的字面量",
            targets!!.mapNotNull { it as? SpelDeclaration }
                .mapNotNull { (it.target as? PsiLiteralExpression)?.text }
                .distinct()
                .size == 2
        )
    }
}
