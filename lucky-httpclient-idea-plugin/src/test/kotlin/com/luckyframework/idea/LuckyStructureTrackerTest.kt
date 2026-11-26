package com.luckyframework.idea

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 结构计数与签名计数的边界：方法体编辑不能递增，声明变化必须递增，
 * 类型文本变化只递增签名。
 *
 * 项目级全量扫描绑定结构计数，逐字面量的 SpEL 求值上下文绑定签名计数：
 * 结构计数多算会让每次击键都全项目重扫，少算会让声明变更后补全与检查
 * 停留在旧结果；把类型文本算进结构计数会让每次改类型都重启 daemon，
 * 整轮高亮（含平台内置检查）被清空重来。
 */
class LuckyStructureTrackerTest : BasePlatformTestCase() {

    private fun counter(): Long = LuckyStructureTracker.getInstance(project).modificationCount

    private fun signature(): Long =
        LuckyStructureTracker.getInstance(project).signature().modificationCount

    fun testTypingInsideMethodBodyDoesNotBump() {
        myFixture.configureByText(
            "Demo.java",
            """
            class Demo {
                void run() {
                    int a = 1<caret>;
                }
            }
            """.trimIndent()
        )
        val before = counter()

        myFixture.type("2")

        assertEquals("方法体内的字面量编辑不应让项目级快照失效", before, counter())
    }

    fun testTypingStatementInsideMethodBodyDoesNotBump() {
        myFixture.configureByText(
            "Demo.java",
            """
            class Demo {
                void run() {
                    <caret>
                }
            }
            """.trimIndent()
        )
        val before = counter()

        myFixture.type("System.out.println(\"a\");")

        assertEquals("方法体内新增语句不应让项目级快照失效", before, counter())
    }

    fun testAddingMethodBumps() {
        myFixture.configureByText("Demo.java", "class Demo {<caret>}")
        val beforeStructure = counter()
        val beforeSignature = signature()

        myFixture.type("\n    void added() {}\n")

        assertTrue("新增方法属于声明结构变化", counter() > beforeStructure)
        assertTrue("结构变化必须同步递增签名", signature() > beforeSignature)
    }

    fun testChangingAnnotationValueBumps() {
        myFixture.configureByText(
            "Demo.java",
            """
            @SuppressWarnings("<caret>")
            class Demo {}
            """.trimIndent()
        )
        val before = counter()

        myFixture.type("unchecked")

        assertTrue("注解属性值变化属于声明结构变化", counter() > before)
    }

    fun testChangingParameterTypeBumpsSignatureOnly() {
        myFixture.configureByText("Demo.java", "class Demo { void run(String a) {} }")
        val beforeStructure = counter()
        val beforeSignature = signature()

        WriteCommandAction.runWriteCommandAction(project) {
            val method = (myFixture.file as PsiJavaFile).classes.single().methods.single()
            val factory = JavaPsiFacade.getElementFactory(project)
            val type = factory.createTypeByFQClassName("java.lang.Integer")
            method.parameterList.parameters.single().typeElement!!.replace(factory.createTypeElement(type))
        }

        assertEquals("参数类型变化不改变符号表键集合", beforeStructure, counter())
        assertTrue("参数类型变化必须刷新 SpEL 求值上下文", signature() > beforeSignature)
    }

    /**
     * 参数改名：事件容器是 `PsiParameter`，变化节点是名字标识符。
     * 参数名进入逐字面量的求值上下文，但不影响任何全项目快照。
     */
    fun testRenamingParameterBumpsSignatureOnly() {
        myFixture.configureByText("Demo.java", "class Demo { void run(String abc) {} }")
        val beforeStructure = counter()
        val beforeSignature = signature()

        replaceWord("abc", "xyz")

        assertEquals("参数改名不应触发全项目扫描", beforeStructure, counter())
        assertTrue("参数改名必须刷新 SpEL 求值上下文", signature() > beforeSignature)
    }

    fun testAddingFileBumps() {
        myFixture.configureByText("Demo.java", "class Demo {}")
        val before = counter()

        myFixture.addFileToProject("pkg/Other.java", "package pkg;\nclass Other {}")

        assertTrue("新增源码文件属于声明结构变化", counter() > before)
    }

    /**
     * 编辑器里选词替换类型：只替换 `PsiTypeElement` 内部的引用元素，
     * 事件容器是类型元素本身，不会替换整个类型节点。
     */
    fun testReplacingParameterTypeInEditorBumpsSignatureOnly() {
        myFixture.configureByText("Demo.java", "class Demo { void run(String abc) {} }")
        val beforeStructure = counter()
        val beforeSignature = signature()

        replaceWord("String", "Integer")

        assertEquals("编辑器里替换参数类型不应触发全项目扫描", beforeStructure, counter())
        assertTrue("编辑器里替换参数类型必须刷新 SpEL 求值上下文", signature() > beforeSignature)
    }

    fun testReplacingReturnTypeInEditorBumpsSignatureOnly() {
        myFixture.configureByText("Demo.java", "class Demo { String run() { return null; } }")
        val beforeStructure = counter()
        val beforeSignature = signature()

        replaceWord("String", "Integer")

        assertEquals("编辑器里替换返回类型不应触发全项目扫描", beforeStructure, counter())
        assertTrue("编辑器里替换返回类型必须刷新 SpEL 求值上下文", signature() > beforeSignature)
    }

    fun testReplacingLocalVariableTypeInEditorDoesNotBump() {
        myFixture.configureByText(
            "Demo.java",
            """
            class Demo {
                void run() {
                    String value = null;
                }
            }
            """.trimIndent()
        )
        val beforeStructure = counter()
        val beforeSignature = signature()

        replaceWord("String", "Integer")

        assertEquals("方法体内局部变量类型不影响项目级快照", beforeStructure, counter())
        assertEquals("方法体内局部变量类型不影响 SpEL 上下文签名", beforeSignature, signature())
    }

    /** 选中第一次出现的 [from] 并整词输入 [to]，走编辑器的增量重解析路径。 */
    private fun replaceWord(from: String, to: String) {
        val document = myFixture.editor.document
        val start = document.text.indexOf(from)
        myFixture.editor.selectionModel.setSelection(start, start + from.length)
        myFixture.type(to)
    }
}
