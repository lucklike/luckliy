package com.luckyframework.idea.spel.yaml

import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.LuckyFixtureTestCase
import com.luckyframework.idea.spel.reference.SpelDeclaration
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

class SpelYamlReferenceTest : LuckyFixtureTestCase() {

    fun testImportVariableResolvesToYamlKey() {
        myFixture.configureByText(
            "config.yml",
            """
            springElImport:
              varLit:
                token: "abc"
            url: "http://#{#token}/api"
            """.trimIndent()
        )

        val scalar = expressionScalar("#token")
        val reference = scalar.references.first {
            scalar.text.substring(it.rangeInElement.startOffset, it.rangeInElement.endOffset) == "#token"
        }
        val declaration = reference.resolve() as SpelDeclaration

        assertEquals("@SpELImport 变量", declaration.kindLabel)
        assertEquals("token", declaration.name)
        val keyValue = keyValueNamed("token")
        assertSame(keyValue, declaration.target)
        assertEquals(keyValue.key!!.textRange.startOffset, declaration.textOffset)
    }

    fun testRootVariableResolvesToYamlKey() {
        myFixture.configureByText(
            "config.yml",
            """
            springElImport:
              rootLit:
                user: "admin"
            url: "http://#{user}/api"
            """.trimIndent()
        )

        val scalar = expressionScalar("user")
        val reference = scalar.references.first {
            scalar.text.substring(it.rangeInElement.startOffset, it.rangeInElement.endOffset) == "user"
        }
        val declaration = reference.resolve() as SpelDeclaration

        assertEquals("Root 变量 @SpELImport", declaration.kindLabel)
        assertEquals("user", declaration.name)
        assertSame(keyValueNamed("user"), declaration.target)
    }

    fun testFunctionClassResolvesToStaticMethod() {
        myFixture.addFileToProject(
            "com/example/YamlUtils.java",
            """
            package com.example;

            public class YamlUtils {
                public static String yamlJoin(String left, String right) {
                    return left + right;
                }
            }
            """.trimIndent()
        )
        myFixture.configureByText(
            "config.yml",
            """
            springElImport:
              classes:
                - com.example.YamlUtils
            url: "http://#{#yamlJoin('a', 'b')}/api"
            """.trimIndent()
        )

        val scalar = expressionScalar("#yamlJoin")
        val reference = scalar.references.first {
            scalar.text.substring(it.rangeInElement.startOffset, it.rangeInElement.endOffset) == "#yamlJoin"
        }
        val declaration = reference.resolve() as SpelDeclaration

        assertEquals("函数", declaration.kindLabel)
        assertEquals("yamlJoin", (declaration.target as com.intellij.psi.PsiMethod).name)
    }

    private fun expressionScalar(marker: String): YAMLScalar =
        PsiTreeUtil.findChildrenOfType(myFixture.file, YAMLScalar::class.java)
            .first { SpelYamlSupport.isExpressionValue(it) && it.text.contains(marker) }

    private fun keyValueNamed(name: String): YAMLKeyValue =
        PsiTreeUtil.findChildrenOfType(myFixture.file, YAMLKeyValue::class.java)
            .first { it.keyText == name }
}
