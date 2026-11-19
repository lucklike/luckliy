package com.luckyframework.idea.debug.container

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilationStatusListener
import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.compiler.CompilerTopics
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.concurrency.AppExecutorUtil
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * 编译完成监听。
 *
 * 两条来源合并到同一个防抖窗口：
 * 1. CompilerManager 的编译状态：IDEA 内构建的主路径；
 * 2. 输出目录的 VFS 变更：捕获外部构建工具直接写文件的情况。
 *
 * 只登记模块的生产输出根，监听器在池化线程中回调；
 * 调用方需要自行切到后台任务，避免阻塞编译事件线程。
 */
@Service(Service.Level.PROJECT)
class CompilationWatcher(private val project: Project) : Disposable {

    fun interface Listener {
        fun compiled(module: Module)
    }

    private val watched = ConcurrentHashMap.newKeySet<Module>()
    private val roots = ConcurrentHashMap<Module, List<String>>()
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val pending = ConcurrentHashMap<Module, ScheduledFuture<*>>()

    private val statusListener = object : CompilationStatusListener {
        override fun compilationFinished(aborted: Boolean, errors: Int, warnings: Int, context: CompileContext) {
            if (aborted || errors > 0 || watched.isEmpty()) {
                return
            }
            // CompileScope 没有按模块的包含判断；多模块项目任一模块编译
            // 都会触发全部活动容器重启，与「每个模块各自起容器」一致
            watched.forEach(::schedule)
        }
    }

    init {
        project.messageBus.connect(this).subscribe(CompilerTopics.COMPILATION_STATUS, statusListener)
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    if (watched.isEmpty() || events.isEmpty()) {
                        return
                    }
                    val touched = HashSet<Module>()
                    for (event in events) {
                        val path = event.path
                        roots.forEach { (module, moduleRoots) ->
                            if (moduleRoots.any { under(path, it) }) {
                                touched.add(module)
                            }
                        }
                    }
                    touched.forEach(::schedule)
                }
            })
    }

    /** 登记模块；没有输出根时静默跳过。 */
    fun watch(module: Module) {
        val moduleRoots = outputRoots(module)
        if (moduleRoots.isEmpty()) {
            return
        }
        roots[module] = moduleRoots
        watched.add(module)
    }

    fun unwatch(module: Module) {
        watched.remove(module)
        roots.remove(module)
        pending.remove(module)?.cancel(false)
    }

    fun addListener(listener: Listener, parent: Disposable): Disposable {
        listeners.add(listener)
        val disposable = Disposable { listeners.remove(listener) }
        Disposer.register(parent, disposable)
        return disposable
    }

    private fun schedule(module: Module) {
        if (!watched.contains(module)) {
            return
        }
        pending.remove(module)?.cancel(false)
        val future = AppExecutorUtil.getAppScheduledExecutorService().schedule({
            pending.remove(module)
            if (watched.contains(module) && !project.isDisposed) {
                listeners.forEach { it.compiled(module) }
            }
        }, DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS)
        pending[module] = future
    }

    private fun outputRoots(module: Module): List<String> = runCatching {
        // 测试输出变化不应触发容器重启，也只监听生产输出。
        listOfNotNull(CompilerPaths.getModuleOutputPath(module, false))
            .map { File(it).absolutePath }
            .distinct()
    }.getOrDefault(emptyList())

    private fun under(path: String, root: String): Boolean =
        path == root || path.startsWith(root + File.separator)

    override fun dispose() {
        pending.values.forEach { it.cancel(false) }
        pending.clear()
        roots.clear()
        watched.clear()
        listeners.clear()
    }

    companion object {
        /** 编译与 VFS 两条来源合并的防抖窗口。 */
        const val DEBOUNCE_MILLIS = 800L

        fun getInstance(project: Project): CompilationWatcher = project.service()
    }
}
