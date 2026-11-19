package com.luckyframework.idea.debug.container

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.concurrency.AppExecutorUtil
import com.luckyframework.idea.debug.protocol.DebugProtocol
import com.luckyframework.idea.debug.protocol.DebugRequest
import com.luckyframework.idea.debug.protocol.DebugResponse
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 按模块管理调试容器子进程。
 *
 * 每个模块一个容器；启动时解析主类与 classpath，编译落盘后自动重启。
 * 自动重启连续失败 [AutoRestartGuard.MAX_CONSECUTIVE_FAILURES] 次即熔断，
 * 避免编译监听与失败容器互相触发。
 *
 * 启动与停止会阻塞（最多数秒到数分钟），必须在后台线程调用；
 * [invoke] 只做请求转发，可以在任意线程调用。
 */
@Service(Service.Level.PROJECT)
class DebugContainerService(private val project: Project) : Disposable {

    enum class State {
        STOPPED, STARTING, READY, FAILED, STOPPING, RESTARTING, SUSPENDED
    }

    data class Snapshot(
        val moduleName: String,
        val state: State,
        val mainClass: String?,
        val info: ContainerLauncher.ContainerInfo?,
        val failure: String?,
        val logs: List<String>,
        val autoRestartSuspended: Boolean
    )

    sealed interface StartResult {
        data class Ready(val snapshot: Snapshot) : StartResult

        data class Failed(val message: String, val logs: List<String>) : StartResult

        /** classpath 超过命令行上限；[sample] 供提示框展示前几条路径。 */
        data class ClasspathTooLong(val length: Int, val limit: Int, val sample: List<String>) : StartResult

        data class MainClassMissing(val message: String, val candidates: List<String>) : StartResult

        data class IndexNotReady(val message: String) : StartResult
    }

    private class Holder(val module: Module) {
        val lock = ReentrantLock()
        val guard = AutoRestartGuard()
        val logs = DebugLogBuffer()
        @Volatile
        var state = State.STOPPED
        @Volatile
        var mainClass: String? = null
        @Volatile
        var programArgs: List<String> = DEFAULT_PROGRAM_ARGS
        @Volatile
        var autoRestartEnabled = true
        @Volatile
        var handle: ContainerLauncher.Started? = null
        @Volatile
        var failure: String? = null
    }

    private sealed interface MainClassResolution {
        data class Ok(val name: String) : MainClassResolution
        data class Missing(val message: String, val candidates: List<String>) : MainClassResolution
        data class Dumb(val message: String) : MainClassResolution
    }

    private val holders = ConcurrentHashMap<Module, Holder>()
    private val listeners = CopyOnWriteArrayList<(Module) -> Unit>()
    private val launcher = ContainerLauncher()

    init {
        CompilationWatcher.getInstance(project).addListener({ module -> onCompiled(module) }, this)
    }

    fun start(
        module: Module,
        mainClass: String? = null,
        programArgs: List<String> = DEFAULT_PROGRAM_ARGS,
        autoRestart: Boolean = true
    ): StartResult {
        val holder = holderOf(module)
        holder.lock.withLock {
            stopLocked(holder)
            // 启动失败（例如主类尚未编译）后仍监听构建，用户 Build 后自动重试
            CompilationWatcher.getInstance(project).watch(module)
            holder.guard.reset()
            holder.programArgs = programArgs
            holder.autoRestartEnabled = autoRestart
            holder.failure = null
            // 解析主类也可能耗时；先清空旧日志并对外呈现 STARTING，
            // 避免 Logs 里残留上一次启动的失败堆栈。
            holder.logs.clear()
            holder.state = State.STARTING
            notifyChanged(module)
            return when (val resolution = resolveMainClass(module, mainClass)) {
                is MainClassResolution.Ok -> {
                    holder.mainClass = resolution.name
                    // 自动解析的主类必须能在模块输出中找到；显式主类可能来自依赖 jar
                    startLocked(holder, verifyCompiled = mainClass.isNullOrBlank())
                }
                is MainClassResolution.Missing -> {
                    holder.state = State.FAILED
                    holder.failure = resolution.message
                    notifyChanged(module)
                    StartResult.MainClassMissing(resolution.message, resolution.candidates)
                }
                is MainClassResolution.Dumb -> {
                    holder.state = State.FAILED
                    holder.failure = resolution.message
                    notifyChanged(module)
                    StartResult.IndexNotReady(resolution.message)
                }
            }
        }
    }

    fun restart(module: Module): StartResult {
        val holder = holderOf(module)
        return start(module, holder.mainClass, holder.programArgs, holder.autoRestartEnabled)
    }

    fun stop(module: Module) {
        val holder = holders[module] ?: return
        holder.lock.withLock { stopLocked(holder) }
    }

