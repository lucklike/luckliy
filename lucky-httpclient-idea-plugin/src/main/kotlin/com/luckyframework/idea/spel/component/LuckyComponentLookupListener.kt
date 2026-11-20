package com.luckyframework.idea.spel.component

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupItem
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.util.ui.EmptyIcon
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import java.awt.Component
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.Collections
import java.util.WeakHashMap
import javax.swing.Icon

/**
 * 补全列表中的组件图标。
 *
 * 补全列表由 `LookupCellRenderer` 绘制，候选图标在 2026.3 之前来自
 * `DefaultLookupItemRenderer` 对 `ICON_ATTR` / `PsiElement.getIcon()` 的读取；
 * 2026.3 的 `JavaPsiClassReferenceElement` 在构造时就把图标缓存进 `myIcon`，
 * 渲染只认缓存，任何后置属性注入都不再生效（构造时读到的还是平台默认图标）。
 *
 * 因此这里优先安装平台从 2024.1 起提供的渲染期定制钩子
 * （`LookupImpl.addPresentationCustomizer`）：钩子在每次绘制候选项时改写
 * 展示数据，覆盖所有候选及后续输入新增的候选，也不依赖候选对象的身份与
 * 注入时机。运行环境没有该钩子时回退到按候选设置 `ICON_ATTR`（老平台
 * 渲染时会读取它）。
 *
 * `Lookup.items` 只包含当前前缀匹配的可见候选；继续输入后新进入列表的
 * 候选没有经过 `activeLookupChanged`，回退路径还要在文档变更（每次按键）
 * 后补一次注入，否则新候选仍是平台默认图标。
 *
 * 组件身份只读 [SpelRuntimeSymbols.cachedComponents]：补全在 EDT 上执行，
 * 这里不能主动触发注解索引扫描；快照尚未预热时保持平台默认图标。判定候选
 * 时只读 [ComponentNames] 的缓存名称（类名补全元素在构造时已缓存全限定名），
 * 不访问 PSI：绘制阶段没有读锁。
 */
class LuckyComponentLookupListener(private val project: Project) : LookupManagerListener, DocumentListener {

    /** 已经安装过渲染钩子的 lookup；弱引用避免阻止补全列表回收。 */
    private val customizedLookups = Collections.synchronizedMap(WeakHashMap<Lookup, Boolean>())

    /** 上次请求后台预热的时间，避免无组件项目反复扫描。 */
    private var lastWarmUpRequest = 0L

