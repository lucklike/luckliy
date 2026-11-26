package com.luckyframework.idea.navigator

import com.intellij.icons.AllIcons
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.Disposable
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
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.pom.Navigatable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.scale.JBUIScale
import com.intellij.ui.tabs.JBTabs
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.TabsListener
import com.intellij.ui.tabs.impl.JBTabsImpl
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.luckyframework.idea.LuckyModificationTrackers
import com.luckyframework.idea.debug.container.DebugContainerDiscovery
import com.luckyframework.idea.debug.container.DebugContainerService
import com.luckyframework.idea.debug.container.DiscoveredContainer
import com.luckyframework.idea.debug.invoke.DebugTarget
import com.luckyframework.idea.debug.invoke.DebugTargetFactory
import com.luckyframework.idea.debug.ui.ContainerLogPanel
import com.luckyframework.idea.debug.ui.DebugNavigatorPanel
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.component.ComponentIconResolver
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import com.luckyframework.idea.std.StdHttpClientIndex
import java.awt.AWTEvent
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.HeadlessException
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.geom.RoundRectangle2D
import java.awt.event.AWTEventListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.ToolTipManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sin

/**
 * Lucky 侧边导航栏：模块/外部库/jar → 固定分组 → 包 → 组件 → 接口方法/函数。
 *
 * 数据由 [LuckyNavigatorModelBuilder] 在后台 read action 中构建，EDT 只做
 * 树渲染；双击组件/接口/函数跳转到源码 PSI 位置；项目模块内的接口方法在行首
 * HTTP 方法图标左侧展示调试图标（容器运行中恒显示），点击时选中该行并在标签栏
 * 打开独立调试页。
 * PSI 版本变化后重建，
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

    private val tree = NavigatorTree(treeModel)

    private val renderer = NodeRenderer()

    /** 项目行的容器状态点与行尾动作都从该服务读取。 */
    private val containerService = DebugContainerService.getInstance(project)

    /** 合并容器状态/日志高频通知触发的树重绘。 */
    private val containerRepaintPending = AtomicBoolean(false)

    /** 容器过渡态的行尾转圈：当前帧序号与是否已在播放。 */
    private var spinnerFrame = 0
    private var spinnerRunning = false

    /** 项目图标状态方块的呼吸动画是否在运行。 */
    private var badgeAnimationRunning = false

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

    /** 上次重建时看到的 Java 文件 PSI 版本号；轮询发现变化后重建。 */
    @Volatile
    private var lastStamp = -1L

    /** 上次重建时看到的配置文件修改计数；YAML/properties 变化后重建。 */
    @Volatile
    private var lastConfigStamp = -1L

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

    /**
     * 调试页内部的标签栏：所有接口调试页与容器日志页都在这里分页展示。
     *
     * 用平台自绘的 [JBTabs]（编辑器标签同一套实现）：标题与关闭按钮由平台
     * 绘制并把控布局，不依赖 `JTabbedPane` 的 tab component 机制——该机制在
     * 滚动标签布局下会把自定义标签头裁掉，只剩空白标签栏。
     */
    private val debugPageTabs: JBTabs = JBTabsFactory.createTabs(project, this)

    /**
     * 标签关闭图标的悬停跟踪。
     *
     * 平台 JBTabs 的标签动作默认常绘，只有编辑器标签会在未悬停时跳过
     * 绘制；这里用全局鼠标事件判断指针是否停在调试子标签上，落在标签
     * 上才绘制关闭图标。
     */
    private val closeIconHoverTracker = AWTEventListener { event ->
        if (event is MouseEvent && !disposed) {
            scheduleTabCloseIconRefresh()
        }
    }

    /** 是否已向 AWT 注册悬停跟踪；headless 测试环境没有 Toolkit。 */
    private var closeIconTrackingInstalled = false

    /** 关闭图标刷新合并标记：每轮事件队列最多按鼠标位置重算一次。 */
    private var closeIconRefreshPending = false

    /** 已打开的调试页；容器状态变化不再自动关闭，只由标签关闭按钮释放面板。 */
    private val debugTabs = ArrayList<DebugTab>()

    /** 一个调试页及其对应标签页。 */
    private class DebugTab(val panel: DebugNavigatorPanel) {
        /** 对应标签页；关闭与切换目标时按它定位。 */
        var info: TabInfo? = null
    }

    /** 容器日志面板；作为独立页展示。 */
    private var logPanel: ContainerLogPanel? = null

    /** 容器日志页的标签；关闭时按它移除。 */
    private var logPanelInfo: TabInfo? = null

    /** 导航树滚动区；作为独立的导航标签页展示。 */
    private val treeScroll = JBScrollPane(tree)

    /** 导航页：搜索框 + 导航树。 */
    private val navigatorPage = JPanel(BorderLayout()).apply {
        add(
            JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(4, 4, 2, 4)
                add(searchField, BorderLayout.CENTER)
            },
            BorderLayout.NORTH
        )
        add(treeScroll, BorderLayout.CENTER)
    }

    /** 调试子标签栏右侧的「隐藏标签」按钮：无边框，只在标签放不下时出现。 */
    private val debugMoreTabsButton: JButton = JButton(MoreTabsIcon).apply {
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusPainted = false
        isOpaque = false
        margin = java.awt.Insets(0, 0, 0, 0)
        preferredSize = JBUI.size(MORE_TABS_BUTTON_SIZE, MORE_TABS_BUTTON_SIZE)
        toolTipText = "Show hidden tabs"
        addActionListener { showHiddenTabsPopup() }
    }

    /** 调试子标签栏「隐藏标签」按钮宿主：可见性由标签是否溢出决定。 */
    private val debugMoreTabsHost = JPanel(BorderLayout()).apply {
        isOpaque = false
        add(debugMoreTabsButton, BorderLayout.CENTER)
        isVisible = false
    }

    /** 调试子标签栏与它的「隐藏标签」按钮并排。 */
    private val debugPageTabsHost = JPanel(BorderLayout()).apply {
        isOpaque = false
        add(debugPageTabs.component, BorderLayout.CENTER)
        add(debugMoreTabsHost, BorderLayout.EAST)
    }

    /** 没有打开任何调试页时的占位提示。 */
    private val debugEmptyPanel = JPanel(BorderLayout()).apply {
        add(
            JBLabel("Open a method from Navigator to start debugging.", SwingConstants.CENTER).apply {
                foreground = UIUtil.getLabelDisabledForeground()
            },
            BorderLayout.CENTER
        )
    }

    /** Debug 页内容：有调试页时展示子标签栏，没有时展示占位提示。 */
    private val debugCards = JPanel(CardLayout()).apply {
        add(debugEmptyPanel, DEBUG_EMPTY_CARD)
        add(debugPageTabsHost, DEBUG_TABS_CARD)
    }

    /**
     * 可运行容器列表：项目内自带 Spring Boot 主类的模块。
     *
     * 探测在后台读锁中完成；索引未就绪时先保持为空，智能模式结束后重试。
     */
    @Volatile
    private var discoveredContainers: List<DiscoveredContainer> = emptyList()

    /** 容器栏中按模块名索引的条目；状态变化时原地更新，不重建组件。 */
    private val containerChips = LinkedHashMap<String, ContainerChip>()

    /** 容器栏条目容器：多个容器横向排开，放不下时横向滚动。 */
    private val containerBarContent = JPanel(
        FlowLayout(FlowLayout.LEFT, JBUI.scale(CONTAINER_CHIP_GAP), 0)
    ).apply {
        isOpaque = false
        border = JBUI.Borders.empty(
            CONTAINER_BAR_PADDING_Y,
            CONTAINER_BAR_PADDING_X,
            CONTAINER_BAR_PADDING_Y,
            CONTAINER_BAR_PADDING_X
        )
    }

    /** 容器栏滚动区：保持单行高度，容器多时横向滚动而不是换行。 */
    private val containerBarScroll = JBScrollPane(
        containerBarContent,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ).apply {
        border = JBUI.Borders.empty()
        isOpaque = false
        viewport.isOpaque = false
    }

    /**
     * 调试页顶部的容器栏：列出所有可运行容器与状态，并直接提供启动/重启/停止。
     *
     * 没有可运行容器时整栏隐藏，不占用调试页空间。
     */
    private val containerBar = JPanel(BorderLayout()).apply {
        isOpaque = false
        border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)
        add(containerBarScroll, BorderLayout.CENTER)
        isVisible = false
    }

    /** Debug 页内容：顶部容器栏，下方为调试子标签栏或占位提示。 */
    private val debugPage = JPanel(BorderLayout()).apply {
        isOpaque = false
        add(containerBar, BorderLayout.NORTH)
        add(debugCards, BorderLayout.CENTER)
    }

    /** 顶层页签的内容卡片：只显示当前选中的导航页或调试页。 */
    private val topPages = JPanel(CardLayout()).apply {
        add(navigatorPage, NAVIGATOR_CARD)
        add(debugPage, DEBUG_CARD)
    }

    /** 当前显示的顶层页签；标题栏动作按页分派。 */
    private var currentTopPage = TopPage.NAVIGATOR

    /** 导航页图标按钮：默认选中。 */
    private val navigatorButton = TopPageButton(
        LuckyIcons.ToolWindow,
        NAVIGATOR_TAB_TITLE,
        "$NAVIGATOR_TAB_TITLE — $NAVIGATOR_TAB_TOOLTIP"
    ) {
        selectTopPage(TopPage.NAVIGATOR)
    }

    /** 调试页图标按钮：打开调试页时自动选中。 */
    private val debugButton = TopPageButton(
        LuckyIcons.Send,
        DEBUG_TAB_TITLE,
        "$DEBUG_TAB_TITLE — $DEBUG_TAB_TOOLTIP"
    ) {
        selectTopPage(TopPage.DEBUG)
    }

    /** 顶层图标页签栏：两个按钮紧邻，间距与工具窗图标栏一致。 */
    private val topPageBar = JPanel(
        FlowLayout(FlowLayout.LEFT, JBUI.scale(TOP_PAGE_BUTTON_GAP), 0)
    ).apply {
        isOpaque = false
        border = JBUI.Borders.empty(3, 2)
        add(navigatorButton)
        add(debugButton)
    }

    /** Tool Window 内容组件：顶部图标页签栏，下方为当前页内容。 */
    val component: JComponent = JPanel(BorderLayout()).apply {
        add(topPageBar, BorderLayout.NORTH)
        add(topPages, BorderLayout.CENTER)
    }

    /**
     * 已展开的 `${...}` 折叠组；为空时全部展示解析值。
     *
     * 折叠组 id 由文件、字面量偏移与占位符偏移组成，模型重建后依然稳定；
     * 展开状态属于视图层，不进入可测试的 [LuckyNavigatorModel]。
     */
    private val expandedFolds = HashSet<String>()

    init {
        // 顶层两个图标页签：默认选中导航页
        selectTopPage(TopPage.NAVIGATOR)
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.cellRenderer = renderer
        tree.toolTipText = ""
        // 行尾图标可能超出树默认的选中背景，选择变化时整树重绘以补足选区
        tree.addTreeSelectionListener { tree.repaint() }
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
        installCloseIconTracking()
        // 选中标签时平台会重建标签动作面板（重建后关闭图标恢复默认绘制），
        // 选择变化后在事件队列末尾重新套用悬停规则。
        // 不重写 beforeSelectionChanged/tabRemoved：平台在 addTab 时会以 null
        // 作为 oldSelection 回调，Kotlin 非空参数会直接抛 NPE。
        debugPageTabs.addListener(
            object : TabsListener {
                override fun selectionChanged(oldSelection: TabInfo?, newSelection: TabInfo?) {
                    scheduleTabCloseIconRefresh()
                    // 调试页切换后同步容器条目的选中项
                    updateContainerSelection()
                }

                override fun tabsMoved() = scheduleTabCloseIconRefresh()
            },
            alarmDisposable
        )
        // 标签区宽度变化时重新判断是否需要「隐藏标签」按钮
        debugPageTabs.component.addComponentListener(
            object : ComponentAdapter() {
                override fun componentResized(event: ComponentEvent) =
                    updateMoreTabsButton(debugPageTabs, debugPageTabsHost, debugMoreTabsButton, debugMoreTabsHost)
            }
        )
        // 调试子标签的右键菜单：关闭系列、复制引用与跳转源码
        (debugPageTabs as? JBTabsImpl)?.addTabMouseListener(
            object : MouseAdapter() {
                override fun mousePressed(event: MouseEvent) = maybeShowDebugTabPopup(event)

                override fun mouseReleased(event: MouseEvent) = maybeShowDebugTabPopup(event)
            }
        )
        containerService.addListener({ scheduleContainerRepaint() }, alarmDisposable)
        refreshTitleActions()
        lastStamp = LuckyModificationTrackers.java(project).modificationCount
        lastConfigStamp = LuckyModificationTrackers.configFiles(project).modificationCount
        lastRootsStamp = LuckyModificationTrackers.roots(project)
        invalidateScans()
        refresh()
        // 先展示已创建容器的状态，再后台探测项目里所有可运行容器
        rebuildContainerBar()
        discoverContainers()
        DumbService.getInstance(project).runWhenSmart {
            if (!disposed) {
                // 首次索引期间可能缓存了空结果，智能模式后必须丢弃并重扫
                invalidateScans()
                refresh()
                discoverContainers()
            }
        }
        scheduleStampCheck()
        // 平台重建标签动作面板后关闭图标会恢复默认绘制，定时兜底重新套用悬停规则
        scheduleCloseIconPoll()
        // 编辑器 gutter 图标通过该入口打开当前面板的调试页
        LuckyDebugOpener.getInstance(project).attach(this)
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
        if (closeIconTrackingInstalled) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(closeIconHoverTracker)
            closeIconTrackingInstalled = false
        }
        LuckyDebugOpener.getInstance(project).detach(this)
        debugTabs.forEach { Disposer.dispose(it.panel) }
        debugTabs.clear()
        containerChips.values.forEach { it.dispose() }
        containerChips.clear()
        logPanel?.let { Disposer.dispose(it) }
        logPanel = null
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
        ensureSpinnerRunning()
    }

    /** 按 模块/外部库/jar → 分组 → 包 → 组件 → 方法 递归构建树节点。 */
    private fun addProjects(model: LuckyNavigatorModel) {
        for (projectNode in model.projects) {
            addContainer(projectNode, root)
        }
    }

    private fun addContainer(container: LuckyProjectNode, parent: DefaultMutableTreeNode) {
        val containerTreeNode = DefaultMutableTreeNode(NodeData.Project(container))
        // 子模块/子库在前，随后是本节点自己的分组
        for (child in container.children) {
            addContainer(child, containerTreeNode)
        }
        for (group in container.groups) {
            val groupTreeNode = DefaultMutableTreeNode(NodeData.Group(container.name, group))
            for (pkg in group.packages) {
                if (showPackageNames) {
                    val packageTreeNode =
                        DefaultMutableTreeNode(NodeData.Package(container.name, group.title, pkg))
                    addComponents(container.name, pkg.components, packageTreeNode)
                    groupTreeNode.add(packageTreeNode)
                } else {
                    // 关闭包名后组件直接挂在分组下，不再插入包节点
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
        is NodeData.Group -> "group:${data.project}/${data.group.title}"
        is NodeData.Package -> "package:${data.project}/${data.groupTitle}/${data.node.name}"
        is NodeData.Component -> "component:${data.project}/${data.node.qualifiedName}"
        is NodeData.Item -> "item:${data.node.name}"
        else -> null
    }

    // ---------------------------------------------------------------------------------------------
    // 交互
    // ---------------------------------------------------------------------------------------------

    private fun installMouseHandler() {
        val handler = object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                showNavigatorContextMenu(event)
            }

            override fun mouseReleased(event: MouseEvent) {
                showNavigatorContextMenu(event)
            }

            override fun mouseClicked(event: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return
                }
                // 行尾容器动作优先于节点选中/双击激活
                val containerHit = containerActionAt(event)
                if (containerHit != null) {
                    if (event.clickCount == 1) {
                        performContainerAction(containerHit)
                    }
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
        }
        tree.addMouseListener(handler)
    }

    /**
     * 导航树右键菜单：模块内的接口方法行提供「Debug Method」，
     * 与编辑器 gutter 小飞机打开同一个调试页；其它行不弹菜单。
     */
    private fun showNavigatorContextMenu(event: MouseEvent): Boolean {
        if (!event.isPopupTrigger) {
            return false
        }
        val path = tree.getPathForLocation(event.x, event.y) ?: return false
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return false
        val item = debugItemOf(path, node) ?: return false
        tree.requestFocusInWindow()
        tree.selectionPath = path
        val menu = JPopupMenu()
        val debugMenuItem = JMenuItem(DEBUG_METHOD_TOOLTIP, DEBUG_ICON)
        debugMenuItem.addActionListener { openDebug(item) }
        menu.add(debugMenuItem)
        menu.show(tree, event.x, event.y)
        return true
    }

    /** 树本体：行尾的容器动作图标画在树组件上，才能与项目行最右侧对齐。 */
    private inner class NavigatorTree(model: TreeModel) : JTree(model) {

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            paintRowActions(g)
        }

        /** 鼠标悬停在行尾容器动作图标上时展示提示，其余位置沿用行提示。 */
        override fun getToolTipText(event: MouseEvent): String? =
            containerActionAt(event)?.action?.tooltip
                ?: super.getToolTipText(event)
    }

    /** 行尾动作图标的点击结果。 */
    private data class ContainerActionHit(val moduleName: String, val action: ContainerAction)

    /** 容器状态/日志变化后合并重绘；监听回调可能来自后台线程。 */
    private fun scheduleContainerRepaint() {
        if (disposed || !containerRepaintPending.compareAndSet(false, true)) {
            return
        }
        ApplicationManager.getApplication().invokeLater({
            containerRepaintPending.set(false)
            if (!disposed) {
                tree.repaint()
                ensureSpinnerRunning()
                ensureBadgeAnimation()
                updateContainerBar()
            }
        }, ModalityState.nonModal())
    }

    /** 模块当前的容器状态；从未创建过容器时返回 null（按未启动展示）。 */
    private fun containerStateOf(moduleName: String): DebugContainerService.State? {
        val module = ModuleManager.getInstance(project).findModuleByName(moduleName) ?: return null
        return containerService.snapshot(module)?.state
    }

    // ---------------------------------------------------------------------------------------------
    // 容器栏
    // ---------------------------------------------------------------------------------------------

    /**
     * 后台探测所有可运行容器。
     *
     * 扫描注解索引必须离开 EDT；索引未就绪时探测返回 null，登记智能模式
     * 结束后重试，不能把「索引没建好」误当成「没有可运行容器」。
     */
    private fun discoverContainers() {
        if (disposed || project.isDisposed) {
            return
        }
        ReadAction.nonBlocking<List<DiscoveredContainer>?> {
            DebugContainerDiscovery.discover(project)
        }.finishOnUiThread(ModalityState.nonModal()) { containers ->
            if (disposed || project.isDisposed) {
                return@finishOnUiThread
            }
            if (containers == null) {
                DumbService.getInstance(project).runWhenSmart {
                    if (!disposed) {
                        discoverContainers()
                    }
                }
                return@finishOnUiThread
            }
            discoveredContainers = containers
            rebuildContainerBar()
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    /**
     * 重建容器栏条目。
     *
     * 条目来自两部分：后台发现的「可运行容器」与已经创建过容器的模块——
     * 运行过的容器即使主类暂时探测不到也保留条目，方便停止或重启。
     * 条目集合不变时不重建，避免容器日志通知触发组件重建。
     */
    private fun rebuildContainerBar() {
        if (disposed) {
            return
        }
        val statuses = containerService.statuses().associateBy { it.moduleName }
        val names = LinkedHashSet<String>()
        discoveredContainers.forEach { names += it.moduleName }
        statuses.keys.sortedWith(String.CASE_INSENSITIVE_ORDER).forEach { names += it }
        if (containerChips.keys.toList() == names.toList()) {
            updateContainerBar()
            return
        }
        containerChips.values.forEach { it.dispose() }
        containerBarContent.removeAll()
        containerChips.clear()
        for (name in names) {
            val chip = ContainerChip(name)
            containerChips[name] = chip
            containerBarContent.add(chip)
        }
        val visible = names.isNotEmpty()
        if (containerBar.isVisible != visible) {
            containerBar.isVisible = visible
            containerBar.revalidate()
            containerBar.repaint()
        }
        containerBarContent.revalidate()
        containerBarContent.repaint()
        updateContainerBar()
    }

    /** 按最新状态更新容器栏条目；容器日志每追加一行都会走到这里。 */
    private fun updateContainerBar() {
        if (disposed) {
            return
        }
        val statuses = containerService.statuses().associateBy { it.moduleName }
        val discovered = discoveredContainers.associateBy { it.moduleName }
        if (statuses.keys.any { it !in containerChips } || discovered.keys.any { it !in containerChips }) {
            rebuildContainerBar()
            return
        }
        for ((name, chip) in containerChips) {
            chip.update(statuses[name], discovered[name]?.mainClass)
        }
        updateContainerSelection()
    }

    /**
     * 同步容器条目的选中态。
     *
     * 选中项跟随当前调试页的目标方法：当前执行的方法属于哪个模块，
     * 哪个模块的条目就使用选中背景框；容器日志页、没有调试页（包括最后一个
     * 标签已关闭但平台未回调选中变化）时全部恢复默认。
     */
    private fun updateContainerSelection() {
        if (disposed) {
            return
        }
        // 标签栏为空时不能沿用平台的选中索引，否则会残留上一个模块的选中态
        val selectedInfo = debugPageTabs.selectedInfo
        val selectedModule = if (debugTabs.isEmpty() || selectedInfo == null) {
            null
        } else {
            debugTabOf(selectedInfo)?.panel?.target?.moduleName
        }
        for ((name, chip) in containerChips) {
            chip.setSelected(name == selectedModule)
        }
    }

    /** 当前行该展示的容器动作。 */
    private fun containerActionsOf(projectNode: LuckyProjectNode): List<ContainerAction> =
        if (projectNode.kind == LuckyProjectKind.MODULE) {
            containerActionsFor(containerStateOf(projectNode.name))
        } else {
            emptyList()
        }

    /** 鼠标位置命中的容器动作；非项目行或未命中图标时返回 null。 */
    private fun containerActionAt(event: MouseEvent): ContainerActionHit? {
        // 图标位于文字右侧的空白区，不能用 getPathForLocation（受文字宽度限制）
        val row = tree.getClosestRowForLocation(event.x, event.y)
        if (row < 0) {
            return null
        }
        val path = tree.getPathForRow(row) ?: return null
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return null
        val projectNode = (node.userObject as? NodeData.Project)?.node ?: return null
        val actions = containerActionsOf(projectNode)
        if (actions.isEmpty()) {
            return null
        }
        val bounds = tree.getRowBounds(row) ?: return null
        val index = rowActionBounds(row, node, bounds, actions.size)
            .indexOfFirst { it.contains(event.x, event.y) }
        return if (index >= 0) ContainerActionHit(projectNode.name, actions[index]) else null
    }

    /**
     * 可打开调试页的接口方法；右键菜单入口使用。
     *
     * 只对项目模块内的接口方法提供入口：外部库与 jar 没有所属模块，无法构建
     * 调试 classpath。源码失效时不提供，等树重建后自动恢复。
     */
    private fun debugItemOf(path: TreePath, node: DefaultMutableTreeNode): LuckyItemNode.Api? {
        val item = (node.userObject as? NodeData.Item)?.node as? LuckyItemNode.Api ?: return null
        if (item.element !is PsiMethod || !item.element.isValid) {
            return null
        }
        val owner = projectNodeFor(path) ?: return null
        if (owner.kind != LuckyProjectKind.MODULE) {
            return null
        }
        return item
    }

    /** 绘制项目行行尾的容器动作图标；接口方法不再在行首绘制图标。 */
    private fun paintRowActions(g: Graphics) {
        val clip = g.clipBounds ?: return
        for (row in 0 until tree.rowCount) {
            val bounds = tree.getRowBounds(row) ?: continue
            if (!bounds.intersects(clip)) {
                continue
            }
            val node = tree.getPathForRow(row)?.lastPathComponent as? DefaultMutableTreeNode ?: continue
            val projectNode = (node.userObject as? NodeData.Project)?.node ?: continue
            paintContainerActions(g, row, node, bounds, projectNode)
        }
    }

    /** 项目行的容器动作图标：纵向居中，贴住选中背景右端。 */
    private fun paintContainerActions(
        g: Graphics,
        row: Int,
        node: DefaultMutableTreeNode,
        bounds: Rectangle,
        projectNode: LuckyProjectNode
    ) {
        val actions = containerActionsOf(projectNode)
        if (actions.isEmpty()) {
            // 过渡态没有可点的动作，改为在同一个位置画转圈
            if (containerBusyOf(projectNode)) {
                val rect = rowActionBounds(row, node, bounds, 1).firstOrNull() ?: return
                paintSelectionExtension(g, row, bounds, listOf(rect))
                paintSpinner(g, rect)
            }
            return
        }
        val iconBounds = rowActionBounds(row, node, bounds, actions.size)
        paintSelectionExtension(g, row, bounds, iconBounds)
        for ((index, action) in actions.withIndex()) {
            val rect = iconBounds.getOrNull(index) ?: continue
            paintIconCentered(g, iconOf(action), rect)
        }
    }

    /** 图标按槽位居中绘制；插件图标与平台图标尺寸不一致时也不会偏上。 */
    private fun paintIconCentered(g: Graphics, icon: Icon, rect: Rectangle) {
        icon.paintIcon(
            tree,
            g,
            rect.x + (rect.width - icon.iconWidth) / 2,
            rect.y + (rect.height - icon.iconHeight) / 2
        )
    }

    /** 在动作图标的位置画一帧转圈，图标按槽位居中。 */
    private fun paintSpinner(g: Graphics, rect: Rectangle) {
        paintIconCentered(g, SPINNER_FRAMES[spinnerFrame % SPINNER_FRAMES.size], rect)
    }

    /**
     * 过渡态的行尾转圈动画：只有存在启动/重启/停止中的项目时才持续重绘，
     * 全部结束后自动停帧，避免常驻定时器。
     */
    private fun ensureSpinnerRunning() {
        if (disposed || spinnerRunning || !hasBusyProject()) {
            return
        }
        spinnerRunning = true
        scheduleSpinnerTick()
    }

    private fun scheduleSpinnerTick() {
        alarm.addRequest({
            if (disposed) {
                spinnerRunning = false
                return@addRequest
            }
            if (!hasBusyProject()) {
                spinnerRunning = false
                spinnerFrame = 0
                tree.repaint()
                return@addRequest
            }
            spinnerFrame = (spinnerFrame + 1) % SPINNER_FRAMES.size
            tree.repaint()
            scheduleSpinnerTick()
        }, SPINNER_FRAME_MS)
    }

    /** 树中是否有项目正处于容器过渡态。 */
    private fun hasBusyProject(): Boolean {
        for (row in 0 until tree.rowCount) {
            val node = tree.getPathForRow(row)?.lastPathComponent as? DefaultMutableTreeNode ?: continue
            val projectNode = (node.userObject as? NodeData.Project)?.node ?: continue
            if (containerBusyOf(projectNode)) {
                return true
            }
        }
        return false
    }

    /** 项目是否处于容器过渡态：此时行尾不展示动作，改画转圈。 */
    private fun containerBusyOf(projectNode: LuckyProjectNode): Boolean =
        projectNode.kind == LuckyProjectKind.MODULE && containerBusy(containerStateOf(projectNode.name))

    /**
     * 项目图标状态方块的呼吸动画：只有存在启动/重启中的容器时才持续重绘，
     * 全部结束后自动停帧，避免常驻定时器。
     */
    private fun ensureBadgeAnimation() {
        if (disposed || badgeAnimationRunning || !hasStartingContainer()) {
            return
        }
        badgeAnimationRunning = true
        scheduleBadgeAnimationTick()
    }

    private fun scheduleBadgeAnimationTick() {
        alarm.addRequest({
            if (disposed) {
                badgeAnimationRunning = false
                return@addRequest
            }
            if (!hasStartingContainer()) {
                badgeAnimationRunning = false
                tree.repaint()
                return@addRequest
            }
            tree.repaint()
            scheduleBadgeAnimationTick()
        }, BADGE_PULSE_INTERVAL_MS)
    }

    /** 是否有容器处于启动/重启中；只读 volatile 状态，可在 EDT 上频繁调用。 */
    private fun hasStartingContainer(): Boolean = containerService.statuses().any {
        it.state == DebugContainerService.State.STARTING ||
            it.state == DebugContainerService.State.RESTARTING
    }

    /**
     * 沿树路径找到接口所属的项目节点：模型构建时已按模块/库分组，
     * 不需要再反查 PSI 的模块归属（在 EDT 上会触发文件索引的慢操作断言）。
     */
    private fun projectNodeFor(path: TreePath): LuckyProjectNode? {
        for (element in path.path) {
            val data = (element as? DefaultMutableTreeNode)?.userObject
            if (data is NodeData.Project) {
                return data.node
            }
        }
        return null
    }

    /**
     * 树默认只把选中背景画到文字宽度，图标可能落在蓝色选区之外。
     * 这里把背景从行末补到图标右边缘，让选中行视觉连续。
     */
    private fun paintSelectionExtension(
        g: Graphics,
        row: Int,
        rowBounds: Rectangle,
        iconBounds: List<Rectangle>
    ) {
        if (iconBounds.isEmpty() || !tree.selectionModel.isRowSelected(row)) {
            return
        }
        val start = rowBounds.x + rowBounds.width
        val visibleRight = tree.visibleRect.let { it.x + it.width }
        val end = minOf(
            iconBounds.last().x + iconBounds.last().width + actionRightGap(),
            visibleRight
        )
        if (end <= start) {
            return
        }
        g.color = UIUtil.getTreeSelectionBackground(tree.hasFocus())
        g.fillRect(start, rowBounds.y, end - start, rowBounds.height)
    }

    /**
     * 行尾图标距视口右缘的距离：让开竖直滚动条，否则图标会被滚动条遮住。
     * 无论滚动条是否显示都按同一宽度留白，图标位置不会随内容高度跳动。
     */
    private fun actionRightGap(): Int {
        val scrollbarWidth = treeScroll.verticalScrollBar.preferredSize.width
        return (if (scrollbarWidth > 0) scrollbarWidth else JBUI.scale(DEFAULT_SCROLLBAR_WIDTH)) +
            JBUI.scale(ACTION_SCROLLBAR_GAP)
    }

    /**
     * 行尾动作图标的矩形：右对齐到可视区右端，并让开竖直滚动条；
     * 面板太窄、右对齐会压住行内容时退回内容之后。
     * 绘制与命中测试共用同一套坐标，保证点击区域与视觉一致。
     */
    private fun rowActionBounds(
        row: Int,
        node: DefaultMutableTreeNode,
        rowBounds: Rectangle,
        count: Int
    ): List<Rectangle> {
        val visible = tree.visibleRect
        if (visible.isEmpty || count <= 0) {
            return emptyList()
        }
        val size = JBUI.scale(ACTION_ICON_SIZE)
        val gap = JBUI.scale(ACTION_ICON_GAP)
        val total = count * size + (count - 1) * gap
        val rightAligned = visible.x + visible.width - actionRightGap() - total
        val contentRight = rowContentRight(row, node)
        val startX = if (contentRight == null) {
            rightAligned
        } else {
            maxOf(rightAligned, contentRight + JBUI.scale(ACTION_CONTENT_GAP))
        }
        val top = rowBounds.y + (rowBounds.height - size) / 2
        return (0 until count).map { index ->
            Rectangle(startX + index * (size + gap), top, size, size)
        }
    }

    /**
     * 行渲染内容的右边缘：行起点 + 渲染器首选宽度（图标 + 文本 + 计数）。
     * 树行宽在部分 LAF 下会被拉满视口，不能用来定位动作图标。
     */
    private fun rowContentRight(row: Int, node: DefaultMutableTreeNode): Int? {
        val path = tree.getPathForRow(row) ?: return null
        val label = tree.getPathBounds(path) ?: return null
        val component = renderer.getTreeCellRendererComponent(
            tree,
            node,
            tree.selectionModel.isRowSelected(row),
            tree.isExpanded(row),
            tree.model.isLeaf(node),
            row,
            tree.hasFocus()
        )
        return label.x + component.preferredSize.width
    }

    private fun iconOf(action: ContainerAction): Icon = when (action) {
        ContainerAction.START -> AllIcons.Actions.Execute
        ContainerAction.RESTART -> AllIcons.Actions.Restart
        ContainerAction.STOP -> AllIcons.Actions.Suspend
        ContainerAction.LOG -> AllIcons.Debugger.Console
    }

    /**
     * 执行容器动作。
     *
     * 启动/重启/停止会阻塞（编译、等待子进程退出），统一丢到后台线程；
     * 查看日志只操作界面，直接在当前线程打开日志页。
     */
    private fun performContainerModuleAction(moduleName: String, action: ContainerAction) {
        if (action == ContainerAction.LOG) {
            showContainerLogs(moduleName)
            return
        }
        val module = ModuleManager.getInstance(project).findModuleByName(moduleName) ?: return
        AppExecutorUtil.getAppExecutorService().execute {
            when (action) {
                ContainerAction.START,
                ContainerAction.RESTART -> runCatching {
                    containerService.start(
                        module,
                        programArgs = DebugContainerService.programArgsFor(
                            LuckyHttpClientSettings.getInstance().isDebugWebServerEnabled
                        )
                    )
                }

                ContainerAction.STOP -> runCatching { containerService.stop(module) }

                // 已在上面直接处理，不会走到后台执行器
                ContainerAction.LOG -> Unit
            }
        }
    }

    /** 树行容器动作入口：与容器栏共用同一套执行逻辑。 */
    private fun performContainerAction(hit: ContainerActionHit) =
        performContainerModuleAction(hit.moduleName, hit.action)

    /**
     * 电源按钮：容器栏唯一的操作入口。
     *
     * 图标颜色表达容器状态：灰=未启动、黄=启动/重启/停止中（呼吸动效）、
     * 绿=运行中、红=启动失败或熔断。左键在「启动」与「关闭」之间切换，
     * 右键按状态弹出可用操作菜单。
     */
    private inner class PowerButton(private val moduleName: String) : JButton() {

        private val pulseTimer = Timer(POWER_PULSE_INTERVAL_MS) {
            pulseFrame++
            repaint()
        }

        private var state: DebugContainerService.State? = null
        private var stateInitialized = false
        private var pulseFrame = 0

        init {
            isBorderPainted = false
            isContentAreaFilled = false
            isFocusPainted = false
            isOpaque = false
            margin = java.awt.Insets(0, 0, 0, 0)
            preferredSize = JBUI.size(CONTAINER_POWER_SIZE, CONTAINER_POWER_SIZE)
            minimumSize = preferredSize
            maximumSize = preferredSize
            addMouseListener(
                object : MouseAdapter() {
                    override fun mousePressed(event: MouseEvent) {
                        showContainerContextMenu(moduleName, state, event)
                    }

                    override fun mouseReleased(event: MouseEvent) {
                        if (showContainerContextMenu(moduleName, state, event)) {
                            return
                        }
                        if (SwingUtilities.isLeftMouseButton(event) && contains(event.point)) {
                            containerPowerAction(state)?.let {
                                performContainerModuleAction(moduleName, it)
                            }
                        }
                    }
                }
            )
        }

        fun update(newState: DebugContainerService.State?) {
            if (stateInitialized && state == newState) {
                return
            }
            stateInitialized = true
            state = newState
            toolTipText = when (containerPowerAction(newState)) {
                ContainerAction.START -> ContainerAction.START.tooltip
                ContainerAction.STOP -> ContainerAction.STOP.tooltip
                else -> "Container is stopping"
            }
            if (containerBusy(newState)) {
                pulseTimer.start()
            } else {
                pulseTimer.stop()
                pulseFrame = 0
            }
            repaint()
        }

        /** 容器栏重建或面板销毁时停掉呼吸动画，避免定时器继续持有按钮。 */
        fun stopAnimation() {
            pulseTimer.stop()
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
                val base = containerPowerColor(state)
                g2.color = if (containerBusy(state)) {
                    // 呼吸动效：透明度在 35% 到 100% 之间往复
                    val pulse = ((sin(pulseFrame * POWER_PULSE_SPEED) + 1.0) / 2.0).toFloat()
                    Color(base.red, base.green, base.blue, (255 * (0.35f + 0.65f * pulse)).toInt())
                } else {
                    base
                }
                g2.stroke = BasicStroke(JBUIScale.scale(1.6f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                val diameter = JBUI.scale(POWER_ARC_SIZE)
                val left = (width - diameter) / 2
                val top = (height - diameter) / 2
                // 电源符号：顶部留缺口的圆环，加一根穿过缺口的竖线
                g2.drawArc(left, top, diameter, diameter, 135, 270)
                g2.drawLine(width / 2, top - JBUI.scale(1), width / 2, top + diameter / 2 - JBUI.scale(1))
            } finally {
                g2.dispose()
            }
        }

    }

    /**
     * 容器条目的右键菜单：电源按钮与模块名共用。
     *
     * 按当前状态给出可用操作，并在事件来源组件上弹出；已弹出时返回 true，
     * 调用方据此避免同一次点击再触发左键动作。
     */
    private fun showContainerContextMenu(
        moduleName: String,
        state: DebugContainerService.State?,
        event: MouseEvent
    ): Boolean {
        if (!event.isPopupTrigger) {
            return false
        }
        val actions = containerContextMenuActions(state)
        if (actions.isEmpty()) {
            return true
        }
        val menu = JPopupMenu()
        for (action in actions) {
            val item = JMenuItem(action.tooltip, iconOf(action))
            item.addActionListener { performContainerModuleAction(moduleName, action) }
            menu.add(item)
        }
        menu.show(event.component, event.x, event.y)
        return true
    }

    /**
     * 容器栏中的单个容器：电源按钮 + 模块名。
     *
     * 条目自带圆角背景框（与编辑器标签同形）；当前调试页所属模块的条目
     * 使用选中背景与更亮的描边。组件常驻，状态变化只更新电源按钮的
     * 颜色/动效/提示；容器日志每追加一行都会触发一次刷新，不能在这里重建组件。
     */
    private inner class ContainerChip(private val moduleName: String) : JPanel() {

        private val powerButton = PowerButton(moduleName)

        private val nameLabel = JLabel(moduleName).apply {
            font = UIUtil.getLabelFont()
            // 模块名过长时省略，完整名称仍可在条目提示中看到
            if (moduleName.length > CONTAINER_NAME_MAX_CHARS) {
                text = moduleName.take(CONTAINER_NAME_MAX_CHARS - 3) + "..."
            }
        }

        private var state: DebugContainerService.State? = null
        private var stateInitialized = false
        private var mainClass: String? = null
        private var failure: String? = null

        /** 当前调试页是否属于该模块；选中时使用高亮背景框。 */
        private var selected = false

        init {
            isOpaque = false
            toolTipText = moduleName
            border = JBUI.Borders.empty(CONTAINER_CHIP_PADDING_Y, CONTAINER_CHIP_PADDING_X)
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(powerButton)
            add(Box.createHorizontalStrut(JBUI.scale(CONTAINER_ICON_GAP)))
            add(nameLabel)
            // 模块名与电源按钮共用右键菜单，条目上任意位置右键都能操作容器
            nameLabel.addMouseListener(
                object : MouseAdapter() {
                    override fun mousePressed(event: MouseEvent) {
                        showContainerContextMenu(moduleName, state, event)
                    }

                    override fun mouseReleased(event: MouseEvent) {
                        showContainerContextMenu(moduleName, state, event)
                    }
                }
            )
            update(null, null)
        }

        /** 切换选中状态：只有当前调试页的模块高亮，其余恢复默认背景框。 */
        fun setSelected(value: Boolean) {
            if (selected == value) {
                return
            }
            selected = value
            repaint()
        }

        /**
         * 圆角背景框：未选中与编辑器未选中标签同形（只有底色），
         * 选中时换成选中标签的底色并补一圈描边，尺寸随字体与内边距自适应。
         */
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (width <= 0 || height <= 0) {
                return
            }
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
                val arc = JBUI.scale(CONTAINER_CHIP_ARC).toFloat()
                // 半像素偏移让 1px 描边落在整像素上，圆角边缘不发虚
                val shape = RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, arc, arc)
                g2.color = if (selected) CONTAINER_CHIP_SELECTED_BACKGROUND else CONTAINER_CHIP_BACKGROUND
                g2.fill(shape)
                if (selected) {
                    g2.color = CONTAINER_CHIP_SELECTED_BORDER
                    g2.stroke = BasicStroke(1f)
                    g2.draw(shape)
                }
            } finally {
                g2.dispose()
            }
        }

        fun update(status: DebugContainerService.ContainerStatus?, discoveredMainClass: String?) {
            val newState = status?.state
            val newMainClass = status?.mainClass ?: discoveredMainClass
            val newFailure = status?.failure
            if (!stateInitialized || state != newState) {
                stateInitialized = true
                state = newState
                powerButton.update(newState)
            }
            if (mainClass != newMainClass || failure != newFailure) {
                mainClass = newMainClass
                failure = newFailure
                toolTipText = buildString {
                    append(moduleName)
                    newMainClass?.let { append(" — ").append(it) }
                    newFailure?.let { append(" — ").append(it) }
                }
            }
        }

        /** 容器栏重建时停止电源按钮的呼吸动画。 */
        fun dispose() {
            powerButton.stopAnimation()
        }
    }

    /** 打开调试面板所需的当前目标与同组件候选方法。 */
    private data class DebugPanelRequest(val target: DebugTarget, val siblings: List<DebugTarget>)

    /** 导航树右键菜单入口：在后台 read action 中解析目标，再回到 EDT 打开调试面板。 */
    private fun openDebug(item: LuckyItemNode.Api) {
        val method = item.element as? PsiMethod ?: return
        openDebugMethod(method)
    }

    /** 编辑器 gutter 图标入口：与侧边栏共用同一套目标解析与分页规则。 */
    internal fun openDebugMethod(method: PsiMethod) {
        if (!method.isValid) {
            refresh()
            return
        }
        val model = lastModel
        ReadAction.nonBlocking<DebugPanelRequest?> {
            // 找模块要读 workspace 文件索引，同样不能放在 EDT 上
            val module = ModuleUtilCore.findModuleForPsiElement(method)
            if (module == null || disposed || project.isDisposed || !method.isValid) {
                null
            } else {
                val target = DebugTargetFactory.of(module, method) ?: return@nonBlocking null
                val siblings = siblingApiMethods(model, method)
                    .filter { it.isValid }
                    .mapNotNull { DebugTargetFactory.of(module, it) }
                    .distinct()
                DebugPanelRequest(target, siblings)
            }
        }.finishOnUiThread(ModalityState.nonModal()) { request ->
            if (request != null && !disposed && !project.isDisposed) {
                showDebugPanel(request.target, request.siblings)
            }
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    /**
     * 同一组件下的接口方法，供调试面板的目标选择器使用。
     *
     * 以导航树模型为准，与侧边栏保持同一份清单；模型尚未构建（直接从编辑器
     * gutter 打开）时回退为同类的实例方法，面板不会退化成只有当前方法。
     */
    private fun siblingApiMethods(model: LuckyNavigatorModel, method: PsiMethod): List<PsiMethod> {
        val fromModel = siblingApiMethods(model, method.containingClass?.qualifiedName)
        if (fromModel.isNotEmpty()) {
            return fromModel
        }
        val owner = method.containingClass ?: return emptyList()
        return owner.methods
            .filterNot { it.isConstructor }
            .filterNot { it.hasModifierProperty(PsiModifier.STATIC) }
    }

    /** 在导航树模型中查找指定组件的方法；模型里没有该类时返回空列表。 */
    private fun siblingApiMethods(model: LuckyNavigatorModel, className: String?): List<PsiMethod> {
        if (className == null) {
            return emptyList()
        }
        fun find(node: LuckyProjectNode): LuckyComponentNode? {
            for (group in node.groups) {
                for (pkg in group.packages) {
                    pkg.components.firstOrNull { it.qualifiedName == className }?.let { return it }
                }
            }
            for (child in node.children) {
                find(child)?.let { return it }
            }
            return null
        }
        var component: LuckyComponentNode? = null
        for (projectNode in model.projects) {
            component = find(projectNode)
            if (component != null) {
                break
            }
        }
        val owner = component ?: return emptyList()
        return owner.items.filterIsInstance<LuckyItemNode.Api>()
            .mapNotNull { it.element as? PsiMethod }
    }

    /**
     * 在标签栏中打开接口调试页。
     *
     * 每个目标一个独立标签页：同目标重复打开时选中现有页；不同目标各自保留
     * 参数、结果与 Logs。[siblings] 是同一组件下的接口方法，供页内选择器切换。
     */
    private fun showDebugPanel(target: DebugTarget, siblings: List<DebugTarget> = emptyList()) {
        val existing = debugTabs.firstOrNull { it.panel.target == target }
        val existingInfo = existing?.info
        if (existing != null && existingInfo != null) {
            debugPageTabs.select(existingInfo, true)
            selectTopPage(TopPage.DEBUG)
            existing.panel.component.requestFocusInWindow()
            return
        }
        var tab: DebugTab? = null
        val panel = DebugNavigatorPanel(
            project = project,
            target = target,
            siblings = siblings,
            onSwitchTarget = { next -> tab?.let { switchDebugTabTarget(it, next, siblings) } },
            onClose = { tab?.let { closeDebugTab(it) } },
            // 标题与关闭由标签页头承担，面板内部不再重复一行
            showTitleRow = false
        )
        val created = DebugTab(panel)
        tab = created
        val info = tabInfo(panel.component, panel.tabTitle, AllIcons.Nodes.Method, target.displayName) {
            closeDebugTab(created)
        }
        created.info = info
        debugTabs.add(created)
        debugPageTabs.addTab(info)
        debugPageTabs.select(info, true)
        updateDebugCards()
        selectTopPage(TopPage.DEBUG)
        refreshTabActions()
        scheduleMoreTabsUpdate()
        panel.component.requestFocusInWindow()
    }

    /** 页内切换目标：在原标签页位置重建面板，其它标签页不受影响。 */
    private fun switchDebugTabTarget(tab: DebugTab, next: DebugTarget, siblings: List<DebugTarget>) {
        val oldInfo = tab.info ?: return
        val index = debugPageTabs.getIndexOf(oldInfo)
        if (index < 0) {
            return
        }
        var replacement: DebugTab? = null
        val panel = DebugNavigatorPanel(
            project = project,
            target = next,
            siblings = siblings,
            onSwitchTarget = { other -> replacement?.let { switchDebugTabTarget(it, other, siblings) } },
            onClose = { replacement?.let { closeDebugTab(it) } },
            showTitleRow = false
        )
        val created = DebugTab(panel)
        replacement = created
        val info = tabInfo(panel.component, panel.tabTitle, AllIcons.Nodes.Method, next.displayName) {
            closeDebugTab(created)
        }
        created.info = info
        debugTabs.remove(tab)
        debugTabs.add(created)
        debugPageTabs.removeTab(oldInfo)
        debugPageTabs.addTab(info, index.coerceAtMost(debugPageTabs.tabCount))
        debugPageTabs.select(info, true)
        selectTopPage(TopPage.DEBUG)
        scheduleMoreTabsUpdate()
        refreshTabActions()
        Disposer.dispose(tab.panel)
        panel.component.requestFocusInWindow()
    }

    /**
     * 在标签栏中打开容器日志页。
     *
     * 日志按模块一页：重复打开同一个模块时选中现有页，切换模块时替换旧页。
     */
    private fun showContainerLogs(moduleName: String) {
        if (disposed || project.isDisposed) {
            return
        }
        val existing = logPanel
        if (existing != null && existing.moduleName == moduleName) {
            logPanelInfo?.let {
                debugPageTabs.select(it, true)
                selectTopPage(TopPage.DEBUG)
            }
            return
        }
        if (existing != null) {
            closeContainerLogs()
        }
        val panel = ContainerLogPanel(project, moduleName, onClose = ::closeContainerLogs, embedded = true)
        logPanel = panel
        val title = "Logs — $moduleName"
        val info = tabInfo(panel.component, title, null, "Container logs of module $moduleName") {
            closeContainerLogs()
        }
        logPanelInfo = info
        debugPageTabs.addTab(info)
        debugPageTabs.select(info, true)
        updateDebugCards()
        selectTopPage(TopPage.DEBUG)
        refreshTabActions()
        scheduleMoreTabsUpdate()
        panel.component.requestFocusInWindow()
    }

    /** 关闭调试页；导航页标签始终保留，其它标签页不受影响。 */
    private fun closeDebugTab(tab: DebugTab) {
        val info = tab.info ?: return
        if (!debugTabs.remove(tab)) {
            return
        }
        debugPageTabs.removeTab(info)
        Disposer.dispose(tab.panel)
        updateDebugCards()
        scheduleMoreTabsUpdate()
    }

    /** 关闭容器日志页。 */
    private fun closeContainerLogs() {
        val current = logPanel ?: return
        logPanel = null
        val info = logPanelInfo
        logPanelInfo = null
        if (info != null) {
            debugPageTabs.removeTab(info)
        }
        Disposer.dispose(current)
        updateDebugCards()
        scheduleMoreTabsUpdate()
    }

    /** 有调试页时切到子标签栏卡片，全部关闭后显示占位提示。 */
    private fun updateDebugCards() {
        val card = if (debugPageTabs.tabCount == 0) DEBUG_EMPTY_CARD else DEBUG_TABS_CARD
        (debugCards.layout as CardLayout).show(debugCards, card)
        debugCards.revalidate()
        debugCards.repaint()
        // 标签增删的统一出口：最后一个调试页关闭后清掉容器条目的残留选中
        updateContainerSelection()
    }

    /** 切换顶层页签：更新内容卡片、按钮选中态，并把焦点交给新页内容。 */
    private fun selectTopPage(page: TopPage) {
        val changed = currentTopPage != page
        currentTopPage = page
        (topPages.layout as CardLayout).show(topPages, page.card)
        navigatorButton.selected = page == TopPage.NAVIGATOR
        debugButton.selected = page == TopPage.DEBUG
        when (page) {
            TopPage.NAVIGATOR -> searchField.requestFocusInWindow()
            TopPage.DEBUG -> {
                debugPageTabs.selectedInfo?.component?.requestFocusInWindow()
                // 调试页刚显示时子标签栏才有真实宽度，布局后重新判断溢出
                scheduleMoreTabsUpdate()
            }
        }
        // 展开/折叠只属于导航页：切页后立即重建标题栏动作，不等 toolbar 周期刷新
        if (changed) {
            refreshTitleActions()
        }
    }

    /** 标题栏动作：展开/折叠只在导航页挂载，其余动作两个页面都保留。 */
    private fun refreshTitleActions() {
        val actions = ArrayList<AnAction>(5)
        actions += RefreshAction()
        actions += JumpToSourceAction()
        if (currentTopPage == TopPage.NAVIGATOR) {
            actions += ExpandAllNavigatorAction()
            actions += CollapseAllNavigatorAction()
        }
        actions += SettingsActionGroup()
        toolWindow.setTitleActions(actions)
    }

    /** 顶层两个固定页签的卡片名。 */
    private enum class TopPage(val card: String) {
        NAVIGATOR(NAVIGATOR_CARD),
        DEBUG(DEBUG_CARD)
    }

    /**
     * 标签总宽超过可用宽度时显示「隐藏标签」按钮。
     *
     * 判断时先扣除按钮自身宽度，避免按钮出现后把最后一个标签挤掉、
     * 下一轮又判定不溢出而产生闪烁。
     */
    private fun updateMoreTabsButton(
        tabs: JBTabs,
        host: JPanel,
        button: JButton,
        buttonHost: JPanel
    ) {
        if (tabs.tabCount == 0 || host.width <= 0) {
            buttonHost.isVisible = false
            return
        }
        var needed = 0
        for (index in 0 until tabs.tabCount) {
            needed += tabs.getTabLabel(tabs.getTabAt(index)).preferredSize.width
        }
        val overflow = needed > host.width - button.preferredSize.width
        if (buttonHost.isVisible != overflow) {
            buttonHost.isVisible = overflow
            host.revalidate()
            host.repaint()
        }
    }

    /** 布局完成后再判断溢出：标签增删时宽度还未分配。 */
    private fun scheduleMoreTabsUpdate() {
        SwingUtilities.invokeLater {
            if (!disposed) {
                updateMoreTabsButton(debugPageTabs, debugPageTabsHost, debugMoreTabsButton, debugMoreTabsHost)
                refreshTabActions()
            }
        }
    }

    /**
     * 立即刷新标签内动作按钮。
     *
     * 关闭图标的显隐由动作的 update 回调根据鼠标位置决定，这里强制平台
     * 重新执行一次 update，让新加/重建的标签立即进入正确状态。
     */
    private fun refreshTabActions() {
        val tabs = debugPageTabs as? JBTabsImpl ?: return
        tabs.updateTabActions(true)
    }

    /** 安装关闭图标的悬停跟踪；无图形环境的测试不需要鼠标事件。 */
    private fun installCloseIconTracking() {
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(
                closeIconHoverTracker,
                AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
            )
            closeIconTrackingInstalled = true
        } catch (ignored: HeadlessException) {
            // headless 测试环境没有 Toolkit
        }
    }

    /**
     * 合并刷新关闭图标，放到事件队列末尾执行。
     *
     * 图标由关闭动作的 update 按鼠标位置实时决定；选择变化、标签移动和
     * 平台重建动作面板后都要重新触发 update，否则图标会停留在旧状态。
     */
    private fun scheduleTabCloseIconRefresh() {
        if (closeIconRefreshPending || disposed) {
            return
        }
        closeIconRefreshPending = true
        SwingUtilities.invokeLater {
            closeIconRefreshPending = false
            if (!disposed) {
                refreshTabActions()
            }
        }
    }

    /**
     * 关闭图标的兜底轮询。
     *
     * 平台重建动作面板的时机不完全由我们的代码控制（选中、动作刷新、外观
     * 变化都可能触发），轮询能保证即使重建发生在最后一次鼠标事件之后，
     * 关闭图标也会恢复为未悬停不绘制。
     */
    private fun scheduleCloseIconPoll() {
        alarm.addRequest({
            if (!disposed) {
                if (debugPageTabs.tabCount > 0) {
                    refreshTabActions()
                }
                scheduleCloseIconPoll()
            }
        }, CLOSE_ICON_POLL_INTERVAL_MS)
    }

    /** 指针是否位于 [info] 对应标签内；标签尚未布局或指针不可用时返回 false。 */
    private fun isPointerOverTab(info: TabInfo): Boolean {
        if (disposed) {
            return false
        }
        val tabs = debugPageTabs as? JBTabsImpl ?: return false
        val label = tabs.getTabLabel(info) ?: return false
        if (!tabs.component.isShowing || !label.isShowing) {
            return false
        }
        val pointer = MouseInfo.getPointerInfo()?.location ?: return false
        val local = Point(pointer)
        SwingUtilities.convertPointFromScreen(local, label)
        return local.x >= 0 && local.y >= 0 && local.x < label.width && local.y < label.height
    }

    /** 弹出调试子标签栏中被隐藏的标签供选择。 */
    private fun showHiddenTabsPopup() {
        val popup = JPopupMenu()
        for (index in 0 until debugPageTabs.tabCount) {
            val info = debugPageTabs.getTabAt(index)
            val item = JMenuItem(info.text, info.icon)
            item.addActionListener { debugPageTabs.select(info, true) }
            popup.add(item)
        }
        popup.show(debugMoreTabsButton, 0, debugMoreTabsButton.height)
    }

    // ---------------------------------------------------------------------------------------------
    // 调试子标签右键菜单
    // ---------------------------------------------------------------------------------------------

    /** 右键标签时弹出菜单；非右键触发时直接返回。 */
    private fun maybeShowDebugTabPopup(event: MouseEvent) {
        if (!event.isPopupTrigger || disposed) {
            return
        }
        val info = (debugPageTabs as? JBTabsImpl)?.findInfo(event) ?: return
        showDebugTabPopup(info, event.component, event.x, event.y)
    }

    /**
     * 调试子标签的右键菜单。
     *
     * 关闭系列沿用平台编辑器标签的菜单命名；调试页没有编辑器式的“修改”状态，
     * 全部页都视为未修改，因此 `Close Unmodified Tabs` 与 `Close All Tabs` 等价。
     */
    private fun showDebugTabPopup(info: TabInfo, component: Component, x: Int, y: Int) {
        val index = debugPageTabs.getIndexOf(info)
        if (index < 0) {
            return
        }
        val target = debugTabOf(info)?.panel?.target
        val infos = (0 until debugPageTabs.tabCount).map { debugPageTabs.getTabAt(it) }
        val menu = JPopupMenu()

        menu.add(debugTabMenuItem("Close", KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.CTRL_DOWN_MASK)) {
            closeDebugPage(info)
        })
        menu.add(debugTabMenuItem("Close Other Tabs", enabled = infos.size > 1) {
            closeDebugPages(infos.filter { it != info })
        })
        menu.add(debugTabMenuItem("Close All Tabs") { closeDebugPages(infos) })
        menu.add(debugTabMenuItem("Close Unmodified Tabs") { closeDebugPages(infos) })
        menu.add(
            debugTabMenuItem("Close Tabs to the Left", enabled = index > 0) {
                closeDebugPages(infos.take(index))
            }
        )
        menu.add(
            debugTabMenuItem("Close Tabs to the Right", enabled = index < infos.size - 1) {
                closeDebugPages(infos.drop(index + 1))
            }
        )
        menu.addSeparator()
        menu.add(debugTabMenuItem("Copy Path/Reference...") { copyDebugReference(info) })
        menu.addSeparator()
        menu.add(
            debugTabMenuItem(
                "Jump to Source",
                KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0),
                enabled = target != null
            ) {
                target?.let { jumpToSource(it) }
            }
        )
        menu.show(component, x, y)
    }

    /** 菜单项：文本、可选快捷键与可用性。 */
    private fun debugTabMenuItem(
        text: String,
        accelerator: KeyStroke? = null,
        enabled: Boolean = true,
        action: () -> Unit
    ): JMenuItem = JMenuItem(text).apply {
        this.accelerator = accelerator
        isEnabled = enabled
        addActionListener { action() }
    }

    /** 右键标签对应的调试页；容器日志页返回 null。 */
    private fun debugTabOf(info: TabInfo): DebugTab? = debugTabs.firstOrNull { it.info == info }

    /** 关闭一个调试子标签：调试页与容器日志页走各自的关闭流程。 */
    private fun closeDebugPage(info: TabInfo) {
        debugTabOf(info)?.let {
            closeDebugTab(it)
            return
        }
        if (logPanelInfo == info) {
            closeContainerLogs()
        }
    }

    /** 按右键时的标签快照成批关闭，避免边遍历边移除导致漏关。 */
    private fun closeDebugPages(infos: List<TabInfo>) {
        infos.forEach { closeDebugPage(it) }
    }

    /** 复制当前调试页的引用（类#方法）；容器日志页复制模块名。 */
    private fun copyDebugReference(info: TabInfo) {
        val reference = debugTabOf(info)?.panel?.target?.displayName
            ?: logPanel?.takeIf { logPanelInfo == info }?.moduleName
            ?: return
        CopyPasteManager.getInstance().setContents(StringSelection(reference))
    }

    /**
     * 跳到调试页对应方法的源码。
     *
     * [DebugTarget] 只保存类名与方法名，这里在 read action 中反查 PSI：
     * 同名方法多于一个时按参数个数与类型名收窄，仍无法区分时取第一个。
     */
    private fun jumpToSource(target: DebugTarget) {
        if (disposed || project.isDisposed) {
            return
        }
        ReadAction.nonBlocking<PsiMethod?> {
            findDebugMethod(target)
        }.finishOnUiThread(ModalityState.nonModal()) { method ->
            if (method != null && !disposed && !project.isDisposed && method.isValid) {
                navigate(method)
            }
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    /** 按调试目标反查接口方法；类不在项目源码范围内时返回 null。 */
    private fun findDebugMethod(target: DebugTarget): PsiMethod? {
        val psiClass = JavaPsiFacade.getInstance(project)
            .findClass(target.className, GlobalSearchScope.projectScope(project)) ?: return null
        val candidates = psiClass.findMethodsByName(target.methodName, false)
            .filter { it.parameterList.parametersCount == target.parameters.size }
        if (candidates.size <= 1) {
            return candidates.firstOrNull()
        }
        val expected = target.parameters.map { it.typeName }
        return candidates.firstOrNull { method ->
            method.parameterList.parameters.map { it.type.presentableText } == expected
        } ?: candidates.first()
    }

    /** 可关闭的标签页：标题由 JBTabs 绘制，关闭动作渲染在标签右侧。 */
    private fun tabInfo(
        component: JComponent,
        title: String,
        icon: Icon?,
        tooltip: String?,
        onClose: () -> Unit
    ): TabInfo {
        val info = TabInfo(component)
            .setText(title)
            .setIcon(icon)
            .setTooltipText(tooltip)
        info.setTabLabelActions(DefaultActionGroup(closeTabAction(info, onClose)), TAB_ACTIONS_PLACE)
        return info
    }

    /**
     * 顶层页签按钮：图标 + 文字，自绘圆角块背景。
     *
     * 悬停显示灰色块、选中显示品牌蓝块，图标始终保持原样；宽度随标题
     * 文字自适应。平台 JBTabs 没有可替换的标签绘制入口，顶层两个固定
     * 页签用轻量自绘实现。
     */
    private inner class TopPageButton(
        private val normalIcon: Icon,
        private val text: String,
        tooltip: String,
        private val onClick: () -> Unit
    ) : JComponent() {

        var selected = false
            set(value) {
                if (field != value) {
                    field = value
                    repaint()
                }
            }

        private var hovered = false

        init {
            isOpaque = false
            isFocusable = true
            toolTipText = tooltip
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            preferredSize = preferredButtonSize()
            minimumSize = preferredSize
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(event: MouseEvent) {
                    hovered = true
                    repaint()
                }

                override fun mouseExited(event: MouseEvent) {
                    hovered = false
                    repaint()
                }

                override fun mouseClicked(event: MouseEvent) {
                    if (SwingUtilities.isLeftMouseButton(event)) {
                        onClick()
                    }
                }
            })
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "lucky.topPage.activate")
            actionMap.put(
                "lucky.topPage.activate",
                object : AbstractAction() {
                    override fun actionPerformed(event: java.awt.event.ActionEvent) = onClick()
                }
            )
        }

        /** 图标 + 间距 + 文字 + 左右内边距；高度沿用顶层页签的图标按钮高度。 */
        private fun preferredButtonSize(): Dimension {
            val metrics = getFontMetrics(UIUtil.getLabelFont())
            val width = normalIcon.iconWidth +
                JBUI.scale(TOP_PAGE_ICON_TEXT_GAP) +
                metrics.stringWidth(text) +
                JBUI.scale(TOP_PAGE_BUTTON_PADDING_X * 2)
            return Dimension(width, JBUI.scale(TOP_PAGE_BUTTON_SIZE))
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val background = when {
                    selected -> TOP_PAGE_SELECTED_BACKGROUND
                    hovered -> TOP_PAGE_HOVER_BACKGROUND
                    else -> null
                }
                if (background != null) {
                    g2.color = background
                    val arc = JBUI.scale(TOP_PAGE_BUTTON_ARC)
                    g2.fillRoundRect(0, 0, width, height, arc, arc)
                }
                // 图标与文字作为整体水平居中，文字基线按行高居中
                g2.font = UIUtil.getLabelFont()
                val metrics = g2.fontMetrics
                val gap = JBUI.scale(TOP_PAGE_ICON_TEXT_GAP)
                val contentWidth = normalIcon.iconWidth + gap + metrics.stringWidth(text)
                val startX = (width - contentWidth) / 2
                normalIcon.paintIcon(this, g2, startX, (height - normalIcon.iconHeight) / 2)
                g2.color = if (selected) Color.WHITE else UIUtil.getLabelForeground()
                g2.drawString(
                    text,
                    startX + normalIcon.iconWidth + gap,
                    (height - metrics.height) / 2 + metrics.ascent
                )
            } finally {
                g2.dispose()
            }
        }
    }

    /**
     * 标签页上的关闭动作：平台以图标按钮形式绘制在标签右侧。
     *
     * 模板图标是透明的，只在指针位于对应标签内时由 [update] 换成可见的 ×。
     * 图标切换不改变标签宽度，也不依赖动作面板的 autoHide 标记，因此平台
     * 重建动作面板后仍能保持“未悬停不显示”，不会出现全部显示或闪烁。
     */
    private fun closeTabAction(info: TabInfo, onClose: () -> Unit): AnAction {
        return object : AnAction("Close", "Close tab", EmptyTabActionIcon) {
            // 标签动作面板只收集 EDT 线程的动作，BGT 会被过滤掉不显示
            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

            override fun update(event: AnActionEvent) {
                event.presentation.icon =
                    if (isPointerOverTab(info)) CloseTabIcon else EmptyTabActionIcon
            }

            override fun actionPerformed(event: AnActionEvent) {
                onClose()
            }
        }
    }

    /** Debug 页中的调试子标签标题；供测试验证多标签页互不覆盖。 */
    internal fun debugTabTitlesForTest(): List<String> =
        (0 until debugPageTabs.tabCount).map { debugPageTabs.getTabAt(it).text }

    /** 顶层当前选中的页签标题；供测试确认默认页与调试自动切换。 */
    internal fun selectedTopPageForTest(): String =
        if (navigatorButton.selected) NAVIGATOR_TAB_TITLE else DEBUG_TAB_TITLE

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
        val stamp = LuckyModificationTrackers.java(project).modificationCount
        val configStamp = LuckyModificationTrackers.configFiles(project).modificationCount
        val rootsStamp = LuckyModificationTrackers.roots(project)
        if (stamp == lastStamp && configStamp == lastConfigStamp && rootsStamp == lastRootsStamp) {
            return
        }
        val rootsChanged = rootsStamp != lastRootsStamp
        lastStamp = stamp
        lastConfigStamp = configStamp
        lastRootsStamp = rootsStamp
        refresh()
        // 模块/依赖变化可能带来新的主类，重新探测可运行容器
        if (rootsChanged) {
            discoverContainers()
        }
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

    /** 标题栏「Jump to Source」：跳转到当前页选中项的源码位置。 */
    private inner class JumpToSourceAction : AnAction(
        "Jump to Source",
        "Open the source of the selected component, method, or debug target",
        AllIcons.General.Locate
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(event: AnActionEvent) {
            val data = selectedNodeData()
            event.presentation.isEnabled = !DumbService.isDumb(project) && when (currentTopPage) {
                TopPage.NAVIGATOR -> data is NodeData.Component || data is NodeData.Item
                TopPage.DEBUG -> selectedDebugTarget() != null
            }
        }

        override fun actionPerformed(event: AnActionEvent) {
            when (currentTopPage) {
                TopPage.NAVIGATOR -> activate(selectedNodeData())
                TopPage.DEBUG -> selectedDebugTarget()?.let { jumpToSource(it) }
            }
        }
    }

    /** 导航树当前选中节点的数据；没有选中时返回 null。 */
    private fun selectedNodeData(): Any? =
        (tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode)?.userObject

    /** 调试页当前子标签的目标；选中的是容器日志页或没有调试页时返回 null。 */
    private fun selectedDebugTarget(): DebugTarget? =
        debugPageTabs.selectedInfo?.let { debugTabOf(it)?.panel?.target }

    private inner class RefreshAction : AnAction(
        "Refresh",
        "Rescan Lucky components",
        AllIcons.Actions.Refresh
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(event: AnActionEvent) {
            invalidateScans()
            refresh()
            discoverContainers()
        }
    }

    /**
     * 展开/折叠按钮的可见性：只对导航树有意义，切到调试页时隐藏。
     * 标题栏 toolbar 会周期性刷新 presentation，切页后很快同步。
     */
    private fun updateTreeActionPresentation(event: AnActionEvent) {
        val navigatorActive = currentTopPage == TopPage.NAVIGATOR
        event.presentation.isVisible = navigatorActive
        event.presentation.isEnabled = navigatorActive && tree.rowCount > 0
    }

    /** 标题栏「Expand All」：展开导航树全部节点，并记住展开状态。 */
    private inner class ExpandAllNavigatorAction : AnAction(
        "Expand All",
        "Expand all nodes in the navigator tree",
        AllIcons.Actions.Expandall
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(event: AnActionEvent) = updateTreeActionPresentation(event)

        override fun actionPerformed(event: AnActionEvent) {
            var row = 0
            while (row < tree.rowCount) {
                tree.expandRow(row)
                row++
            }
            expandedKeys = collectExpandedKeys()
        }
    }

    /** 标题栏「Collapse All」：折叠导航树全部节点，只保留顶层项目行。 */
    private inner class CollapseAllNavigatorAction : AnAction(
        "Collapse All",
        "Collapse all nodes in the navigator tree",
        AllIcons.Actions.Collapseall
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(event: AnActionEvent) = updateTreeActionPresentation(event)

        override fun actionPerformed(event: AnActionEvent) {
            var row = tree.rowCount - 1
            while (row >= 0) {
                tree.collapseRow(row)
                row--
            }
            expandedKeys = collectExpandedKeys()
            tree.repaint()
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

    // ---------------------------------------------------------------------------------------------
    // 节点与渲染
    // ---------------------------------------------------------------------------------------------

    /** 项目行图标：文件夹 + 右下角容器状态方块，启动中带呼吸效果。 */
    private fun moduleNodeIcon(moduleName: String): Icon {
        val state = containerStateForIcon(moduleName)
        val breathing = state == DebugContainerService.State.STARTING ||
            state == DebugContainerService.State.RESTARTING
        return StatusBadgeIcon(AllIcons.Nodes.Folder, containerBadgeColor(state), breathing)
    }

    /** 图标渲染用的轻量状态查询：只读 volatile 字段，避免每帧复制容器日志。 */
    private fun containerStateForIcon(moduleName: String): DebugContainerService.State? =
        containerService.statuses().firstOrNull { it.moduleName == moduleName }?.state

    private sealed class NodeData {
        data class Project(val node: LuckyProjectNode) : NodeData()
        data class Group(val project: String, val group: LuckyGroupNode) : NodeData()
        data class Package(
            val project: String,
            val groupTitle: String,
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
                        LuckyProjectKind.MODULE -> moduleNodeIcon(data.node.name)
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
                    appendHighlighted(data.group.title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
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

        /** 右键菜单中的调试入口：与调试面板执行按钮同一枚小飞机图标。 */
        private val DEBUG_ICON: Icon = LuckyIcons.Send

        /** 方法行右键菜单项的文字。 */
        private const val DEBUG_METHOD_TOOLTIP = "Debug Method"

        /** 项目行容器动作图标：尺寸、图标之间的间距、与选区右端的留白（逻辑像素）。 */
        private const val ACTION_ICON_SIZE = 16
        private const val ACTION_ICON_GAP = 6
        private const val ACTION_CONTENT_GAP = 8

        /** 容器栏：内边距、条目间距、电源按钮尺寸与呼吸动效参数（逻辑像素/毫秒）。 */
        private const val CONTAINER_BAR_PADDING_X = 6
        private const val CONTAINER_BAR_PADDING_Y = 2
        private const val CONTAINER_CHIP_GAP = 2
        private const val CONTAINER_ICON_GAP = 4
        private const val CONTAINER_POWER_SIZE = 22
        private const val POWER_ARC_SIZE = 12
        private const val POWER_PULSE_INTERVAL_MS = 60
        private const val POWER_PULSE_SPEED = 0.35
        private const val CONTAINER_NAME_MAX_CHARS = 22

        /** 容器条目：内边距与背景框圆角半径（逻辑像素）。 */
        private const val CONTAINER_CHIP_PADDING_X = 8
        private const val CONTAINER_CHIP_PADDING_Y = 2
        private const val CONTAINER_CHIP_ARC = 6

        /**
         * 容器条目背景框配色：未选中沉在容器栏下，选中提亮一档并配亮描边。
         *
         * 选中/未选中必须一眼能分开，因此不直接复用编辑器标签的
         * `underlinedTabBackground`（当前主题下它与标签栏底色相同，
         * 只能靠描边区分）。取值对齐 expUI 灰阶：深浅主题均与容器栏拉开对比。
         */
        private val CONTAINER_CHIP_BACKGROUND: Color =
            JBColor.namedColor("EditorTabs.background", JBColor(0xFFFFFF, 0x1E1F22))
        private val CONTAINER_CHIP_SELECTED_BACKGROUND = JBColor(0xEBECF0, 0x393B40)
        private val CONTAINER_CHIP_SELECTED_BORDER = JBColor(0xA8ADBD, 0x4E5157)

        /** 行尾图标与竖直滚动条之间、以及滚动条宽度未知时的默认值（逻辑像素）。 */
        private const val ACTION_SCROLLBAR_GAP = 6
        private const val DEFAULT_SCROLLBAR_WIDTH = 14

        /** 过渡态转圈：经典 8 帧进度图标与帧间隔（毫秒）。 */
        private val SPINNER_FRAMES = arrayOf(
            AllIcons.Process.Step_1,
            AllIcons.Process.Step_2,
            AllIcons.Process.Step_3,
            AllIcons.Process.Step_4,
            AllIcons.Process.Step_5,
            AllIcons.Process.Step_6,
            AllIcons.Process.Step_7,
            AllIcons.Process.Step_8
        )
        private const val SPINNER_FRAME_MS = 90

        /** 项目图标状态方块呼吸动画的帧间隔（毫秒）。 */
        private const val BADGE_PULSE_INTERVAL_MS = 60

        /** 顶层固定标签的标题与提示；界面文案保持英文。 */
        private const val NAVIGATOR_TAB_TITLE = "Navigator"
        private const val NAVIGATOR_TAB_TOOLTIP = "Browse Lucky components and APIs"
        private const val DEBUG_TAB_TITLE = "Debug"
        private const val DEBUG_TAB_TOOLTIP = "All API debug pages and container logs"

        /** 顶层内容卡片名。 */
        private const val NAVIGATOR_CARD = "navigator"
        private const val DEBUG_CARD = "debug"

        /** 顶层图标页签：按钮高度、圆角直径、按钮间距、左右内边距与图标文字间距（逻辑像素）。 */
        private const val TOP_PAGE_BUTTON_SIZE = 24
        private const val TOP_PAGE_BUTTON_ARC = 12
        private const val TOP_PAGE_BUTTON_GAP = 3
        private const val TOP_PAGE_BUTTON_PADDING_X = 8
        private const val TOP_PAGE_ICON_TEXT_GAP = 4

        /** 顶层页签配色：悬停灰块、选中品牌蓝块，与工具窗图标按钮一致。 */
        private val TOP_PAGE_HOVER_BACKGROUND = JBColor(Color(0xE9EBEE), Color(0x3F4248))
        private val TOP_PAGE_SELECTED_BACKGROUND = JBColor(Color(0x3574F0), Color(0x3574F0))

        /** Debug 页在占位提示与调试子标签栏之间切换的卡片名。 */
        private const val DEBUG_EMPTY_CARD = "empty"
        private const val DEBUG_TABS_CARD = "tabs"

        /** 调试标签页动作（关闭）的 place；只影响标签内动作按钮。 */
        private const val TAB_ACTIONS_PLACE = "LuckyNavigatorTabs"

        /** 「隐藏标签」按钮的边长（逻辑像素）。 */
        private const val MORE_TABS_BUTTON_SIZE = 20

        /** 关闭图标兜底轮询间隔（毫秒）。 */
        private const val CLOSE_ICON_POLL_INTERVAL_MS = 1000

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

/** 标签页关闭图标：自绘 ×，不依赖 SVG 图标绘制链路。 */
private object CloseTabIcon : Icon {

    override fun getIconWidth(): Int = 16

    override fun getIconHeight(): Int = 16

    override fun paintIcon(component: Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
            g2.color = JBColor(Color(0x6C707E), Color(0xA8ADB8))
            g2.stroke = BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g2.drawLine(x + 5, y + 5, x + 11, y + 11)
            g2.drawLine(x + 11, y + 5, x + 5, y + 11)
        } finally {
            g2.dispose()
        }
    }
}

/** 关闭按钮的隐藏态：与关闭图标同尺寸的透明图标，保留按钮占位但不可见。 */
private object EmptyTabActionIcon : Icon {

    override fun getIconWidth(): Int = 16

    override fun getIconHeight(): Int = 16

    override fun paintIcon(component: Component?, g: Graphics, x: Int, y: Int) = Unit
}

/** 「隐藏标签」按钮图标：自绘下箭头，不依赖 SVG 图标绘制链路。 */
private object MoreTabsIcon : Icon {

    override fun getIconWidth(): Int = 16

    override fun getIconHeight(): Int = 16

    override fun paintIcon(component: Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
            g2.color = JBColor(Color(0x6C707E), Color(0xA8ADB8))
            g2.stroke = BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g2.drawLine(x + 4, y + 6, x + 8, y + 10)
            g2.drawLine(x + 12, y + 6, x + 8, y + 10)
        } finally {
            g2.dispose()
        }
    }
}

/** path 实际渲染的一段文本与样式，用于测量圆角背景宽度。 */
private class RenderedPathFragment(val text: String, val attributes: SimpleTextAttributes)

/**
 * 在项目图标右下角叠画容器状态角标。
 *
 * 形状与位置对齐平台 Module 图标：6×6 圆角方块（圆角半径 1.5），
 * 描边色表达容器状态，填充用图标底色；启动中按呼吸节奏改变描边透明度。
 */
private class StatusBadgeIcon(
    private val base: Icon,
    private val badgeColor: Color,
    private val breathing: Boolean
) : Icon {

    override fun getIconWidth(): Int = base.iconWidth

    override fun getIconHeight(): Int = base.iconHeight

    override fun paintIcon(component: Component?, g: Graphics, x: Int, y: Int) {
        base.paintIcon(component, g, x, y)
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
            // x/y 是设备像素，而 iconWidth/iconHeight 是逻辑尺寸，必须先乘回设备缩放
            val size = JBUIScale.scale(BADGE_SIZE)
            val inset = JBUIScale.scale(BADGE_INSET)
            val left = x + JBUIScale.scale(base.iconWidth.toFloat()) - size - inset
            val top = y + JBUIScale.scale(base.iconHeight.toFloat()) - size - inset
            val arc = JBUIScale.scale(BADGE_ARC * 2)
            val rect = RoundRectangle2D.Float(left, top, size, size, arc, arc)
            g2.color = BADGE_FILL
            g2.fill(rect)
            g2.color = if (breathing) pulseColor() else badgeColor
            g2.stroke = BasicStroke(JBUIScale.scale(BADGE_STROKE))
            g2.draw(rect)
        } finally {
            g2.dispose()
        }
    }

    /** 按时间计算呼吸透明度；JBColor 用 rgb 取值以跟随深浅主题。 */
    private fun pulseColor(): Color {
        val pulse = (sin(System.currentTimeMillis() / BADGE_PULSE_RADIANS_PER_MS) + 1.0) / 2.0
        val alpha = (255 * (PULSE_MIN_ALPHA + (1 - PULSE_MIN_ALPHA) * pulse)).toInt()
        val rgb = badgeColor.rgb
        return Color((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, alpha)
    }

    private companion object {
        /** 状态角标边长（逻辑像素）：略大于平台 Module 图标角标，状态更醒目。 */
        private const val BADGE_SIZE = 7f

        /** 角标右侧/底部与图标边缘的间距（逻辑像素）：等于描边半径，使描边外缘恰好贴边。 */
        private const val BADGE_INSET = 0.5f

        /** 角标圆角半径（逻辑像素）。 */
        private const val BADGE_ARC = 1.5f

        /** 角标描边宽度（逻辑像素）。 */
        private const val BADGE_STROKE = 1f

        /** 角标底色：与平台 Module 图标角标同色，突出描边状态色。 */
        private val BADGE_FILL = JBColor(Color(0xE7EFFD), Color(0x25324D))

        /** 呼吸低点的透明度。 */
        private const val PULSE_MIN_ALPHA = 0.35

        /** 呼吸角速度：毫秒除以该值得到弧度，周期约 1 秒。 */
        private const val BADGE_PULSE_RADIANS_PER_MS = 250.0
    }
}

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