    fun invoke(
        module: Module,
        request: DebugRequest,
        timeoutMillis: Long = DebugProtocol.DEFAULT_TIMEOUT_MILLIS
    ): DebugResponse {
        val holder = holders[module]
            ?: throw IllegalStateException("模块 ${module.name} 未启动调试容器")
        val handle = holder.handle
            ?: throw IllegalStateException("模块 ${module.name} 的容器未就绪（${holder.state}）")
        return handle.client.request(request, timeoutMillis)
    }

    fun snapshot(module: Module): Snapshot? = holders[module]?.let { snapshot(it) }

    fun addListener(listener: (Module) -> Unit, parent: Disposable): Disposable {
        listeners.add(listener)
        val disposable = Disposable { listeners.remove(listener) }
        Disposer.register(parent, disposable)
        return disposable
    }

    /** 用户手动重新启动后调用：解除自动重启熔断。 */
    fun resetAutoRestart(module: Module) {
        holders[module]?.guard?.reset()
    }

    override fun dispose() {
        holders.values.forEach { holder ->
            runCatching { holder.handle?.close() }
            holder.handle = null
            holder.state = State.STOPPED
        }
        holders.clear()
        listeners.clear()
    }

    private fun startLocked(holder: Holder, verifyCompiled: Boolean = false): StartResult {
        val module = holder.module
        val mainClass = holder.mainClass ?: return failure(holder, "没有可用的启动主类")
        holder.logs.clear()
        val launcherJar = try {
            LauncherJarProvider.jarPath()
        } catch (e: Throwable) {
            return failure(holder, "launcher jar 解压失败：${e.message}")
        }
        val classpath = when (val result = LauncherClasspath.build(module, launcherJar.toFile())) {
            is LauncherClasspath.Result.Ok -> result
            is LauncherClasspath.Result.Empty -> return failure(holder, "classpath 为空：模块没有生产输出或依赖")
            is LauncherClasspath.Result.TooLong -> {
                holder.state = State.FAILED
                holder.failure = "classpath 长度 ${result.length} 超过命令行上限 ${result.limit}"
                notifyChanged(module)
                return StartResult.ClasspathTooLong(result.length, result.limit, result.sample)
            }
        }
        val javaExecutable = javaExecutable(module)
            ?: return failure(holder, "未找到可用的 JDK，请在 Project Structure 中配置模块 SDK")

        if (verifyCompiled) {
            // 只校验生产输出；测试输出中的同名类不能证明主类可启动。
            val outputDirs = listOfNotNull(CompilerPaths.getModuleOutputPath(module, false)).map(::File)
            if (!MainClassOutputCheck.isCompiled(outputDirs, classpath.entries, mainClass)) {
                val message = if (outputDirs.none { it.isDirectory }) {
                    "主类 $mainClass 尚未编译：模块没有编译输出，请先 Build Project 再启动调试容器"
                } else {
                    "主类 $mainClass 不在模块输出目录中，可能尚未编译；请先 Build Project 后重试"
                }
                return failure(holder, message)
            }
        }

        holder.state = State.STARTING
        notifyChanged(module)
        val spec = ContainerLauncher.Spec(
            javaExecutable = javaExecutable,
            classpath = classpath.entries,
            mainClass = mainClass,
            launcherJar = launcherJar,
            programArgs = holder.programArgs,
            workingDirectory = File(project.basePath ?: moduleContentRoot(module) ?: ".")
        )
        return try {
            holder.handle = launcher.launch(spec, holder.logs)
            holder.state = State.READY
            holder.failure = null
            CompilationWatcher.getInstance(project).watch(module)
            notifyChanged(module)
            StartResult.Ready(snapshot(holder))
        } catch (e: ContainerStartException) {
            failure(holder, e.message ?: "容器启动失败")
        } catch (e: Throwable) {
            failure(holder, e.message ?: e.javaClass.name)
        }
    }

    private fun stopLocked(holder: Holder) {
        val handle = holder.handle
        if (handle == null && holder.state == State.STOPPED) {
            return
        }
        holder.state = State.STOPPING
        notifyChanged(holder.module)
        runCatching { handle?.close() }
        holder.handle = null
        CompilationWatcher.getInstance(project).unwatch(holder.module)
        holder.state = State.STOPPED
        notifyChanged(holder.module)
    }

    private fun onCompiled(module: Module) {
        val holder = holders[module] ?: return
        if (!holder.autoRestartEnabled || !holder.guard.canAutoRestart()) {
            return
        }
        if (holder.state != State.READY && holder.state != State.FAILED) {
            return
        }
        AppExecutorUtil.getAppExecutorService().execute { restartAutomatically(holder) }
    }

