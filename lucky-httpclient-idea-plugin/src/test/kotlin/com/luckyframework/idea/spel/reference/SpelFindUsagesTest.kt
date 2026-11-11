package com.luckyframework.idea.spel.reference

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 10.7：Find Usages 跨文件收集 `#{}` 引用并按文件分组。
 */
class SpelFindUsagesTest : SymbolFixtureTestCase() {

    fun testParameterUsagesAreGroupedByFile() {
        val file = myFixture.addFileToProject(
            "Api.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#userId} #{#userId} #{#userId}")
    void call(String userId) {
    }
}
"""
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        val literal = literalIn(myFixture.file, "#{#userId}")
        val declaration = declarationOf(literal, "#userId")

        val usages = ReferencesSearch.search(declaration).findAll()
        assertEquals(3, usages.size)
        val grouped = usages.groupBy { it.element.containingFile.name }
        assertEquals(setOf("Api.java"), grouped.keys)
    }

    fun testImportVariableUsagesAcrossFiles() {
        val base = myFixture.addFileToProject(
            "p/BaseApi.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(var = "token=#{1}")
class BaseApi {
    @HttpRequest(url = "#{#token}")
    void base() {
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/SubOne.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class SubOne extends BaseApi {
    @HttpRequest(url = "#{#token}")
    void one() {
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/SubTwo.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class SubTwo extends BaseApi {
    @HttpRequest(url = "#{#token}")
    void two() {
    }
}
"""
        )
        val declaration = declarationOf(literalIn(base, "#{#token}"), "#token")

        val usages = ReferencesSearch.search(declaration).findAll()
        assertEquals(3, usages.size)
        val grouped = usages.groupBy { it.element.containingFile.name }
        assertEquals(setOf("BaseApi.java", "SubOne.java", "SubTwo.java"), grouped.keys)
    }

    fun testFunctionUsagesAcrossFiles() {
        val base = myFixture.addFileToProject(
            "p/BaseApi.java",
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(MyUtils.class)
class BaseApi {
    @HttpRequest(url = "#{#crypto.md5('a')}")
    void base() {
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/SubOne.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class SubOne extends BaseApi {
    @HttpRequest(url = "#{#crypto.md5('b')}")
    void one() {
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/SubTwo.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class SubTwo extends BaseApi {
    @HttpRequest(url = "#{#crypto.md5('c')}")
    void two() {
    }
}
"""
        )
        val declaration = declarationOf(literalIn(base, "#{#crypto.md5"), "md5")

        val usages = ReferencesSearch.search(declaration).findAll()
        assertEquals(3, usages.size)
        val grouped = usages.groupBy { it.element.containingFile.name }
        assertEquals(setOf("BaseApi.java", "SubOne.java", "SubTwo.java"), grouped.keys)
    }

    private fun literalIn(file: PsiFile, marker: String): PsiLiteralExpression =
        PsiTreeUtil.findChildrenOfType(file, PsiLiteralExpression::class.java)
            .first { it.text.contains(marker) }

    private fun declarationOf(literal: PsiLiteralExpression, text: String): SpelDeclaration {
        val reference = literal.references.first { reference ->
            literal.text.substring(reference.rangeInElement.startOffset, reference.rangeInElement.endOffset) == text
        }
        val resolved = reference.resolve()
        assertNotNull("引用未解析：$text", resolved)
        return resolved as SpelDeclaration
    }
}
