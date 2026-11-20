package com.luckyframework.idea.spel.component

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.Alarm
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import java.awt.Component

/**
 * 组件图标状态刷新。
 *
 * 首次打开项目时索引与依赖模型尚未就绪，[SpelRuntimeSymbols] 会得到空的
 * 组件清单，图标按平台默认渲染；索引完成或模块根变化后需要丢弃快照并重绘
 * 项目树与编辑器标签页，否则默认图标会一直保留到手动刷新。
 *
 * 刷新统一等到智能模式下再执行：dumb 期间重扫只会再次得到空结果，也应避免
 * 阻塞索引构建；多次触发由 [Alarm] 合并成一次重绘。
 *
 * 依赖模型（Maven/Gradle 导入）可能晚于索引就绪，此时重绘依然只能得到空
 * 结果；这时在有限次数内延迟重试，避免图标停留在平台默认图标，直到用户
 * 手动点击才陆续出现。
 */
@Service(Service.Level.PROJECT)
class LuckyComponentIconRefresher(private val project: Project) {

    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, project)

    /**
     * 丢弃组件扫描缓存，并在索引完成后的短暂延迟后刷新图标。
     *
     * 依赖导入、索引重建、模块根变化可能在短时间内连续触发，这里合并执行。
     */
    fun scheduleRefresh() {
        if (project.isDisposed) {
            return
        }
        DumbService.getInstance(project).smartInvokeLater {
            if (project.isDisposed) {
                return@smartInvokeLater
            }
            alarm.cancelAllRequests()
            alarm.addRequest({ refreshIcons(0) }, REFRESH_DELAY_MS)
        }
    }

    /** 立即重绘项目树与已打开文件的标签页图标（EDT）。 */
    fun refreshNow() {
        refreshIcons(0)
    }

    /**
     * 重绘图标并检查扫描结果。
     *
     * 扫描为空说明依赖或索引尚未就绪，下一次重绘仍会是默认图标；在
     * [MAX_RETRY] 次内延迟重试，直到扫描到组件或次数用尽。
     */
    private fun refreshIcons(attempt: Int) {
        if (project.isDisposed) {
            return
        }
        if (DumbService.isDumb(project)) {
            // 索引期间注解扫描不可用，等退出 dumb 后再刷新；不消耗重试次数，
            // 大项目首次索引可能持续数分钟
            alarm.cancelAllRequests()
            alarm.addRequest({ refreshIcons(attempt) }, RETRY_DELAY_MS)
            return
        }
        // 注解扫描是索引查询：在后台线程丢弃旧快照并重扫，EDT 重绘时直接命中
        // 新快照；失效与重扫在同一读操作内，避免 EDT 看到未重建的空窗口
        ApplicationManager.getApplication().executeOnPooledThread {
            val count = if (project.isDisposed) {
                0
            } else {
                ReadAction.compute<Int, RuntimeException> {
                    SpelRuntimeSymbols.invalidate(project)
                    SpelRuntimeSymbols.components(project).size
                }
            }
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) {
                    return@invokeLater
                }
                redraw()
                // 记录刷新时刻的扫描结果，便于排查“图标未出现”是扫描为空还是刷新未触发
                logger.info("Component icons refreshed (attempt $attempt): $count component(s)")
                if (count == 0 && attempt < MAX_RETRY) {
                    alarm.addRequest({ refreshIcons(attempt + 1) }, RETRY_DELAY_MS)
                }
            }
        }
    }

    /** 重绘项目树、已打开文件的标签页以及当前展开的补全列表（EDT，图标直接命中已预热的扫描快照）。 */
    private fun redraw() {
        val projectView = ProjectView.getInstance(project)
        projectView.currentProjectViewPane?.updateFromRoot(true)
        projectView.refresh()
        val editorManager = FileEditorManager.getInstance(project)
        editorManager.openFiles.forEach { editorManager.updateFilePresentation(it) }
        // 补全弹窗是独立窗口，frame.repaint() 不覆盖它；主动重绘已打开的列表，
        // 让预热完成后图标立即生效，而不是等用户再敲一个字符
        repaintActiveLookup()
        WindowManager.getInstance().getFrame(project)?.repaint()
    }

    /** 重绘当前展开的补全列表；`getComponent` 不在 `Lookup` 接口上，用反射获取。 */
    private fun repaintActiveLookup() {
        val lookup = LookupManager.getInstance(project).activeLookup ?: return
        runCatching {
            lookup.javaClass.getMethod("getComponent").invoke(lookup) as? Component
        }.getOrNull()?.repaint()
    }

    companion object {
        private val logger = Logger.getInstance(LuckyComponentIconRefresher::class.java)

        /** 合并依赖导入与索引完成引发的连续触发。 */
        private const val REFRESH_DELAY_MS = 500

        /** 依赖模型晚于索引就绪时的重试间隔。 */
        private const val RETRY_DELAY_MS = 2000

        /** 扫描为空时的最大重试次数，避免无组件项目反复重扫。 */
        private const val MAX_RETRY = 8

        fun getInstance(project: Project): LuckyComponentIconRefresher = project.service()
    }
}