    /**
     * 编译触发的自动重启。
     *
     * 结果计入熔断器：连续 [AutoRestartGuard.MAX_CONSECUTIVE_FAILURES] 次失败后
     * 状态转为 [State.SUSPENDED]，不再响应编译事件；用户手动启动会清零。
     */
    private fun restartAutomatically(holder: Holder) {
        holder.lock.withLock {
            if (holder.state != State.READY && holder.state != State.FAILED) {
                return
            }
            if (!holder.autoRestartEnabled || !holder.guard.canAutoRestart()) {
                return
            }
            holder.state = State.RESTARTING
            notifyChanged(holder.module)
            runCatching { holder.handle?.close() }
            holder.handle = null
            CompilationWatcher.getInstance(project).unwatch(holder.module)

            val result = startLocked(holder, verifyCompiled = true)
            // 自动重启失败后继续监听，下一次构建仍可重试
            CompilationWatcher.getInstance(project).watch(holder.module)
            val allowed = holder.guard.record(result is StartResult.Ready)
            if (result !is StartResult.Ready && !allowed) {
                holder.state = State.SUSPENDED
                holder.failure = "连续 ${AutoRestartGuard.MAX_CONSECUTIVE_FAILURES} 次自动重启失败，已停止自动重启"
                notifyChanged(holder.module)
            }
        }
    }

    private fun resolveMainClass(module: Module, explicit: String?): MainClassResolution {
        if (!explicit.isNullOrBlank()) {
            return MainClassResolution.Ok(explicit)
        }
        return ReadAction.compute<MainClassResolution, RuntimeException> {
            when (val result = BootMainClassLocator.locate(project, module)) {
                is BootMainClassLocator.Result.Found -> {
                    val name = result.mainClass.qualifiedName
                    when {
                        name == null ->
                            MainClassResolution.Missing("主类没有全限定名", emptyList())
                        ModuleUtilCore.findModuleForPsiElement(result.mainClass) != module ->
                            MainClassResolution.Missing(
                                "当前模块没有 Spring Boot 主类，依赖模块中的 $name 不能用于调试",
                                listOf(name)
                            )
                        else -> MainClassResolution.Ok(name)
                    }
                }
                is BootMainClassLocator.Result.Multiple -> {
                    val first = result.candidates.first()
                    val name = first.qualifiedName
                    if (name != null && ModuleUtilCore.findModuleForPsiElement(first) == module) {
                        MainClassResolution.Ok(name)
                    } else {
                        MainClassResolution.Missing(
                            "当前模块没有 Spring Boot 主类，依赖模块中存在多个候选",
                            result.candidates.mapNotNull { it.qualifiedName }
                        )
                    }
                }
                BootMainClassLocator.Result.NotFound ->
                    MainClassResolution.Missing("未找到带 @SpringBootApplication 的主类", emptyList())
                BootMainClassLocator.Result.IndexNotReady ->
                    MainClassResolution.Dumb("索引尚未就绪，请稍后重试")
            }
        }
    }

    private fun moduleContentRoot(module: Module): String? =
        ModuleRootManager.getInstance(module).contentRoots.firstOrNull()?.path

    private fun javaExecutable(module: Module): String? {
        val sdkHome = ModuleRootManager.getInstance(module).sdk?.homePath
            ?: ProjectRootManager.getInstance(project).projectSdk?.homePath
        javaIn(sdkHome)?.let { return it }
        return javaIn(System.getProperty("java.home"))
    }

    private fun javaIn(home: String?): String? {
        if (home.isNullOrBlank()) {
            return null
        }
        val name = if (SystemInfo.isWindows) "java.exe" else "java"
        val file = File(File(home, "bin"), name)
        return if (file.isFile) file.absolutePath else null
    }

    private fun failure(holder: Holder, message: String): StartResult.Failed {
        holder.state = State.FAILED
        holder.failure = message
        notifyChanged(holder.module)
        return StartResult.Failed(message, holder.logs.tail())
    }

    private fun holderOf(module: Module): Holder = holders.computeIfAbsent(module) { Holder(it) }

    private fun snapshot(holder: Holder): Snapshot = Snapshot(
        moduleName = holder.module.name,
        state = holder.state,
        mainClass = holder.mainClass,
        info = holder.handle?.info,
        failure = holder.failure,
        logs = holder.logs.tail(SNAPSHOT_LOG_LINES),
        autoRestartSuspended = !holder.guard.canAutoRestart()
    )

    private fun notifyChanged(module: Module) {
        listeners.forEach { listener -> runCatching { listener(module) } }
    }

    companion object {
        /**
         * 默认不启动内嵌 Web 容器：通过 `spring.main.web-application-type=none`
         * 让 Spring Boot 以非 Web 方式启动，避免调试期间占用端口、启动 Tomcat。
         */
        val DEFAULT_PROGRAM_ARGS: List<String> = programArgsFor(webServer = false)

        /**
         * 调试容器的启动参数。
         *
         * 需要 Servlet 上下文（`HttpServletRequest` 参数、`@RequestScope` bean 等）时
         * 传 `true` 启动内嵌 Web 服务器，端口固定为 0（随机端口）。
         */
        fun programArgsFor(webServer: Boolean): List<String> = if (webServer) {
            listOf("--server.port=0")
        } else {
            listOf("--spring.main.web-application-type=none")
        }

        private const val SNAPSHOT_LOG_LINES = 200

        fun getInstance(project: Project): DebugContainerService = project.service()
    }
}
