package com.luckyframework.idea.spel.refactoring

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.extensions.Extensions
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.rename.RenameHandler
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
    @HttpRequest(url = "#{userId}")
    void call(String userId) {
    }
}
"""
        )
        val parameter = methodNamed("call").parameterList.parameters.first()

        myFixture.renameElement(parameter, "accountId")

        assertTrue(
            "参数引用应更新: ${myFixture.file.text}",
            myFixture.file.text.contains("#{accountId}")
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

    /** @FunctionAlias 别名值内 Rename 可用；引号与方法名处不受影响。 */
    fun testAliasRenameHandlerAvailableInAliasValue() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;

class Api {
    @FunctionAlias("sha")
    public static String sha256(String text) {
        return text;
    }
}
"""
        )
        val literal = literalOf("sha")
        val handler = SpelAliasRenameHandler()

        assertTrue(
            "别名值内应可用",
            handler.isAvailableOnDataContext(
                contextWithCaretAt(literal.textRange.startOffset + 1)
            )
        )
        assertFalse(
            "引号位置不在名字片段内",
            handler.isAvailableOnDataContext(contextWithCaretAt(literal.textRange.startOffset))
        )
        assertFalse(
            "方法名处不是注解字符串内的名字",
            handler.isAvailableOnDataContext(
                contextWithCaretAt(myFixture.file.text.indexOf("sha256"))
            )
        )
    }

    /** @SpELImport 变量名内 Rename 可用；等号后的表达式部分不可用。 */
    fun testAliasRenameHandlerAvailableInImportVariableName() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(var = "token=#{1}")
class Api {
}
"""
        )
        val literal = literalOf("token=#{1}")
        val handler = SpelAliasRenameHandler()
        val valueStart = literal.textRange.startOffset + 1

        assertTrue(
            "变量名内应可用",
            handler.isAvailableOnDataContext(contextWithCaretAt(valueStart))
        )
        assertFalse(
            "表达式部分不属于名字片段",
            handler.isAvailableOnDataContext(contextWithCaretAt(valueStart + 7))
        )
    }

    /** plugin.xml 必须注册 renameHandler，否则 Rename 动作在字符串内仍为灰色。 */
    fun testAliasRenameHandlerRegisteredInPluginXml() {
        val handlers = Extensions.getExtensions(RenameHandler.EP_NAME).toList()

        assertTrue(
            "plugin.xml 未注册 SpelAliasRenameHandler: $handlers",
            handlers.any { it is SpelAliasRenameHandler }
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

    fun testRenameMethodUpdatesFunctionNameValue() {
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

        myFixture.renameElement(methodNamed("getUrl"), "fetchUrl")

        assertTrue(
            "函数名属性应随方法重命名: ${myFixture.file.text}",
            myFixture.file.text.contains("func = \"fetchUrl\"")
        )
    }

    fun testRenameFieldUpdatesPropertyReference() {
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

        myFixture.renameElement(field, "nickname")

        assertTrue(
            "SpEL 属性访问应随字段重命名: ${myFixture.file.text}",
            myFixture.file.text.contains("#{account.nickname}")
        )
    }

    fun testRenameTypeUpdatesTypeReference() {
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

        myFixture.renameElement(type, "Payload2")

        assertTrue(
            "T(Type) 应随类重命名并保留包名: ${myFixture.file.text}",
            myFixture.file.text.contains("T(p.Payload2)")
        )
    }

    /** 构造含编辑器与文件的 DataContext，并把光标移到指定偏移。 */
    private fun contextWithCaretAt(offset: Int): DataContext {
        myFixture.editor.caretModel.moveToOffset(offset)
        return SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.EDITOR, myFixture.editor)
            .add(CommonDataKeys.PSI_FILE, myFixture.file)
            .build()
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
