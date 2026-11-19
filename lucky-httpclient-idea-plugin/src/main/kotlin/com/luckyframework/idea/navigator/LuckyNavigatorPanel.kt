package com.luckyframework.idea.navigator

import com.intellij.icons.AllIcons
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import com.luckyframework.idea.debug.invoke.DebugTarget
import com.luckyframework.idea.debug.invoke.DebugTargetFactory
import com.luckyframework.idea.debug.ui.DebugNavigatorPanel
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.component.ComponentIconResolver
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import com.luckyframework.idea.std.StdHttpClientIndex
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.ToolTipManager
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

/**
 * Lucky 侧边导航栏：模块/外部库/jar → 固定分组 → 包 → 组件 → 接口方法/函数。
 *
 * 数据由 [LuckyNavigatorModelBuilder] 在后台 read action 中构建，EDT 只做
 * 树渲染；双击组件/接口/函数跳转到源码 PSI 位置，右键接口方法可打开调试面板；
 * 接口方法右键菜单提供 Debug Method 与 Go to Source。PSI 版本变化后重建，
 * 展开状态按节点 key 恢复。首次渲染只展开项目模块，分组与包保持折叠；
 * 顶部搜索框按关键字过滤当前树，输入时自动展开命中节点并高亮命中文本，
 * 清空后还原完整树。标题栏齿轮菜单可关闭包名展示。
 */
