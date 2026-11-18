package com.luckyframework.idea.spel.environment

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.testFramework.PlatformTestUtil

/**
 * Java 注解属性中 `${key}` 的配置键补全。
 */
class SpringPlaceholderCompletionTest : SpringEnvironmentFixtureTestCase() {

    fun testCompletesDottedPropertyKeys() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\napi.port=8080\nother.key=ignored\n"
        )
        configureHttpRequest("http://\${api.}/x")
        moveCaretAfter("api.")

        myFixture.completeBasic()
        val lookup = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()

        assertTrue("缺少 api.host: $lookup", lookup.contains("api.host"))
        assertTrue("缺少 api.port: $lookup", lookup.contains("api.port"))
        assertFalse("不应出现无关键: $lookup", lookup.contains("other.key"))
    }

    /**
     * 候选展示为：属性图标 + `key=value` + 右对齐的所在文件名。
     * 与用户要求的图 1 一致，同一键在多个 profile 文件中定义时逐条展示。
     */
    fun testLookupShowsValueAndSourceFile() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\napi.port=8080\n"
        )
        configureHttpRequest("http://\${api.}/x")
        moveCaretAfter("api.")
        myFixture.completeBasic()

        val element = myFixture.lookupElements?.firstOrNull { it.lookupString == "api.host" }
            ?: error("未提供 api.host 候选")
        val presentation = LookupElementPresentation.renderElement(element)

        assertEquals("api.host", presentation.itemText)
        assertEquals("=example.com", presentation.tailText)
        assertEquals("application.properties", presentation.typeText)
        assertNotNull("候选缺少属性图标", presentation.icon)
    }

    /** 同一键在多个文件里定义时，每个文件各给一条候选。 */
    fun testMultipleDefinitionsProduceCandidatePerFile() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=default-host\n"
        )
        myFixture.addFileToProject(
            "src/main/resources/application-local.properties",
            "api.host=local-host\n"
        )
        configureHttpRequest("http://\${api.}/x")
        moveCaretAfter("api.")
        myFixture.completeBasic()

        val typeTexts = myFixture.lookupElements
            ?.filter { it.lookupString == "api.host" }
            ?.map { LookupElementPresentation.renderElement(it).typeText }
            ?: emptyList()

        assertTrue("缺少默认配置候选: $typeTexts", typeTexts.contains("application.properties"))
        assertTrue("缺少 local 配置候选: $typeTexts", typeTexts.contains("application-local.properties"))
    }

    /** 选择带 `.` 的键后必须替换整个键名，而不是在已有前缀后重复插入。 */
    fun testInsertingDottedKeyReplacesWholeKey() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\napi.port=8080\n"
        )
        configureHttpRequest("http://\${api.}/x")
        moveCaretAfter("api.")
        myFixture.completeBasic()

        val first = myFixture.lookupElements?.firstOrNull { it.lookupString == "api.host" }
            ?: error("未提供 api.host 候选")
        myFixture.lookup.currentItem = first
        myFixture.finishLookup('\n')

        val text = myFixture.file.text
        assertTrue("未插入完整键名: $text", text.contains("\${api.host}"))
        assertFalse("键名前缀重复: $text", text.contains("api.api."))
    }

    /** 刚输入 `${` 时键名为空，也必须给出全部候选。 */
    fun testCompletesKeysAtEmptyKey() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\napi.port=8080\n"
        )
        configureHttpRequest("http://\${}/x")
        moveCaretAfter("\${")

        myFixture.completeBasic()
        val lookup = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()

        assertTrue("空键名时缺少候选: $lookup", lookup.contains("api.host"))
        assertTrue("空键名时缺少候选: $lookup", lookup.contains("api.port"))
    }

    /** 键入键名字符时自动弹出补全，不需要手动 Ctrl+Space。 */
    fun testAutoPopupWhileTypingKey() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\napi.port=8080\n"
        )
        configureHttpRequest("http://\${}/x")
        moveCaretAfter("\${")
    
        myFixture.type("a")
        val listed = awaitKey("api.host")
    
        assertNotNull("输入键名时未自动弹出补全", myFixture.lookup)
        assertTrue(
            "输入键名时自动弹出的候选不正确: ${'$'}{myFixture.lookupElements?.map { it.lookupString }}",
            listed
        )
    }
    
    /** typedHandler 触发的补全在后台计算候选，轮询等待列表项回投。 */
    private fun awaitKey(key: String): Boolean {
        val deadline = System.currentTimeMillis() + 3000
        while (System.currentTimeMillis() < deadline) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            if (myFixture.lookupElements?.any { it.lookupString == key } == true) {
                return true
            }
            Thread.sleep(10)
        }
        return false
    }

    /** 默认值位置不属于键名，不弹出环境变量键补全。 */
    fun testNoKeyCompletionInDefaultValue() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\n"
        )
        configureHttpRequest("http://\${api.host:}")
        moveCaretAfter("api.host:")

        myFixture.completeBasic()
        val lookup = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()

        assertFalse("默认值位置不应出现键补全: $lookup", lookup.contains("api.host"))
    }

    private fun moveCaretAfter(marker: String) {
        val offset = myFixture.file.text.indexOf(marker)
        assertTrue("未找到标记: $marker", offset >= 0)
        myFixture.editor.caretModel.moveToOffset(offset + marker.length)
    }
}
