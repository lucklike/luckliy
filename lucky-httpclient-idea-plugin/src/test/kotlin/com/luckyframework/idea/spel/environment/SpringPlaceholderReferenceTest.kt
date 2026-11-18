package com.luckyframework.idea.spel.environment

import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.reference.SpelDeclaration
import com.luckyframework.idea.spel.reference.SpelReference

/**
 * `${key}` 的跳转：键名解析到配置文件中的键定义。
 */
class SpringPlaceholderReferenceTest : SpringEnvironmentFixtureTestCase() {

    fun testKeyResolvesToConfigurationFile() {
        val config = myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\n"
        )
        configureHttpRequest("http://\${api.host}/x")

        val reference = placeholderReference("api.host")
        val declaration = reference.resolve() as SpelDeclaration

        assertEquals("环境变量", declaration.kindLabel)
        assertEquals("api.host", declaration.name)
        val target = declaration.target
        assertNotNull(target)
        assertEquals(config.virtualFile, target!!.containingFile.virtualFile)
        // 跳转目标是配置文件中的键定义所在元素，properties 文件里可能整行作为一个叶子
        assertTrue("未定位到键定义: ${target.text}", target.text.startsWith("api.host"))
    }

    /** 未激活 profile 中的定义同样可以作为跳转目标。 */
    fun testInactiveProfileDefinitionIsNavigable() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=default-host\n"
        )
        myFixture.addFileToProject(
            "src/main/resources/application-prod.properties",
            "api.host=prod-host\n"
        )
        configureHttpRequest("http://\${api.host}/x")

        val declarations = placeholderReference("api.host").multiResolve(false)
            .map { it.element as SpelDeclaration }

        assertEquals(2, declarations.size)
        assertTrue(
            "未激活 profile 应保留跳转目标: ${declarations.map { it.target?.containingFile?.virtualFile?.name }}",
            declarations.any {
                it.target?.containingFile?.virtualFile?.name == "application-prod.properties"
            }
        )
    }

    private fun placeholderReference(key: String): SpelReference {
        val literal = PsiTreeUtil.findChildOfType(myFixture.file, PsiLiteralExpression::class.java)
            ?: error("未找到字符串字面量")
        return literal.references.first {
            literal.text.substring(it.rangeInElement.startOffset, it.rangeInElement.endOffset) == key
        } as SpelReference
    }
}
