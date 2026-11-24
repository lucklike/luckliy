package com.luckyframework.idea.navigator

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.InplaceButton
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.impl.ActionPanel
import com.intellij.ui.tabs.impl.JBTabsImpl
import com.intellij.ui.tabs.impl.TabLabel
import javax.swing.JPanel

/**
 * 调试子标签关闭图标的平台行为验证。
 *
 * 调试标签的关闭按钮走平台 [TabLabel] 的动作面板：只有把动作面板设为
 * 自动隐藏并用 [TabLabel.toggleShowActions] 切换，才能在不改变标签宽度
 * 的前提下隐藏/显示关闭图标。
 */
class TabCloseIconVisibilityTest : BasePlatformTestCase() {

    fun `test reapplying auto hide keeps close icon hidden after action panel rebuild`() {
        val tabs = JBTabsFactory.createTabs(project, testRootDisposable) as JBTabsImpl
        val info = TabInfo(JPanel())
            .setText("get")
            .setTabLabelActions(DefaultActionGroup(CloseAction()), "LuckyTabs")
        tabs.addTab(info)
        tabs.updateTabActions(true)

        val label = tabs.getTabLabel(info)
        // 初始未自动隐藏：关闭图标默认绘制
        assertTrue(closeIconPainting(label))

        // 自动隐藏且未悬停：关闭图标不绘制
        label.setTabActionsAutoHide(true)
        label.toggleShowActions(false)
        assertFalse(closeIconPainting(label))

        // 平台在选中标签或刷新动作时重建动作面板，自动隐藏标记随之丢失
        label.setTabActions(info.getTabLabelActions())
        tabs.updateTabActions(true)
        assertTrue(closeIconPainting(label))

        // 修复方式：重新设置自动隐藏，再按悬停状态切换绘制
        label.setTabActionsAutoHide(true)
        label.toggleShowActions(false)
        assertFalse(closeIconPainting(label))

        // 悬停标签：关闭图标绘制
        label.toggleShowActions(true)
        assertTrue(closeIconPainting(label))
    }

    /** 读取关闭动作按钮当前的绘制状态；动作面板是平台内部结构，只能按字段类型定位。 */
    private fun closeIconPainting(label: TabLabel): Boolean {
        val panel = findFieldValue(label, ActionPanel::class.java) as ActionPanel
        @Suppress("UNCHECKED_CAST")
        val buttons = findFieldValue(panel, List::class.java) as List<Any>
        val button = buttons.firstOrNull() ?: error("关闭动作应创建动作按钮")
        val inplace = button.javaClass.getMethod("getComponent").apply { isAccessible = true }
            .invoke(button) as InplaceButton
        val paintingField = InplaceButton::class.java.declaredFields.first {
            it.type == Boolean::class.javaPrimitiveType && it.name.contains("painting", ignoreCase = true)
        }.apply { isAccessible = true }
        return paintingField.getBoolean(inplace)
    }

    /** 在类层次中按字段类型找值：不同平台版本的动作面板字段名不同。 */
    private fun findFieldValue(instance: Any, type: Class<*>): Any {
        var cls: Class<*>? = instance.javaClass
        while (cls != null) {
            val field = cls.declaredFields.firstOrNull { type.isAssignableFrom(it.type) }
            if (field != null) {
                field.isAccessible = true
                return field.get(instance) ?: error("字段 ${field.name} 不应为空")
            }
            cls = cls.superclass
        }
        error("${instance.javaClass.name} 中找不到 ${type.name} 字段")
    }

    private class CloseAction : AnAction("Close") {
        override fun actionPerformed(event: AnActionEvent) = Unit

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }
}