    override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
        val lookup = newLookup ?: return
        if (!lookup.isCompletion || lookup.project.isDisposed) {
            return
        }
        if (installPresentationCustomizer(lookup)) {
            // 渲染钩子覆盖所有候选及其后续变化，无需再逐个注入属性；
            // 清单尚未预热时请求一次后台扫描，预热完成后刷新器会重绘弹窗
            warmUpComponentsIfNeeded()
            return
        }
        val icon = configuredIcon() ?: return
        val componentNames = componentNames() ?: return
        // 事件可能先于查找列表把候选写入 LookupImpl（此时 getItems 为空），
        // 同一个 EDT 事件结束后再试，否则整次补全都会停留在默认图标
        if (!inject(lookup, componentNames, icon)) {
            retry(lookup, componentNames, icon, MAX_RETRIES)
        }
    }

    override fun documentChanged(event: DocumentEvent) {
        if (project.isDisposed || !ApplicationManager.getApplication().isDispatchThread) {
            return
        }
        val lookup = LookupManager.getInstance(project).activeLookup
        if (lookup == null) {
            // 普通编辑：没有补全列表需要处理，不引入 EDT 排队
            return
        }
        if (customizedLookups.containsKey(lookup)) {
            // 渲染钩子每次重绘都会生效，无需按候选补注入
            return
        }
        ApplicationManager.getApplication().invokeLater({
            refreshActiveLookup()
        }, ModalityState.any())
    }

    /**
     * 给当前补全列表补一次图标。
     *
     * 继续输入后新进入列表的候选没有经过 [activeLookupChanged]；已经设置过
     * 图标的候选会被 [inject] 跳过，不会反复刷新列表。
     */
    private fun refreshActiveLookup() {
        if (project.isDisposed) {
            return
        }
        val lookup = LookupManager.getInstance(project).activeLookup ?: return
        if (!lookup.isCompletion) {
            return
        }
        if (installPresentationCustomizer(lookup)) {
            // lookup 事件早于监听器就绪时，这里补装渲染钩子
            warmUpComponentsIfNeeded()
            return
        }
        val icon = configuredIcon() ?: return
        val componentNames = componentNames() ?: return
        inject(lookup, componentNames, icon)
    }

    /**
     * 清单尚未预热时请求后台扫描。
     *
     * 启动时的预热可能因依赖未导入而得到空结果，补全恰好在这段时间弹出时
     * 图标会停在默认图标；这里补一次请求（限流，避免无组件项目反复扫描）。
     */
    private fun warmUpComponentsIfNeeded() {
        if (SpelRuntimeSymbols.cachedComponents(project) != null) {
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastWarmUpRequest < WARM_UP_INTERVAL_MS) {
            return
        }
        lastWarmUpRequest = now
        LuckyComponentIconRefresher.getInstance(project).scheduleRefresh()
    }

    /** 当前生效的组件图标；开关关闭时返回 null。 */
    private fun configuredIcon(): Icon? {
        if (!LuckyHttpClientSettings.getInstance().isComponentIconEnabled) {
            return null
        }
        return ComponentIconResolver.icon()
    }

    private fun componentNames(): ComponentNames? =
        ComponentNameCache.names(project).takeIf { !it.isEmpty }

    /**
     * 安装渲染期图标定制钩子（`LookupImpl.addPresentationCustomizer`）。
     *
     * 编译 SDK 没有该内部类型，因此用动态代理实现它的内部接口；运行环境
     * 没有该 API 时返回 false，调用方回退到按候选设置 `ICON_ATTR`。
     *
     * 代理方法必须遵守接口契约：返回值非空、不抛异常——渲染器直接把它
     * 的返回值当作列表单元格内容，任何异常都会让整行渲染失败。
     */
    private fun installPresentationCustomizer(lookup: Lookup): Boolean {
        if (customizedLookups.containsKey(lookup)) {
            return true
        }
        val registered = runCatching {
            val method = lookup.javaClass.methods.firstOrNull {
                it.name == ADD_PRESENTATION_CUSTOMIZER && it.parameterCount == 1
            } ?: return@runCatching false
            val customizerClass = method.parameterTypes[0]
            if (!customizerClass.isInterface ||
                customizerClass.methods.none { it.name == CUSTOMIZE_PRESENTATION }
            ) {
                return@runCatching false
            }
            val customizer = ComponentPresentationCustomizer(project)
            val handler = InvocationHandler { proxy, invoked, args ->
                when (invoked.name) {
                    CUSTOMIZE_PRESENTATION -> {
                        val item = args?.getOrNull(0) as? LookupElement
                        val presentation = args?.getOrNull(1) as? LookupElementPresentation
                        if (item == null || presentation == null) {
                            presentation ?: LookupElementPresentation()
                        } else {
                            runCatching { customizer.customizePresentation(item, presentation) }
                                .getOrDefault(presentation)
                        }
                    }
                    CUSTOMIZE_EMPTY_ICON -> (args?.getOrNull(0) as? Icon)
                        ?.let(customizer::customizeEmptyIcon) ?: EmptyIcon.ICON_0
                    "toString" -> "LuckyComponentPresentationCustomizer"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.getOrNull(0)
                    else -> null
                }
            }
            val proxy = Proxy.newProxyInstance(
                customizerClass.classLoader,
                arrayOf(customizerClass),
                handler
            )
            method.invoke(lookup, proxy)
            true
        }.getOrElse {
            logger.debug("Lookup presentation customizer is not available", it)
            false
        }
        if (registered) {
            customizedLookups[lookup] = true
        }
        return registered
    }

    /**
     * 给列表中属于组件类的候选设置品牌图标。
     *
     * @return 列表已填充（无论是否命中组件）时返回 true；列表仍为空时返回 false
     */
    private fun inject(lookup: Lookup, componentNames: ComponentNames, icon: Icon): Boolean {
        if (lookup.project.isDisposed) {
            return true
        }
        val items = lookup.items
        if (items.isEmpty()) {
            return false
        }
        var changed = false
        for (item in items) {
            val lookupItem = item as? LookupItem<*> ?: continue
            if (lookupItem.getAttribute(LookupItem.ICON_ATTR) === icon) {
                // 已经注入过：继续输入时避免反复重建展示数据
                continue
            }
            if (!componentNames.matches(lookupItem)) {
                continue
            }
            lookupItem.setAttribute(LookupItem.ICON_ATTR, icon)
            changed = true
        }
        if (changed) {
            refresh(lookup)
        }
        return true
    }

    /** 候选晚于 lookup 事件写入时，在下一个 EDT 事件重试有限次数。 */
    private fun retry(lookup: Lookup, componentNames: ComponentNames, icon: Icon, left: Int) {
        if (left <= 0) {
            return
        }
        ApplicationManager.getApplication().invokeLater({
            if (lookup.project.isDisposed || inject(lookup, componentNames, icon)) {
                return@invokeLater
            }
            retry(lookup, componentNames, icon, left - 1)
        }, ModalityState.any())
    }

    /**
     * 重建补全列表的展示数据。
     *
     * `refreshUi(boolean, boolean)` 是 `LookupImpl` 的方法，不在 `Lookup` 接口
     * 上；用反射调用避免编译期依赖平台实现类。方法不存在或调用失败时退化为
     * 直接重绘组件（渲染钩子仍在每次绘制时生效）。
     */
    private fun refresh(lookup: Lookup) {
        val refreshed = runCatching {
            lookup.javaClass
                .getMethod("refreshUi", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
                .invoke(lookup, false, false)
        }
        if (refreshed.isSuccess) {
            return
        }
        runCatching {
            lookup.javaClass.getMethod("getComponent").invoke(lookup) as? Component
        }.getOrNull()?.repaint()
    }

    private companion object {
        private val logger = Logger.getInstance(LuckyComponentLookupListener::class.java)

        /** 渲染期定制钩子：`LookupImpl.addPresentationCustomizer`。 */
        private const val ADD_PRESENTATION_CUSTOMIZER = "addPresentationCustomizer"

        /** 定制接口的方法名，用于确认参数类型确实是渲染回调。 */
        private const val CUSTOMIZE_PRESENTATION = "customizePresentation"

        private const val CUSTOMIZE_EMPTY_ICON = "customizeEmptyIcon"

        /** 清单未预热时请求后台扫描的最小间隔。 */
        private const val WARM_UP_INTERVAL_MS = 5000L

        /** lookup 事件早于候选填充时的最大重试次数。 */
        private const val MAX_RETRIES = 2
    }
}
