package com.luckyframework.idea.spel.component

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.lang.reflect.Method
import javax.swing.Icon

/**
 * 编辑器标签页图标刷新。
 *
 * 首次打开项目时，标签页会话恢复与组件扫描是两条异步链路：标签页在扫描完成前
 * 创建，只能拿到平台默认图标。平台提供的两条刷新路径都不可靠：
 *
 * - [FileEditorManager.updateFilePresentation] 只是入队，消费时按 `isFileOpen`
 *   过滤（2026.3 `processFileUpdateRequests`），未登记的请求被直接丢弃；
 * - `refreshIcons()` 在 `initJob` 未完成时静默返回，内部的 `updateFileIcon`
 *   也只在分栏能定位到已渲染的标签页时才 `tab.setIcon`。
 *
 * 这里改为直接调用 `EditorsSplitters.updateFileIconImmediately` 写图标
 * （2024.1 与 2026.3 同名同签名），不依赖初始化状态与合并队列；标签页尚未
 * 创建时调用是空操作，所以调用方要在会话恢复期间反复重试。
 */
internal object EditorTabIconRefresher {

    /**
     * 刷新组件标签页图标，返回写入的文件数；平台入口缺失返回 -1。
     *
     * 直接写入不可用时回退平台的刷新入口，仍然失败才回到公开 API。
     */
    fun refresh(project: Project, manager: FileEditorManager): Int {
        val files = componentFiles(project, manager)
        if (files.isEmpty()) {
            return 0
        }
        val icon = ComponentIconResolver.icon() ?: return 0
        val applied = applyToSplitters(splittersOf(manager), files, icon)
        if (applied > 0) {
            return applied
        }
        if (!invokeRefreshIcons(manager)) {
            files.forEach { manager.updateFilePresentation(it) }
        }
        return applied
    }
}

/** 平台直接写标签页图标的入口（Kotlin `internal` 成员在 JVM 上的名称）。 */
internal const val APPLY_ICON_METHOD = "updateFileIconImmediately\$intellij_platform_ide_impl"

/**
 * 把图标写到各分栏容器的标签页，返回写入的文件数。
 *
 * 文件不在该分栏内时平台入口是空操作，重复调用没有副作用；入口缺失返回 -1，
 * 由调用方回退其他刷新方式。同一文件写入多个分栏只计一次。
 */
internal fun applyToSplitters(splitters: List<Any>, files: List<VirtualFile>, icon: Icon?): Int {
    if (icon == null || files.isEmpty()) {
        return 0
    }
    // 先确认所有分栏都提供入口：部分分栏写入成功不应被其他分栏的缺失掩盖
    val methods = splitters.map { updateIconMethod(it) ?: return -1 }
    val written = HashSet<VirtualFile>(files.size)
    for ((index, splitter) in splitters.withIndex()) {
        for (file in files) {
            if (runCatching { methods[index].invoke(splitter, file, icon) }.isSuccess) {
                written.add(file)
            }
        }
    }
    return written.size
}

/**
 * 编辑器分栏容器。
 *
 * 2024.1 提供 `getAllSplitters()`；入口缺失（测试替身 `TestEditorManagerImpl`）
 * 或初始化未完成（`mainSplitters` 延迟赋值）时改为经 `EditorWindow.getOwner()`
 * 获取，两者都失败返回空列表，由调用方在下一拍重试。
 */
internal fun splittersOf(manager: Any): List<Any> {
    val all = runCatching {
        manager.javaClass.getMethod("getAllSplitters").invoke(manager) as? Collection<*>
    }.getOrNull()
    if (!all.isNullOrEmpty()) {
        return all.filterNotNull()
    }
    val windows = runCatching {
        manager.javaClass.getMethod("getWindows").invoke(manager) as? Array<*>
    }.getOrNull() ?: return emptyList()
    return windows.filterNotNull().mapNotNull { window ->
        runCatching { window.javaClass.getMethod("getOwner").invoke(window) }.getOrNull()
    }.distinct()
}

/**
 * 刷新是否继续观察标签页。
 *
 * 会话恢复是异步的：标签页可能比组件扫描更晚出现，`openFiles` 为空时更要继续
 * 等待；至少跑满 [minTicks] 拍，标签页数量变化时顺延，最多 [maxTicks] 拍。
 */
internal fun shouldContinueTabTicks(
    tick: Int,
    lastTabCount: Int,
    openTabCount: Int,
    minTicks: Int,
    maxTicks: Int
): Boolean = tick < maxTicks && (tick < minTicks || openTabCount == 0 || openTabCount != lastTabCount)

/** 通过反射调用平台的图标刷新入口，返回是否调用成功。 */
internal fun invokeRefreshIcons(manager: Any): Boolean {
    val refresh = runCatching {
        manager.javaClass.getMethod("refreshIcons")
    }.getOrNull() ?: return false
    return runCatching { refresh.invoke(manager) }.isSuccess
}

/** 已打开文件中的组件文件（Java 类名与文件名一致，按简单名匹配即可）。 */
private fun componentFiles(project: Project, manager: FileEditorManager): List<VirtualFile> {
    val names = ComponentNameCache.names(project)
    if (names.isEmpty) {
        return emptyList()
    }
    return manager.openFiles.filter { it.extension == "java" && names.matchesSimpleName(it.nameWithoutExtension) }
}

/** 平台的标签页图标写入入口；不同版本改名的返回 null。 */
private fun updateIconMethod(splitter: Any): Method? = runCatching {
    splitter.javaClass.getMethod(APPLY_ICON_METHOD, VirtualFile::class.java, Icon::class.java)
}.getOrNull()
