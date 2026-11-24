package com.luckyframework.idea.navigator

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiMethod

/**
 * 编辑器 gutter 调试图标与 Lucky 工具窗口之间的入口。
 *
 * 工具窗口内容是懒创建的：从编辑器点击图标时面板可能还不存在。这里先把
 * 目标方法挂起，等 [LuckyNavigatorPanel] 创建后再打开对应方法的调试页；
 * 面板已存在时直接转交。所有方法都应在 EDT 上调用。
 */
@Service(Service.Level.PROJECT)
class LuckyDebugOpener(private val project: Project) {

    /** 当前存活的导航面板；工具窗口内容销毁后置空。 */
    private var panel: LuckyNavigatorPanel? = null

    /** 面板尚未创建时挂起的调试目标。 */
    private var pendingMethod: PsiMethod? = null

    fun attach(panel: LuckyNavigatorPanel) {
        this.panel = panel
        val method = pendingMethod ?: return
        pendingMethod = null
        panel.openDebugMethod(method)
    }

    fun detach(panel: LuckyNavigatorPanel) {
        if (this.panel === panel) {
            this.panel = null
        }
    }

    /**
     * 打开方法对应的调试页。
     *
     * 面板已存在时直接打开并确保工具窗口可见；否则先激活 Lucky 工具窗口，
     * 让平台创建内容，面板构造完成后消费挂起的目标。
     */
    fun open(method: PsiMethod) {
        if (project.isDisposed || !method.isValid) {
            return
        }
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)
        val existing = panel
        if (existing != null) {
            toolWindow?.activate(null, true)
            existing.openDebugMethod(method)
            return
        }
        pendingMethod = method
        if (toolWindow == null) {
            pendingMethod = null
            return
        }
        toolWindow.activate(null, true)
    }

    companion object {
        /** plugin.xml 中 Lucky 工具窗口的 id。 */
        private const val TOOL_WINDOW_ID = "Lucky"

        fun getInstance(project: Project): LuckyDebugOpener = project.service()
    }
}
