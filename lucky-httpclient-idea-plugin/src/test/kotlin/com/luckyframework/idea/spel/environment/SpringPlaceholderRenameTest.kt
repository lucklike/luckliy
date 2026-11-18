package com.luckyframework.idea.spel.environment

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.properties.psi.Property
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.reference.SpelReference
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * 配置键重命名时同步 Java 注解中的 `${key}`。
 */
class SpringPlaceholderRenameTest : SpringEnvironmentFixtureTestCase() {

    fun testRenamePropertyKeyUpdatesAnnotation() {
        val config = myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\n"
        )
        configureHttpRequest("http://\${api.host}/x")

        val property = PsiTreeUtil.findChildrenOfType(config, Property::class.java).single()
        myFixture.renameElement(property, "api.newHost")

        val text = myFixture.file.text
        assertTrue("配置键应更新: ${config.text}", config.text.contains("api.newHost=example.com"))
        assertTrue("注解中的占位符应随配置键重命名: $text", text.contains("\${api.newHost}"))
        assertFalse("旧键名应被替换: $text", text.contains("\${api.host}"))
    }

    /** 重命名后引用必须解析到新键，不能停留在重命名前的配置快照。 */
    fun testReferenceResolvesAfterPropertyRename() {
        val config = myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\n"
        )
        configureHttpRequest("http://\${api.host}/x")
        val property = PsiTreeUtil.findChildrenOfType(config, Property::class.java).single()
        myFixture.renameElement(property, "api.newHost")

        val literal = PsiTreeUtil.findChildOfType(myFixture.file, PsiLiteralExpression::class.java)!!
        val reference = literal.references.filterIsInstance<SpelReference>().single()
        val targets = reference.multiResolve(false)

        assertTrue(
            "重命名后引用应能跳转到配置: ${literal.text}",
            targets.isNotEmpty()
        )
        val model = SpringEnvironmentService.getInstance(project).modelFor(literal)
        assertNotNull("重命名后的键应出现在配置快照中", model?.resolve("api.newHost"))
    }

    /** 重命名后注解不应再报「未定义的配置键」。 */
    fun testNoUndefinedWarningAfterPropertyRename() {
        val config = myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\n"
        )
        configureHttpRequest("http://\${api.host}/x")
        val property = PsiTreeUtil.findChildrenOfType(config, Property::class.java).single()
        myFixture.renameElement(property, "api.newHost")

        val warnings = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.WARNING }
        assertFalse(
            "重命名后不应再出现未定义警告: ${warnings.map { it.description }}",
            warnings.any { it.description?.contains("未定义的配置键") == true }
        )
    }

    /** YAML 叶子键重命名参数只有叶子名，引用要保留父路径。 */
    fun testRenameYamlLeafKeyUpdatesAnnotation() {
        val config = myFixture.addFileToProject(
            "src/main/resources/application.yml",
            "server:\n  port: 8080\n"
        )
        configureHttpRequest("http://localhost:\${server.port}/x")

        val port = PsiTreeUtil.findChildrenOfType(config, YAMLKeyValue::class.java)
            .first { it.keyText == "port" }
        myFixture.renameElement(port, "http-port")

        val text = myFixture.file.text
        assertTrue("YAML 叶子键应更新: ${config.text}", config.text.contains("http-port:"))
        assertTrue("引用应保留父路径: $text", text.contains("\${server.http-port}"))
        assertFalse("旧键名应被替换: $text", text.contains("\${server.port}"))
    }

    /** YAML 重命名后引用同样要解析到新的完整键。 */
    fun testReferenceResolvesAfterYamlRename() {
        val config = myFixture.addFileToProject(
            "src/main/resources/application.yml",
            "server:\n  port: 8080\n"
        )
        configureHttpRequest("http://localhost:\${server.port}/x")
        val port = PsiTreeUtil.findChildrenOfType(config, YAMLKeyValue::class.java)
            .first { it.keyText == "port" }
        myFixture.renameElement(port, "http-port")

        val literal = PsiTreeUtil.findChildOfType(myFixture.file, PsiLiteralExpression::class.java)!!
        val reference = literal.references.filterIsInstance<SpelReference>().single()
        assertTrue(
            "YAML 重命名后引用应能跳转到配置: ${literal.text}",
            reference.multiResolve(false).isNotEmpty()
        )
        val model = SpringEnvironmentService.getInstance(project).modelFor(literal)
        assertNotNull("重命名后的键应出现在配置快照中", model?.resolve("server.http-port"))
    }
}
