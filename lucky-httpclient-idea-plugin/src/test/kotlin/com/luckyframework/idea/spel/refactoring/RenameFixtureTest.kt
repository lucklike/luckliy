package com.luckyframework.idea.spel.refactoring

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 13.1/13.2/13.4：SpEL 变量、参数、函数别名重命名与预览/冲突中止。
 */
class RenameFixtureTest : SymbolFixtureTestCase() {

    fun testRenameImportVariableAcrossFiles() {
        val base = myFixture.addFileToProject(
            "p/BaseApi.java",
            """
package p;

import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(var = "token=#{1}")
public class BaseApi {
}
"""
        )
        val use = myFixture.addFileToProject(
            "p/ApiUse.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

public class ApiUse extends BaseApi {
    @HttpRequest(url = "#{#token}")
    void call() {
    }
}
"""
        )
        configure(EMPTY_HOLDER)

        myFixture.renameElement(literalIn(base, "token=#{1}"), "accessToken")

        assertTrue("声明应更新: ${base.text}", base.text.contains("accessToken=#{1}"))
        assertTrue("跨文件引用应更新: ${use.text}", use.text.contains("#{#accessToken}"))
    }

    fun testRenameParameterUpdatesReferences() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{#userId}")
    void call(String userId) {
    }
}
"""
        )
        val parameter = methodNamed("call").parameterList.parameters.first()

        myFixture.renameElement(parameter, "accountId")

        assertTrue(
            "参数引用应更新: ${myFixture.file.text}",
            myFixture.file.text.contains("#{#accountId}")
        )
    }

    fun testRenameAliasUpdatesReferences() {
        val utils = myFixture.addFileToProject(
            "com/example/HashUtils.java",
            """
package com.example;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;

public class HashUtils {
    @FunctionAlias("sha")
    public static String sha256(String text) {
        return text;
    }
}
"""
        )
        configure(
            """
package p;

import com.example.HashUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(HashUtils.class)
    @HttpRequest(url = "#{#sha('a')}")
    void call() {
    }
}
"""
        )

        myFixture.renameElement(literalIn(utils, "sha"), "sha2")

        assertTrue("别名声明应更新: ${utils.text}", utils.text.contains("@FunctionAlias(\"sha2\")"))
        assertTrue(
            "别名引用应更新: ${myFixture.file.text}",
            myFixture.file.text.contains("#{#sha2('a')}")
        )
    }

    fun testRenameImportedMethodUpdatesReferences() {
        val utils = myFixture.addFileToProject(
            "com/example/HashUtils.java",
            """
package com.example;

public class HashUtils {
    public static String md5(String text) {
        return text;
    }
}
"""
        )
        configure(
            """
package p;

import com.example.HashUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(HashUtils.class)
    @HttpRequest(url = "#{#md5('a')}")
    void call() {
    }
}
"""
        )
        val method = methodsIn(utils).first { it.name == "md5" }

        myFixture.renameElement(method, "md5x")

        assertTrue("方法引用应更新: ${myFixture.file.text}", myFixture.file.text.contains("#{#md5x('a')}"))
    }

    fun testRenameAliasedMethodKeepsAliasReference() {
        val utils = myFixture.addFileToProject(
            "com/example/HashUtils.java",
            """
package com.example;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;

public class HashUtils {
    @FunctionAlias("sha")
    public static String sha256(String text) {
        return text;
    }
}
"""
        )
        configure(
            """
package p;

import com.example.HashUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(HashUtils.class)
    @HttpRequest(url = "#{#sha('a')}")
    void call() {
    }
}
"""
        )
        val method = methodsIn(utils).first { it.name == "sha256" }

        myFixture.renameElement(method, "sha512")

        assertTrue("方法名应更新: ${utils.text}", utils.text.contains("sha512"))
        assertTrue(
            "有别名时引用保持别名: ${myFixture.file.text}",
            myFixture.file.text.contains("#{#sha('a')}")
        )
    }

    fun testRenamePreviewListsAffectedFiles() {
        val base = myFixture.addFileToProject(
            "p/BaseApi.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(var = "token=#{1}")
public class BaseApi {
    @HttpRequest(url = "#{#token}")
    void base() {
    }
}
"""
        )
        myFixture.addFileToProject(
            "p/ApiUse.java",
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

public class ApiUse extends BaseApi {
    @HttpRequest(url = "#{#token}")
    void call() {
    }
}
"""
        )
        configure(EMPTY_HOLDER)
        val literal = literalIn(base, "token=#{1}")

        val target = RenamePsiElementProcessor.forElement(literal)
            .substituteElementToRename(literal, myFixture.editor)
        assertNotNull("应包装为可重命名声明", target)
        val usages = RenameProcessor(project, target!!, "accessToken", false, false).findUsages()
        val files = usages.mapNotNull { it.element?.containingFile?.name }.toSet()

        assertTrue("预览应包含声明文件: $files", files.contains("BaseApi.java"))
        assertTrue("预览应包含引用文件: $files", files.contains("ApiUse.java"))
    }

    fun testRenameConflictAbortsWithoutChanges() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(var = {"to<caret>ken=#{1}", "accessToken=#{2}"})
    @HttpRequest(url = "#{#token}")
    void call() {
    }
}
"""
        )
        val before = myFixture.file.text

        // 冲突时平台通过 findExistingNameConflicts 中止，不写入任何内容
        try {
            myFixture.renameElement(literalIn(myFixture.file, "token=#{1}"), "accessToken")
        } catch (expected: BaseRefactoringProcessor.ConflictsInTestsException) {
            // 预期：测试模式下冲突以异常形式中止
        }

        assertEquals("冲突时源文件不应改变", before, myFixture.file.text)
    }

    private fun literalIn(file: PsiFile, value: String): PsiLiteralExpression =
        PsiTreeUtil.findChildrenOfType(file, PsiLiteralExpression::class.java)
            .first { it.value == value }

    private fun methodsIn(file: PsiFile): List<PsiMethod> =
        PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java).toList()

    companion object {
        private val EMPTY_HOLDER = """
package p;

class Holder {
}
"""
    }
}
