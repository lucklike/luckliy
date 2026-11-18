package com.luckyframework.idea.navigator

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.component.ComponentIconResolver
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.ToolTipManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

/**
 * Lucky 侧边导航栏：模块 → 固定分组 → 组件 → 接口方法/函数。
 *
 * 数据由 [LuckyNavigatorModelBuilder] 在后台 read action 中构建，EDT 只做
 * 树渲染；双击组件/方法直接跳转到源码 PSI 位置。PSI 版本变化后重建，
 * 展开状态按节点 key 恢复。
 */
class LuckyNavigatorPanel(
    private val project: Project,
    private val toolWindow: ToolWindow
) : Disposable {

    private val builder = LuckyNavigatorModelBuilder()

    private val root = DefaultMutableTreeNode()

    private val treeModel = DefaultTreeModel(root)

    private val tree = JTree(treeModel)

    /**
     * 轮询定时器挂内部 Disposable：面板构造时还未注册进 Disposer 树，
     * 直接以 `this` 作为 parent 会让面板被隐式挂到 ROOT_DISPOSABLE 并泄漏。
     */
    private val alarmDisposable = Disposer.newDisposable("lucky.navigator.panel")

    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, alarmDisposable)

    @Volatile
    private var refreshing = false

    @Volatile
    private var disposed = false

    /** 上次重建时看到的 PSI 版本号；轮询发现变化后重建。 */
    @Volatile
    private var lastStamp = -1L

    /** Tool Window 内容组件。 */
    val component: JComponent = JBScrollPane(tree)

    init {
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.cellRenderer = NodeRenderer()
        tree.toolTipText = ""
        ToolTipManager.sharedInstance().registerComponent(tree)
        installMouseHandler()
        installKeyboardHandler()
        toolWindow.setTitleActions(listOf(RefreshAction()))
        lastStamp = PsiModificationTracker.getInstance(project).modificationCount
        refresh()
        DumbService.getInstance(project).runWhenSmart {
            if (!disposed) {
                refresh()
            }
        }
        scheduleStampCheck()
    }

    /** 重建整棵树；并发调用只保留一次构建。 */
    fun refresh() {
        if (disposed || refreshing) {
            return
        }
        refreshing = true
        // 已有内容时保留旧树，避免每次自动刷新都闪烁提示。
        // 空树访问 getFirstChild()/getChildAt(0) 都会抛异常，必须先判断数量。
        val first = if (root.childCount > 0) root.getChildAt(0) as? DefaultMutableTreeNode else null
        if (first == null || first.userObject is NodeData.Message) {
            showMessage("正在扫描 Lucky 组件…")
        }
        AppExecutorUtil.getAppExecutorService().execute {
            val model = runCatching {
                ReadAction.compute<LuckyNavigatorModel, RuntimeException> { builder.build(project) }
            }.getOrElse { LuckyNavigatorModel(emptyList()) }
            ApplicationManager.getApplication().invokeLater {
                refreshing = false
                if (disposed || project.isDisposed) {
                    return@invokeLater
                }
                render(model)
            }
        }
    }

    override fun dispose() {
        disposed = true
        alarm.cancelAllRequests()
        Disposer.dispose(alarmDisposable)
    }

    // ---------------------------------------------------------------------------------------------
    // 渲染
    // ---------------------------------------------------------------------------------------------

    private fun render(model: LuckyNavigatorModel) {
        val expanded = collectExpandedKeys()
        root.removeAllChildren()
        if (model.projects.isEmpty()) {
            root.add(DefaultMutableTreeNode(NodeData.Message(emptyMessage())))
        } else {
            for (projectNode in model.projects) {
                val projectTreeNode = DefaultMutableTreeNode(NodeData.Project(projectNode.name))
                for (group in projectNode.groups) {
                    val groupTreeNode = DefaultMutableTreeNode(NodeData.Group(projectNode.name, group))
                    for (component in group.components) {
                        val componentTreeNode =
                            DefaultMutableTreeNode(NodeData.Component(projectNode.name, component))
                        for (item in component.items) {
                            componentTreeNode.add(DefaultMutableTreeNode(NodeData.Item(item)))
                        }
                        groupTreeNode.add(componentTreeNode)
                    }
                    projectTreeNode.add(groupTreeNode)
                }
                root.add(projectTreeNode)
            }
        }
        treeModel.reload()
        restoreExpanded(expanded)
    }

    private fun showMessage(text: String) {
        root.removeAllChildren()
        root.add(DefaultMutableTreeNode(NodeData.Message(text)))
        treeModel.reload()
    }

    private fun emptyMessage(): String = when {
        !LuckyHttpClientSettings.getInstance().isEnabled ->
            "Lucky HttpClient 插件已关闭，请在 Settings | Tools | Lucky HttpClient 中启用"

        DumbService.isDumb(project) -> "索引构建中，稍后自动刷新…"
        else -> "当前项目未发现 Lucky 组件"
    }

    /** 当前树中已展开节点的 key；根与消息节点不记录。 */
    private fun collectExpandedKeys(): Set<String> {
        val result = HashSet<String>()
        for (row in 0 until tree.rowCount) {
            val path = tree.getPathForRow(row) ?: continue
            if (!tree.isExpanded(path)) {
                continue
            }
            keyOf(path.lastPathComponent as? DefaultMutableTreeNode)?.let { result += it }
        }
        return result
    }

    private fun restoreExpanded(expanded: Set<String>) {
        val firstRender = expanded.isEmpty()
        var row = 0
        while (row < tree.rowCount) {
            val path = tree.getPathForRow(row) ?: break
            val node = path.lastPathComponent as? DefaultMutableTreeNode
            val key = keyOf(node)
            val shouldExpand = when {
                key == null -> false
                firstRender -> node?.userObject !is NodeData.Component
                else -> key in expanded
            }
            if (shouldExpand) {
                tree.expandPath(path)
            }
            row++
        }
    }

    private fun keyOf(node: DefaultMutableTreeNode?): String? = when (val data = node?.userObject) {
        is NodeData.Project -> "project:${data.name}"
        is NodeData.Group -> "group:${data.project}/${data.group.kind.name}"
        is NodeData.Component -> "component:${data.project}/${data.node.qualifiedName}"
        is NodeData.Item -> "item:${data.node.name}"
        else -> null
    }

    // ---------------------------------------------------------------------------------------------
    // 交互
    // ---------------------------------------------------------------------------------------------

    private fun installMouseHandler() {
        tree.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    if (event.clickCount != 2) {
                        return
                    }
                    val path = tree.getPathForLocation(event.point.x, event.point.y) ?: return
                    activate((path.lastPathComponent as? DefaultMutableTreeNode)?.userObject)
                }
            }
        )
    }

    private fun installKeyboardHandler() {
        tree.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "lucky.navigate")
        tree.actionMap.put(
            "lucky.navigate",
            object : AbstractAction() {
                override fun actionPerformed(event: java.awt.event.ActionEvent) {
                    val path: TreePath = tree.selectionPath ?: return
                    activate((path.lastPathComponent as? DefaultMutableTreeNode)?.userObject)
                }
            }
        )
    }

    /** 双击组件/接口/函数跳转到源码；元素已失效时重建树。 */
    private fun activate(data: Any?) {
        val element = when (data) {
            is NodeData.Component -> data.node.psiClass
            is NodeData.Item -> data.node.element
            else -> null
        } ?: return
        if (!element.isValid) {
            refresh()
            return
        }
        // navigate(true) 会打开所在文件、定位到元素并请求焦点
        (element as? NavigatablePsiElement)?.navigate(true)
    }

    private fun onPsiChanged() {
        if (disposed || !toolWindow.isVisible) {
            return
        }
        val stamp = PsiModificationTracker.getInstance(project).modificationCount
        if (stamp == lastStamp) {
            return
        }
        lastStamp = stamp
        refresh()
    }

    /**
     * 周期性检查 PSI 版本号。
     *
     * 2024.1 的 `PsiModificationTracker` 没有稳定的监听 API，这里在 EDT 上
     * 只读一个 long 做比对，开销可忽略；面板不可见时不触发重建。
     */
    private fun scheduleStampCheck() {
        alarm.addRequest(
            {
                if (!disposed) {
                    onPsiChanged()
                    scheduleStampCheck()
                }
            },
            STAMP_CHECK_DELAY_MS
        )
    }

    private inner class RefreshAction : AnAction(
        "刷新",
        "重新扫描 Lucky 组件",
        AllIcons.Actions.Refresh
    ) {
        override fun actionPerformed(event: AnActionEvent) = refresh()
    }

    // ---------------------------------------------------------------------------------------------
    // 节点与渲染
    // ---------------------------------------------------------------------------------------------

    private sealed class NodeData {
        data class Project(val name: String) : NodeData()
        data class Group(val project: String, val group: LuckyGroupNode) : NodeData()
        data class Component(val project: String, val node: LuckyComponentNode) : NodeData()
        data class Item(val node: LuckyItemNode) : NodeData()
        data class Message(val text: String) : NodeData()
    }

    private inner class NodeRenderer : ColoredTreeCellRenderer() {

        override fun customizeCellRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val data = (value as? DefaultMutableTreeNode)?.userObject ?: return
            when (data) {
                is NodeData.Project -> {
                    icon = AllIcons.Nodes.Module
                    append(data.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                }

                is NodeData.Group -> {
                    icon = AllIcons.Nodes.Folder
                    append(data.group.kind.title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                    append("  ${data.group.components.size}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }

                is NodeData.Component -> {
                    icon = ComponentIconResolver.icon() ?: AllIcons.Nodes.Class
                    append(data.node.title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                    data.node.subtitle?.takeIf { it.isNotEmpty() }?.let {
                        append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                    toolTipText = data.node.qualifiedName
                }

                is NodeData.Item -> when (val item = data.node) {
                    is LuckyItemNode.Api -> {
                        icon = LuckyIcons.httpMethod(item.httpMethod) ?: AllIcons.Nodes.Method
                        append(item.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                        append("  ${item.subtitle}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                        toolTipText = listOfNotNull(item.httpMethod, item.subtitle).joinToString(" ")
                    }

                    is LuckyItemNode.Function -> {
                        icon = AllIcons.Nodes.Method
                        append(item.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                        append("  ${item.subtitle}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                        toolTipText = "${item.name} ${item.subtitle}"
                    }
                }

                is NodeData.Message -> {
                    icon = null
                    append(data.text, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        }
    }

    private companion object {
        private const val STAMP_CHECK_DELAY_MS = 1500
    }
}
