package com.luckyframework.idea.spel.component

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.EditorFactory
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.UIUtil
import com.luckyframework.idea.spel.LuckyFixtureTestCase
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import java.lang.reflect.Field

/**
 * 真实补全链路验证。
 *
 * 此前的用例只构造补全元素再调用定制器，不能证明真实补全时定制器被装到了
 * 渲染器上；而渲染钩子的安装（反射调用内部 API、动态代理）正是最容易失效的
 * 环节。这里触发一次真实类名补全，从真实 lookup 的渲染器里取出定制器，按
 * 渲染器的调用方式（已冻结的 presentation）调用，断言组件候选拿到品牌图标、
 * 普通类候选保持原样。
 */
class LuckyComponentRealCompletionTest : LuckyFixtureTestCase() {

    fun testRealClassCompletionInstallsPresentationCustomizer() {
        myFixture.addFileToProject(
            "com/example/StdBingApi.java",
            """
            package com.example;

            import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;

            @LuckyComponent
            public interface StdBingApi {
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/PlainBingApi.java",
            """
            package com.example;

            public interface PlainBingApi {
            }
            """.trimIndent()
        )
        SpelRuntimeSymbols.components(project)

        val listener = LuckyComponentLookupListener(project)
        project.messageBus.connect(testRootDisposable).subscribe(LookupManagerListener.TOPIC, listener)
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(listener, testRootDisposable)

        myFixture.configureByText(
            "Use.java",
            """
            package com.example;

            class Use {
                void m() {
                    Bing<caret>
                }
            }
            """.trimIndent()
        )
        myFixture.completeBasic()
        UIUtil.dispatchAllInvocationEvents()

        val lookup = LookupManager.getActiveLookup(myFixture.editor)
        assertNotNull("lookup should be shown", lookup)
        val customizers = presentationCustomizers(lookupCellRenderer(lookup!!))
        assertFalse("渲染钩子应装到真实 lookup 的渲染器上", customizers.isEmpty())

        val componentItem = lookup.items.firstOrNull { it.lookupString == "StdBingApi" }
        assertNotNull("StdBingApi should be in completion list", componentItem)
        val plainItem = lookup.items.firstOrNull { it.lookupString == "PlainBingApi" }
        assertNotNull("PlainBingApi should be in completion list", plainItem)

        // 组件候选：按 2026.3 的真实输入（已冻结）调用，必须得到品牌图标
        val componentSource = platformPresentation(componentItem!!)
        componentSource.freeze()
        val branded = customizers
            .map { customize(it, componentItem, componentSource) }
            .filter { it !== componentSource }
        assertTrue("组件候选应被改写展示数据", branded.any { it.icon === LuckyIcons.Component })
        assertSame("原展示数据保持平台图标", AllIcons.Nodes.Class, componentSource.icon)

        // 普通类候选：原样返回，保持平台图标
        val plainSource = platformPresentation(plainItem!!)
        assertTrue(
            "普通类不应被改写",
            customizers.all { customize(it, plainItem, plainSource) === plainSource }
        )

        // 空图标回调必须原样返回：渲染器会直接拿它当列表缩进基准
        val emptyIcon = EmptyIcon.create(16)
        assertTrue(
            "空图标回调应原样返回",
            customizers.all { customizeEmptyIcon(it, emptyIcon) === emptyIcon }
        )
    }

    /** 模拟渲染器交给定制器的输入：平台默认图标。 */
    private fun platformPresentation(item: LookupElement): LookupElementPresentation =
        LookupElementPresentation().apply {
            itemText = item.lookupString
            icon = AllIcons.Nodes.Class
        }

    private fun customize(
        customizer: Any,
        item: LookupElement,
        presentation: LookupElementPresentation
    ): LookupElementPresentation {
        val method = customizer.javaClass.methods.first { it.name == "customizePresentation" }
        return method.invoke(customizer, item, presentation) as LookupElementPresentation
    }

    private fun customizeEmptyIcon(customizer: Any, icon: EmptyIcon): Any? {
        val method = customizer.javaClass.methods.first { it.name == "customizeEmptyIcon" }
        return method.invoke(customizer, icon)
    }

    private fun lookupCellRenderer(lookup: Lookup): Any {
        val field = allFields(lookup.javaClass).firstOrNull {
            it.type.name.endsWith("LookupCellRenderer")
        } ?: throw AssertionError("lookup 应持有 LookupCellRenderer")
        field.isAccessible = true
        return field.get(lookup) ?: throw AssertionError("LookupCellRenderer 尚未创建")
    }

    private fun presentationCustomizers(renderer: Any): List<Any> {
        val field = allFields(renderer.javaClass).firstOrNull {
            List::class.java.isAssignableFrom(it.type) &&
                it.genericType.typeName.contains("ItemPresentationCustomizer")
        } ?: throw AssertionError("渲染器应暴露展示定制器列表")
        field.isAccessible = true
        return (field.get(renderer) as? List<*>)?.filterNotNull().orEmpty()
    }

    private fun allFields(type: Class<*>): List<Field> {
        val fields = ArrayList<Field>()
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            fields += current.declaredFields
            current = current.superclass
        }
        return fields
    }
}
