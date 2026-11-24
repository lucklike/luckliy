package com.luckyframework.idea.spel.reference

import com.intellij.psi.PsiField
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 回归：引用搜索命中字面量时不能读取全量的 `literal.references`。
 *
 * IntelliLang 会为 `@Language` 变量的拼接使用处计算注入语言，计算过程中
 * 再次对变量发起 [ReferencesSearch]；搜索器若在搜索回调里读取
 * `literal.references`，会重入自身并无限递归（用户文件
 * SerializerMessages.java 的 StackOverflowError）。
 *
 * 测试运行时默认不加载 IntelliLang 插件，此时至少验证搜索器直接使用
 * SpEL 提供器仍能收集到字段引用；加载了该插件的环境会实际走到注入链。
 */
class SpelReferenceSearchRecursionTest : SymbolFixtureTestCase() {

    fun testLanguageInjectionDoesNotRecurseIntoSpelSearch() {
        myFixture.addFileToProject(
            "org/intellij/lang/annotations/Language.java",
            """
package org.intellij.lang.annotations;

public @interface Language {
    String value() default "";
    String prefix() default "";
    String suffix() default "";
}
"""
        )
        val file = myFixture.addFileToProject(
            "p/Messages.java",
            """
package p;

import org.intellij.lang.annotations.Language;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Messages {
    @Language("JAVA")
    static final String MESSAGE = "class A {}";

    static String compose() {
        return MESSAGE + "";
    }

    @HttpRequest(url = "#{MESSAGE}")
    void call() {
    }
}
"""
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        val field = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiField::class.java)
            .first { it.name == "MESSAGE" }

        val usages = ReferencesSearch.search(field).findAll()

        assertTrue("未命中 SpEL 中的字段引用", usages.isNotEmpty())
    }
}
