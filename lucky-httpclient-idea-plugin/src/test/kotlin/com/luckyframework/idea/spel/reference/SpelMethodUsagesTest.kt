package com.luckyframework.idea.spel.reference

import com.intellij.codeInspection.deadCode.UnusedDeclarationInspection
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * SpEL 引用参与平台的 Find Usages 与未使用判定（10.7/13.2）。
 *
 * 平台对方法的 Find Usages/unused 判定走 MethodReferencesSearch，不会执行
 * `referencesSearch` 扩展点；这些用例保证只注册后者时「已引用仍灰化」的问题
 * 不会回归。
 */
class SpelMethodUsagesTest : SymbolFixtureTestCase() {

    fun testMethodReferencesSearchFindsFunctionNameLiteral() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(func = "getUrl")
    void call() {
    }

    public static String getUrl() {
        return "";
    }
}
"""
        )
        val method = methodNamed("getUrl")
        val scope = GlobalSearchScope.projectScope(project)

        for (strict in listOf(false, true)) {
            val usages = MethodReferencesSearch.search(method, scope, strict).findAll()
            assertTrue(
                "strict=$strict 时应找到函数名属性里的用法: ${usages.map { it.element.text }}",
                usages.any {
                    it.element is PsiLiteralExpression && it.element.text.contains("getUrl")
                }
            )
        }
    }

    fun testUnusedHighlightSparesMethodReferencedByFunctionName() {
        // 编辑器里的 unused 灰化由该 inspection 创建的同名本地工具提供
        myFixture.enableInspections(UnusedDeclarationInspection())
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(func = "getUrl")
    void call() {
    }

    public static String getUrl() {
        return "";
    }

    private static String orphan() {
        return "";
    }
}
"""
        )
        val unused = unusedMethodFlagged()

        assertTrue("对照：未被引用的私有方法应被标记未使用", unused(methodNamed("orphan")))
        assertFalse("被 func 引用的方法不应被标记未使用", unused(methodNamed("getUrl")))
    }

    fun testFieldReferencesSearchFindsMemberAccess() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Account {
    String name;
}

class Api {
    @HttpRequest(url = "#{account.name}")
    void call(Account account) {
    }
}
"""
        )
        val field = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiField::class.java)
            .first { it.name == "name" }

        val usages = ReferencesSearch.search(field).findAll()

        assertTrue(
            "字段的 SpEL 成员访问应参与查找用法: ${usages.map { it.element.text }}",
            usages.any { it.element.text.contains("account.name") }
        )
    }

    fun testTypeReferencesSearchFindsTypeLiteral() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Payload {
}

class Api {
    @HttpRequest(url = "#{T(p.Payload)}")
    void call() {
    }
}
"""
        )
        val type = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiClass::class.java)
            .first { it.name == "Payload" }

        val usages = ReferencesSearch.search(type).findAll()

        assertTrue(
            "T(Type) 应参与查找用法: ${usages.map { it.element.text }}",
            usages.any { it.element.text.contains("T(p.Payload)") }
        )
    }

    fun testBeanReferenceIsNotClassReference() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{@myUtils.md5('a')}")
    void call() {
    }
}
"""
        )
        val beanClass = JavaPsiFacade.getInstance(project)
            .findClass("com.example.MyUtils", GlobalSearchScope.allScope(project))
        assertNotNull("夹具应包含 com.example.MyUtils", beanClass)
        val literal = literalOf("#{@myUtils.md5('a')}")
        val beanReference = literal.references.first {
            literal.text.substring(it.rangeInElement.startOffset, it.rangeInElement.endOffset) == "@myUtils"
        }

        assertFalse(
            "Bean 名不随类名变化，不能算作对类的引用",
            beanReference.isReferenceTo(beanClass!!)
        )
    }

    /** unused 提示按方法名范围匹配，避免依赖具体提示文案中的方法签名。 */
    private fun unusedMethodFlagged(): (PsiMethod) -> Boolean {
        val highlights = myFixture.doHighlighting().filter {
            it.description?.contains("never used", ignoreCase = true) == true
        }
        return { method ->
            val name = method.nameIdentifier?.textRange
            name != null && highlights.any {
                it.startOffset >= name.startOffset && it.endOffset <= name.endOffset
            }
        }
    }
}