class LuckyNavigatorPanel(
    private val project: Project,
    private val toolWindow: ToolWindow
) : Disposable {

    private val builder = LuckyNavigatorModelBuilder()

    private val root = DefaultMutableTreeNode()

    private val treeModel = DefaultTreeModel(root)

    private val tree = JTree(treeModel)

    private val renderer = NodeRenderer()

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

    /** 上次重建时看到的项目根版本号；依赖导入/模块根变化后重建。 */
    @Volatile
    private var lastRootsStamp = -1L

    /** 顶部搜索框：输入后只展示匹配的组件/接口。 */
    private val searchField = SearchTextField()

    /** 是否已经应用并记录过默认展开状态：未应用前，首个带数据的渲染只展开项目模块。 */
    private var defaultExpansionApplied = false

    /**
     * 非搜索状态下的展开 key。
     *
     * 搜索时的自动展开是临时的：过滤渲染不覆盖该值，清空搜索后按它还原；
     * 搜索无结果时树中只剩提示行，也不会把用户之前的展开状态清空。
     */
    private var expandedKeys: Set<String> = emptySet()

    /** 上一次渲染时的搜索词，用于区分「刚清空搜索」和普通的空搜索渲染。 */
    private var lastQuery = ""

    /** 最近一次完整模型；过滤始终基于它，清空搜索框即可还原。 */
    private var lastModel = LuckyNavigatorModel(emptyList())

    /** Tool Window 内容组件：搜索框 + 组件树。 */
    val component: JComponent = JPanel(BorderLayout()).apply {
        add(
            JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(4, 4, 2, 4)
                add(searchField, BorderLayout.CENTER)
            },
            BorderLayout.NORTH
        )
        add(JBScrollPane(tree), BorderLayout.CENTER)
    }

    /**
     * 已展开的 `${...}` 折叠组；为空时全部展示解析值。
     *
     * 折叠组 id 由文件、字面量偏移与占位符偏移组成，模型重建后依然稳定；
     * 展开状态属于视图层，不进入可测试的 [LuckyNavigatorModel]。
     */
    private val expandedFolds = HashSet<String>()

    init {
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.cellRenderer = renderer
        tree.toolTipText = ""
        ToolTipManager.sharedInstance().registerComponent(tree)
        searchField.textEditor.emptyText.text = "Search components, APIs, paths, or packages"
        searchField.toolTipText = "Filter Lucky components and APIs by keyword"
        searchField.addDocumentListener(
            object : DocumentListener {
                override fun insertUpdate(event: DocumentEvent) = renderFiltered()

                override fun removeUpdate(event: DocumentEvent) = renderFiltered()

                override fun changedUpdate(event: DocumentEvent) = Unit
            }
        )
        installMouseHandler()
        installKeyboardHandler()
        toolWindow.setTitleActions(listOf(RefreshAction(), SettingsActionGroup()))
        lastStamp = PsiModificationTracker.getInstance(project).modificationCount
        lastRootsStamp = ProjectRootModificationTracker.getInstance(project).modificationCount
        invalidateScans()
        refresh()
        DumbService.getInstance(project).runWhenSmart {
            if (!disposed) {
                // 首次索引期间可能缓存了空结果，智能模式后必须丢弃并重扫
                invalidateScans()
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
            showMessage("Scanning Lucky components…")
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

    /** 丢弃组件/标准客户端扫描缓存，下一次构建强制重新读取索引与依赖。 */
    private fun invalidateScans() {
        SpelRuntimeSymbols.invalidate(project)
        StdHttpClientIndex.getInstance(project).invalidate()
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
        lastModel = model
        renderFiltered()
    }

    /** 按当前搜索词过滤 [lastModel] 并重建树；搜索时自动展开全部命中节点。 */
    private fun renderFiltered() {
        val query = searchField.text.trim()
        // 普通渲染先读取用户当前的展开状态；搜索渲染不覆盖它，清空搜索后再还原
        val expanded = if (query.isEmpty() && lastQuery.isEmpty()) {
            collectExpandedKeys().also { expandedKeys = it }
        } else {
            expandedKeys
        }
        val visible = if (query.isEmpty()) lastModel else LuckyNavigatorFilter.filter(lastModel, query)
        root.removeAllChildren()
        when {
            lastModel.projects.isEmpty() && refreshing ->
                root.add(DefaultMutableTreeNode(NodeData.Message("Scanning Lucky components…")))

            lastModel.projects.isEmpty() ->
                root.add(DefaultMutableTreeNode(NodeData.Message(emptyMessage())))

            visible.projects.isEmpty() ->
                root.add(DefaultMutableTreeNode(NodeData.Message("No Lucky components match \"$query\"")))

            else -> addProjects(visible)
        }
        treeModel.reload()
        val firstRender = !defaultExpansionApplied && visible.projects.isNotEmpty()
        restoreExpanded(expanded, expandAll = query.isNotEmpty(), firstRender = firstRender)
        // 默认展开发生在本次渲染中，需要立即记住；搜索渲染不记录，清空后再应用默认展开
        if (query.isEmpty() && visible.projects.isNotEmpty()) {
            defaultExpansionApplied = true
            expandedKeys = collectExpandedKeys()
        }
        lastQuery = query
    }

    /** 按 模块/外部库/jar → 固定分组 → 包 → 组件 → 方法 递归构建树节点。 */
    private fun addProjects(model: LuckyNavigatorModel) {
        for (projectNode in model.projects) {
            addContainer(projectNode, root)
        }
    }

    private fun addContainer(container: LuckyProjectNode, parent: DefaultMutableTreeNode) {
        val containerTreeNode = DefaultMutableTreeNode(NodeData.Project(container))
        // 子模块/子库在前，随后是本节点自己的固定分组
        for (child in container.children) {
            addContainer(child, containerTreeNode)
        }
        for (group in container.groups) {
            val groupTreeNode = DefaultMutableTreeNode(NodeData.Group(container.name, group))
            for (pkg in group.packages) {
                if (showPackageNames) {
                    val packageTreeNode =
                        DefaultMutableTreeNode(NodeData.Package(container.name, group.kind, pkg))
                    addComponents(container.name, pkg.components, packageTreeNode)
                    groupTreeNode.add(packageTreeNode)
                } else {
                    // 关闭包名后组件直接挂在固定分组下，不再插入包节点
                    addComponents(container.name, pkg.components, groupTreeNode)
                }
            }
            containerTreeNode.add(groupTreeNode)
        }
        parent.add(containerTreeNode)
    }

    /** 添加组件及其方法/函数项，供「展示包名」开关决定挂在包节点还是分组节点下。 */
    private fun addComponents(
        containerName: String,
        components: List<LuckyComponentNode>,
        parent: DefaultMutableTreeNode
    ) {
        for (component in components) {
            val componentTreeNode =
                DefaultMutableTreeNode(NodeData.Component(containerName, component))
            for (item in component.items) {
                componentTreeNode.add(DefaultMutableTreeNode(NodeData.Item(item)))
            }
            parent.add(componentTreeNode)
        }
    }

    /** 导航树是否展示包名：关闭后不渲染包节点，也不展示包名副标题。 */
    private val showPackageNames: Boolean
        get() = LuckyHttpClientSettings.getInstance().isShowPackageNames

    private fun showMessage(text: String) {
        root.removeAllChildren()
        root.add(DefaultMutableTreeNode(NodeData.Message(text)))
        treeModel.reload()
    }

    private fun emptyMessage(): String = when {
        !LuckyHttpClientSettings.getInstance().isEnabled ->
            "Lucky HttpClient is disabled. Enable it in Settings | Tools | Lucky HttpClient"

        DumbService.isDumb(project) -> "Indexing in progress. The view will refresh automatically…"
        else -> "No Lucky components found in this project"
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

    private fun restoreExpanded(
        expanded: Set<String>,
        expandAll: Boolean,
        firstRender: Boolean
    ) {
        var row = 0
        while (row < tree.rowCount) {
            val path = tree.getPathForRow(row) ?: break
            val node = path.lastPathComponent as? DefaultMutableTreeNode
            val key = keyOf(node)
            val shouldExpand = when {
                key == null -> false
                expandAll -> (node?.childCount ?: 0) > 0
                // 首次渲染只展开项目模块，分组与包保持折叠；外部库节点也保持折叠
                firstRender -> isModuleNode(node)
                else -> key in expanded
            }
            if (shouldExpand) {
                tree.expandPath(path)
            }
            row++
        }
    }

    private fun isModuleNode(node: DefaultMutableTreeNode?): Boolean =
        (node?.userObject as? NodeData.Project)?.node?.kind == LuckyProjectKind.MODULE

    private fun keyOf(node: DefaultMutableTreeNode?): String? = when (val data = node?.userObject) {
        is NodeData.Project -> "project:${data.node.kind.name}:${data.node.name}"
        is NodeData.Group -> "group:${data.project}/${data.group.kind.name}"
        is NodeData.Package -> "package:${data.project}/${data.kind.name}/${data.node.name}"
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
                    if (!SwingUtilities.isLeftMouseButton(event)) {
                        return
                    }
                    val treePath = tree.getPathForLocation(event.point.x, event.point.y)
                    val node = treePath?.lastPathComponent as? DefaultMutableTreeNode
                    val segment = if (treePath != null && node != null) {
                        segmentAt(node, treePath, event.point.x)
                    } else {
                        null
                    }
                    if (node != null && segment != null && handlePathClick(segment, node, event.clickCount)) {
                        return
                    }
                    if (event.clickCount == 2) {
                        activate(node?.userObject)
                    }
                }

                override fun mousePressed(event: MouseEvent) {
                    showPopupIfNeeded(event)
                }

                override fun mouseReleased(event: MouseEvent) {
                    showPopupIfNeeded(event)
                }
            }
        )
    }

    /** 右键接口方法：Debug Method / Go to Source。 */
    private fun showPopupIfNeeded(event: MouseEvent) {
        if (!event.isPopupTrigger) {
            return
        }
        val path = tree.getPathForLocation(event.x, event.y) ?: return
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
        val item = (node.userObject as? NodeData.Item)?.node as? LuckyItemNode.Api ?: return
        tree.selectionPath = path
        val group = DefaultActionGroup().apply {
            // 外部库中的接口方法没有所属模块，无法构建调试 classpath，只保留源码跳转
            if (item.element.isValid && ModuleUtilCore.findModuleForPsiElement(item.element) != null) {
                add(DebugMethodAction(item))
            }
            add(GoToSourceAction(item))
        }
        ActionManager.getInstance()
            .createActionPopupMenu("LuckyNavigatorPanel", group)
            .component
            .show(tree, event.x, event.y)
    }

    /** 右键入口：在后台 read action 中解析目标，再回到 EDT 打开调试面板。 */
    private fun openDebug(item: LuckyItemNode.Api) {
        val method = item.element as? PsiMethod ?: return
        if (!method.isValid) {
            refresh()
            return
        }
        val module = ModuleUtilCore.findModuleForPsiElement(method) ?: return
        ReadAction.nonBlocking<DebugTarget?> {
            if (disposed || project.isDisposed || !method.isValid) {
                null
            } else {
                DebugTargetFactory.of(module, method)
            }
        }.finishOnUiThread(ModalityState.nonModal()) { target ->
            if (target != null && !disposed && !project.isDisposed) {
                DebugNavigatorPanel.open(project, target)
            }
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    /**
     * 鼠标位置对应的 path 片段。
     *
     * JTree 的双击/单击事件不会先配置行渲染器，这里手动调用一次
     * `getTreeCellRendererComponent` 让片段坐标与被点击行一致。
     */
    private fun segmentAt(
        node: DefaultMutableTreeNode,
        treePath: TreePath,
        xInTree: Int
    ): LuckyPathSegment? {
        val row = tree.getRowForPath(treePath)
        val bounds = tree.getPathBounds(treePath)
        if (row < 0 || bounds == null) {
            return null
        }
        renderer.getTreeCellRendererComponent(
            tree,
            node,
            tree.isRowSelected(row),
            tree.isExpanded(treePath),
            node.isLeaf,
            row,
            true
        )
        return renderer.segmentAt(xInTree - bounds.x)
    }

    /**
     * path 片段的单击行为：折叠组切换展开/收起，可导航片段打开源码。
     *
     * 返回 true 表示事件已被 path 消费，不再触发整行激活。
     */
    private fun handlePathClick(
        segment: LuckyPathSegment,
        node: DefaultMutableTreeNode,
        clickCount: Int
    ): Boolean {
        val fold = segment.fold
        if (fold != null) {
            if (clickCount == 1) {
                if (!expandedFolds.remove(fold.id)) {
                    expandedFolds += fold.id
                }
                treeModel.nodeChanged(node)
            }
            return true
        }
        val navigation = segment.navigation
        if (navigation != null) {
            if (clickCount == 1 && navigation.isValid) {
                navigate(navigation)
            }
            // 单击 path 打开配置；双击 path 继续交给整行激活（跳转源码）
            return clickCount == 1
        }
        return false
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
        navigate(element)
    }

    /**
     * 解析导航描述符会读取 stub/索引；依赖 jar 的类还要查找 source mirror，
     * 不能在 EDT 上执行。这里在后台 read action 中取描述符，再回到 EDT 打开文件。
     */
    private fun navigate(element: PsiElement) {
        ReadAction.nonBlocking<Navigatable?> {
            if (disposed || project.isDisposed || !element.isValid) {
                null
            } else {
                PsiNavigationSupport.getInstance().getDescriptor(element)
            }
        }.finishOnUiThread(ModalityState.nonModal()) { descriptor ->
            if (!disposed && !project.isDisposed) {
                descriptor?.navigate(true)
            }
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun onPsiChanged() {
        if (disposed || !toolWindow.isVisible) {
            return
        }
        val stamp = PsiModificationTracker.getInstance(project).modificationCount
        val rootsStamp = ProjectRootModificationTracker.getInstance(project).modificationCount
        if (stamp == lastStamp && rootsStamp == lastRootsStamp) {
            return
        }
        lastStamp = stamp
        lastRootsStamp = rootsStamp
        refresh()
    }

    /**
     * 周期性检查 PSI 版本号。
     *
     * 2024.1 的 `PsiModificationTracker` 没有稳定的监听 API，这里在 EDT 上
     * 只读两个 long 做比对，开销可忽略；面板不可见时不触发重建。
     * 项目根版本号用于覆盖首次打开时依赖模型晚于索引就绪的场景。
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
        "Refresh",
        "Rescan Lucky components",
        AllIcons.Actions.Refresh
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(event: AnActionEvent) {
            invalidateScans()
            refresh()
        }
    }

    /** 标题栏齿轮菜单：当前提供包名展示开关。 */
    private inner class SettingsActionGroup : DefaultActionGroup("Navigator Settings", true) {
        init {
            templatePresentation.icon = AllIcons.General.GearPlain
            templatePresentation.description = "Navigator settings"
            add(ShowPackageNamesAction())
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }

    private inner class ShowPackageNamesAction : ToggleAction(
        "Show Package Names",
        "Show package nodes and package subtitles in the navigator tree",
        null
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(event: AnActionEvent): Boolean =
            LuckyHttpClientSettings.getInstance().isShowPackageNames

        override fun setSelected(event: AnActionEvent, state: Boolean) {
            LuckyHttpClientSettings.getInstance().isShowPackageNames = state
            renderFiltered()
        }
    }

    /** 右键菜单：打开接口方法调试面板。 */
    private inner class DebugMethodAction(
        private val item: LuckyItemNode.Api
    ) : AnAction(
        "Debug Method",
        "Open the debug panel for this interface method",
        AllIcons.Actions.Execute
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(event: AnActionEvent) {
            openDebug(item)
        }
    }

    /** 右键菜单：跳转到方法源码。 */
    private inner class GoToSourceAction(
        private val item: LuckyItemNode.Api
    ) : AnAction(
        "Go to Source",
        "Navigate to the method source",
        null
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(event: AnActionEvent) {
            if (item.element.isValid) {
                navigate(item.element)
            } else {
                refresh()
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 节点与渲染
    // ---------------------------------------------------------------------------------------------

    private sealed class NodeData {
        data class Project(val node: LuckyProjectNode) : NodeData()
        data class Group(val project: String, val group: LuckyGroupNode) : NodeData()
        data class Package(
            val project: String,
            val kind: LuckyGroupKind,
            val node: LuckyPackageNode
        ) : NodeData()

        data class Component(val project: String, val node: LuckyComponentNode) : NodeData()
        data class Item(val node: LuckyItemNode) : NodeData()
        data class Message(val text: String) : NodeData()
    }

    private inner class NodeRenderer : ColoredTreeCellRenderer() {

        /** 当前行渲染出的 path 片段；片段 tag 是它在列表中的下标。 */
        private var pathSegments: List<LuckyPathSegment> = emptyList()

        /** path 实际渲染的文本与样式，用于计算灰色圆角背景的宽度。 */
        private val pathRendered = ArrayList<RenderedPathFragment>()

        override fun customizeCellRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            pathSegments = emptyList()
            pathRendered.clear()
            val data = (value as? DefaultMutableTreeNode)?.userObject ?: return
            when (data) {
                is NodeData.Project -> {
                    icon = when (data.node.kind) {
                        LuckyProjectKind.MODULE -> AllIcons.Nodes.Module
                        LuckyProjectKind.LIBRARY -> AllIcons.Nodes.PpLib
                        LuckyProjectKind.JAR -> AllIcons.Nodes.PpJar
                    }
                    appendHighlighted(data.node.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                    if (data.node.componentCount > 0) {
                        append("  ${data.node.componentCount}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                    toolTipText = when (data.node.kind) {
                        LuckyProjectKind.MODULE -> data.node.name
                        LuckyProjectKind.LIBRARY -> "Lucky components from dependency jars (framework built-ins and third-party extensions)"
                        LuckyProjectKind.JAR -> "Lucky components from this dependency"
                    }
                }

                is NodeData.Group -> {
                    icon = AllIcons.Nodes.Folder
                    appendHighlighted(data.group.kind.title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                    append("  ${data.group.components.size}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }

                is NodeData.Package -> {
                    icon = AllIcons.Nodes.Package
                    appendHighlighted(data.node.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    append("  ${data.node.components.size}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    toolTipText = data.node.name
                }

                is NodeData.Component -> {
                    icon = ComponentIconResolver.icon() ?: AllIcons.Nodes.Class
                    appendHighlighted(data.node.title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                    val baseUrl = data.node.baseUrl
                    val subtitle = data.node.subtitle?.takeIf { it.isNotEmpty() }
                    // baseUrl 与接口 path 走同一条富文本链路；关闭包名后仍保留
                    when {
                        baseUrl != null -> {
                            append("  ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                            appendPath(baseUrl)
                        }

                        subtitle != null && (showPackageNames || !isPackageSubtitle(data.node)) -> {
                            append("  ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                            appendHighlighted(subtitle, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                        }
                    }
                    toolTipText = data.node.qualifiedName
                }

                is NodeData.Item -> when (val item = data.node) {
                    is LuckyItemNode.Api -> {
                        val signature = item.signature
                        if (signature != null) {
                            icon = AllIcons.Nodes.Method
                            appendHighlighted(signature, REGULAR_ATTRIBUTES)
                            toolTipText = signature
                        } else {
                            icon = LuckyIcons.httpMethod(item.httpMethod) ?: AllIcons.Nodes.Method
                            appendHighlighted(item.name, REGULAR_ATTRIBUTES)
                            val path = item.path
                            if (path != null) {
                                append("  ", GRAYED_ATTRIBUTES)
                                appendPath(path)
                            } else {
                                item.subtitle?.let {
                                    append("  ", GRAYED_ATTRIBUTES)
                                    appendHighlighted(it, GRAYED_ATTRIBUTES)
                                }
                            }
                            toolTipText = listOfNotNull(item.httpMethod, item.subtitle).joinToString(" ")
                        }
                    }

                    is LuckyItemNode.Function -> {
                        icon = AllIcons.Nodes.Method
                        appendHighlighted(item.name, REGULAR_ATTRIBUTES)
                        item.subtitle?.let {
                            append("  ", GRAYED_ATTRIBUTES)
                            appendHighlighted(it, GRAYED_ATTRIBUTES)
                        }
                        toolTipText = item.subtitle?.let { "${item.name} $it" } ?: item.name
                    }
                }

                is NodeData.Message -> {
                    icon = null
                    append(data.text, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        }

        /** 追加文本，命中当前搜索词的片段用高亮颜色显示；tag 用于鼠标命中。 */
        private fun appendHighlighted(
            text: String,
            attributes: SimpleTextAttributes,
            tag: Int? = null
        ) {
            val ranges = LuckySearchHighlight.rangesOf(text, searchField.text)
            if (ranges.isEmpty()) {
                append(text, attributes, tag)
                return
            }
            var cursor = 0
            for (range in ranges) {
                if (range.first > cursor) {
                    append(text.substring(cursor, range.first), attributes, tag)
                }
                append(text.substring(range.first, range.last + 1), SEARCH_MATCH_ATTRIBUTES, tag)
                cursor = range.last + 1
            }
            if (cursor < text.length) {
                append(text.substring(cursor), attributes, tag)
            }
        }

        /**
         * 追加 path 富文本：SpEL token 按编辑器同一套颜色着色，
         * `${...}` 默认显示配置值，点击后展开原表达式。
         */
        private fun appendPath(presentation: LuckyPathPresentation) {
            pathSegments = presentation.segments
            val renderedFolds = HashSet<String>()
            for ((index, segment) in presentation.segments.withIndex()) {
                val fold = segment.fold
                if (fold != null && fold.id !in expandedFolds) {
                    // 同一折叠组的后续片段不再输出，整段替换为配置值
                    if (renderedFolds.add(fold.id)) {
                        val attributes = attributesOf(fold.attributesKey)
                        pathRendered += RenderedPathFragment(fold.text, attributes)
                        appendHighlighted(fold.text, attributes, index)
                    }
                    continue
                }
                val attributes = attributesOf(segment)
                pathRendered += RenderedPathFragment(segment.text, attributes)
                appendHighlighted(segment.text, attributes, index)
            }
        }

        private fun attributesOf(segment: LuckyPathSegment): SimpleTextAttributes = when {
            segment.grayed -> GRAYED_ATTRIBUTES
            segment.attributesKey == null -> PATH_TEXT_ATTRIBUTES
            else -> attributesOf(segment.attributesKey)
        }

        /**
         * 颜色表里找不到键时回退到键自带的默认属性：其它主题没有为插件
         * 注册 additionalTextAttributes 时，`${...}` 仍需保留下划线约定样式。
         */
        private fun attributesOf(key: TextAttributesKey?): SimpleTextAttributes {
            if (key == null) {
                return REGULAR_ATTRIBUTES
            }
            val attributes = runCatching {
                EditorColorsManager.getInstance().globalScheme.getAttributes(key)
            }.getOrNull()
            val effective = attributes?.takeUnless { it.isEmpty } ?: key.defaultAttributes
            return if (effective == null) {
                REGULAR_ATTRIBUTES
            } else {
                SimpleTextAttributes.fromTextAttributes(effective)
            }
        }

        /** 像素 x 对应的模型片段；非 path 片段（图标、名称、副标题）返回 null。 */
        fun segmentAt(x: Int): LuckyPathSegment? {
            val tag = getFragmentTagAt(x) as? Int ?: return null
            return pathSegments.getOrNull(tag)
        }

        /**
         * path 圆角背景在文本之前绘制；临时关闭 opaque，避免 super 用行背景
         * 覆盖圆角矩形（行背景由 JTree 负责绘制）。
         */
        override fun paintComponent(g: Graphics) {
            paintPathBackground(g)
            val opaque = isOpaque
            isOpaque = false
            try {
                super.paintComponent(g)
            } finally {
                isOpaque = opaque
            }
        }

        private fun paintPathBackground(g: Graphics) {
            val bounds = pathBackgroundBounds() ?: return
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = PATH_BACKGROUND
                val arc = JBUI.scale(PATH_BACKGROUND_ARC)
                g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc)
            } finally {
                g2.dispose()
            }
        }

        /** path 文本在行内的像素范围；没有 path 或尚未布局时返回 null。 */
        private fun pathBackgroundBounds(): Rectangle? {
            if (pathRendered.isEmpty() || width <= 0 || height <= 0) {
                return null
            }
            val start = firstPathPixel() ?: return null
            val metrics = getFontMetrics(font ?: return null)
            val textWidth = pathRendered.sumOf { widthOf(it, metrics) }
            if (textWidth <= 0) {
                return null
            }
            val paddingX = JBUI.scale(PATH_PADDING_X)
            val paddingY = JBUI.scale(PATH_PADDING_Y)
            val blockHeight = (metrics.height + paddingY * 2).coerceAtMost(height)
            if (blockHeight <= 0) {
                return null
            }
            val left = (start - paddingX).coerceAtLeast(0)
            val right = (start + textWidth + paddingX).coerceAtMost(width)
            return if (right <= left) null else Rectangle(left, (height - blockHeight) / 2, right - left, blockHeight)
        }

        /**
         * 首个 path 片段的像素 x。
         *
         * `SimpleColoredComponent` 不公开片段坐标，按 [PATH_SCAN_STEP] 步长采样
         * [getFragmentTagAt] 定位 path 区域，再向左回扫到边界；极小 path 逐像素兜底。
         */
        private fun firstPathPixel(): Int? {
            var x = 0
            while (x < width) {
                if (isPathPixel(x)) {
                    var start = x
                    while (start > 0 && isPathPixel(start - 1)) {
                        start--
                    }
                    return start
                }
                x += PATH_SCAN_STEP
            }
            for (pixel in 0 until width) {
                if (isPathPixel(pixel)) {
                    return pixel
                }
            }
            return null
        }

        private fun isPathPixel(x: Int): Boolean {
            val tag = getFragmentTagAt(x) as? Int ?: return false
            return tag in pathSegments.indices
        }

        private fun widthOf(fragment: RenderedPathFragment, metrics: FontMetrics): Int {
            val baseFont = font ?: return metrics.stringWidth(fragment.text)
            val style = fragment.attributes.fontStyle
            val styledFont = if (style == baseFont.style) baseFont else baseFont.deriveFont(style)
            return getFontMetrics(styledFont).stringWidth(fragment.text)
        }

        /** 鼠标所在片段的悬停文档；无文档时回退整行提示。 */
        override fun getToolTipText(event: MouseEvent): String? {
            val tag = getFragmentTagAt(event.x) as? Int
            val documentation = tag?.let { pathSegments.getOrNull(it)?.documentation }
            if (!documentation.isNullOrBlank()) {
                return documentation
            }
            return super.getToolTipText(event)
        }
    }

    private companion object {
        private const val STAMP_CHECK_DELAY_MS = 1500

        /** 搜索命中片段：浅色主题深橙、深色主题亮橙，树行背景上均清晰可辨。 */
        private val SEARCH_MATCH_ATTRIBUTES = SimpleTextAttributes(
            SimpleTextAttributes.STYLE_BOLD,
            JBColor(Color(0x9E6A03), Color(0xFFAF0A))
        )

        /** path 文本颜色：比方法名更暗，与方法名区分且保持可读性。 */
        private val PATH_TEXT_ATTRIBUTES = SimpleTextAttributes(
            SimpleTextAttributes.STYLE_PLAIN,
            JBColor(Color(0x4B5057), Color(0x9AA4B0))
        )

        /** path 圆角底色：浅色主题淡灰、深色主题略亮的灰。 */
        private val PATH_BACKGROUND = JBColor(Color(0xEBECEF), Color(0x4A4E52))

        /** 圆角半径与内边距（逻辑像素，按 DPI 缩放）。 */
        private const val PATH_BACKGROUND_ARC = 6
        private const val PATH_PADDING_X = 4
        private const val PATH_PADDING_Y = 1

        /** 像素采样步长：先粗扫定位 path，再向左回扫边界。 */
        private const val PATH_SCAN_STEP = 4
        
        private val REGULAR_ATTRIBUTES: SimpleTextAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES
        
        private val GRAYED_ATTRIBUTES: SimpleTextAttributes = SimpleTextAttributes.GRAYED_ATTRIBUTES
    }
}

/** path 实际渲染的一段文本与样式，用于测量圆角背景宽度。 */
private class RenderedPathFragment(val text: String, val attributes: SimpleTextAttributes)

/** 组件副标题是否只是包名；关闭包名展示时不再渲染这类副标题。 */
internal fun isPackageSubtitle(component: LuckyComponentNode): Boolean {
    val packageName = component.qualifiedName.substringBeforeLast('.', "")
    return packageName.isNotEmpty() && component.subtitle == packageName
}

/** 搜索词在节点文本中的命中区间，大小写不敏感，供树渲染分段高亮。 */
internal object LuckySearchHighlight {

    fun rangesOf(text: String, query: String): List<IntRange> {
        val needle = query.trim()
        if (needle.isEmpty()) {
            return emptyList()
        }
        val result = ArrayList<IntRange>()
        var cursor = 0
        while (cursor < text.length) {
            val hit = text.indexOf(needle, cursor, ignoreCase = true)
            if (hit < 0) {
                break
            }
            result += hit until (hit + needle.length)
            cursor = hit + needle.length
        }
        return result
    }
}
