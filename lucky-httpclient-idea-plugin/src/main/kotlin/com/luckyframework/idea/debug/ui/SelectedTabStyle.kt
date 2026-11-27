package com.luckyframework.idea.debug.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color

/**
 * 统一选中样式：与 Debug 页 Result/Logs 的选中标签一致。
 *
 * 平台在 2026.3（Islands UI）用 EditorTabs.underlinedTabBackground 填充、
 * EditorTabs.underlinedBorderColor 描边，圆角取 MainToolbar.Button.arc；
 * 调试页的 Args 标题、侧边栏的顶层页签与容器条目都复用这组取值。
 * 旧版本主题没有这些键时退回一组深色/浅色兜底值。
 */
internal object SelectedTabStyle {

    /** 选中块填充色。 */
    val background: Color = JBColor.namedColor(
        "EditorTabs.underlinedTabBackground",
        JBColor(Color(0xE5EEFF), Color(0x343E51))
    )

    /** 选中块 1px 描边色。 */
    val border: Color = JBColor.namedColor(
        "EditorTabs.underlinedBorderColor",
        JBColor(Color(0x7F9FC3), Color(0x7F9FC3))
    )

    /** 选中块圆角半径：跟随平台工具栏按钮圆角（默认 12，compact 模式 8）。 */
    val arc: Int = JBUI.CurrentTheme.MainToolbar.Button.hoverArc().get()
}
