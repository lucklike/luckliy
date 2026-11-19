package com.luckyframework.idea.debug.container

import com.luckyframework.idea.debug.protocol.DebugProtocol
import com.luckyframework.idea.debug.protocol.DebugRequest
import com.luckyframework.idea.debug.protocol.DebugResponse
import com.luckyframework.idea.debug.rpc.DebugRpcClient
import com.luckyframework.idea.debug.rpc.DebugTimeoutException
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread

/**
 * 启动并连接 launcher 子进程。
 *
 * 只依赖 JDK 与调试协议，不访问 IntelliJ 项目模型；
 * [DebugContainerService] 负责把 Module 解析成 [Spec]。
 * 这样真实 Boot 项目的进程级验证可以复用同一套启动逻辑。
 */
class ContainerLauncher {

    data class Spec(
        val javaExecutable: String,
        val classpath: List<String>,
        val mainClass: String,
        val launcherJar: Path,
        val programArgs: List<String> = emptyList(),
        val jvmArgs: List<String> = emptyList(),
        val workingDirectory: File? = null,
        val environment: Map<String, String> = emptyMap(),
        val handshakeTimeoutMillis: Long = DEFAULT_HANDSHAKE_TIMEOUT_MILLIS,
        val readyTimeoutMillis: Long = DEFAULT_READY_TIMEOUT_MILLIS
    ) {
        fun command(): List<String> = buildList {
            add(javaExecutable)
            add("-D${DebugProtocol.MAIN_CLASS_PROPERTY}=$mainClass")
            add("-Dfile.encoding=UTF-8")
            addAll(jvmArgs)
            add("-cp")
            add(classpath.joinToString(File.pathSeparator))
            add(LAUNCHER_MAIN_CLASS)
            addAll(programArgs)
        }
    }

    data class ContainerInfo(
        val port: Int,
        val pid: Long,
        val mainClass: String,
        val jdkVersion: String,
        val launcherVersion: String,
        val protocolVersion: Int,
        val beanPresent: Boolean
    )

    class Started(
        val spec: Spec,
        val process: Process,
        val client: DebugRpcClient,
        val logs: DebugLogBuffer,
        val info: ContainerInfo
    ) : AutoCloseable {

        /** 先请求 shutdown，再兜底强制结束进程。 */
        override fun close() {
            runCatching {
                client.request(
                    DebugRequest(DebugRpcClient.newId(), DebugProtocol.OP_SHUTDOWN),
                    SHUTDOWN_TIMEOUT_MILLIS
                )
            }
            runCatching { client.close() }
            runCatching {
                if (!process.waitFor(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly()
                    process.waitFor(FORCE_KILL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                }
            }
        }
    }

    fun launch(spec: Spec, logs: DebugLogBuffer = DebugLogBuffer()): Started {
        val process = startProcess(spec)
        val stdout = BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8))
        val stderr = BufferedReader(InputStreamReader(process.errorStream, StandardCharsets.UTF_8))
        drain(stderr, logs, "LuckyDebugStderr", stderr = true)
        val port = readHandshake(stdout, logs, process, spec.handshakeTimeoutMillis)
        drain(stdout, logs, "LuckyDebugStdout", stderr = false)

        val client = try {
            DebugRpcClient(LOOPBACK, port, logListener(logs))
        } catch (e: Throwable) {
            destroy(process)
            throw ContainerStartException("连接 launcher 失败（端口 $port）：${e.message}", logs.tail(), e)
        }

        return try {
            val hello = awaitReady(client, process, logs, spec.readyTimeoutMillis)
            Started(
                spec = spec,
                process = process,
                client = client,
                logs = logs,
                info = ContainerInfo(
                    port = port,
                    pid = process.pid(),
                    mainClass = hello.payloadString("mainClass") ?: spec.mainClass,
                    jdkVersion = hello.payloadString("jdkVersion") ?: "unknown",
                    launcherVersion = hello.payloadString("launcherVersion") ?: "unknown",
                    protocolVersion = hello.payloadString("protocolVersion")?.toIntOrNull() ?: DebugProtocol.VERSION,
                    beanPresent = hello.payloadBoolean("beanPresent") ?: false
                )
            )
        } catch (e: ContainerStartException) {
            runCatching { client.close() }
            destroy(process)
            throw ContainerStartException(e.message ?: "容器启动失败", logs.tail(), e)
        } catch (e: Throwable) {
            runCatching { client.close() }
            destroy(process)
            throw ContainerStartException("容器启动失败：${e.message}", logs.tail(), e)
        }
    }

    private fun startProcess(spec: Spec): Process = try {
        ProcessBuilder(spec.command())
            .directory(spec.workingDirectory)
            .apply { environment().putAll(spec.environment) }
            .start()
    } catch (e: Throwable) {
        throw ContainerStartException(
            "无法启动 Java 进程（${spec.javaExecutable}）：${e.message}",
            emptyList(),
            e
        )
    }

    /**
     * 等待 launcher 输出 `LUCKY_DEBUG_PORT=` 握手行。
     *
     * 握手行之前的输出全部进日志：launcher 自身的启动错误（类缺失、jar 损坏）
     * 通常就打印在这里。
     */
    private fun readHandshake(
        reader: BufferedReader,
        logs: DebugLogBuffer,
        process: Process,
        timeoutMillis: Long
    ): Int {
        val future = CompletableFuture<Int>()
        thread(name = "LuckyDebugHandshake", isDaemon = true) {
            try {
                while (true) {
                    val line = reader.readLine()
                    if (line == null) {
                        future.completeExceptionally(IllegalStateException("launcher 未输出端口握手行"))
                        return@thread
                    }
                    logs.append(line)
                    if (!line.startsWith(DebugProtocol.PORT_PREFIX)) {
                        continue
                    }
                    val port = line.removePrefix(DebugProtocol.PORT_PREFIX).trim().toIntOrNull()
                    if (port != null && port > 0) {
                        future.complete(port)
                    } else {
                        future.completeExceptionally(IllegalStateException("端口握手格式非法：$line"))
                    }
                    return@thread
                }
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }
        return try {
            future.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            destroy(process)
            throw ContainerStartException("等待 launcher 端口握手超时（${timeoutMillis}ms）", logs.tail(), e)
        } catch (e: ExecutionException) {
            destroy(process)
            val cause = e.cause ?: e
            throw ContainerStartException("launcher 启动失败：${cause.message}", logs.tail(), cause)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            destroy(process)
            throw ContainerStartException("等待端口握手被中断", logs.tail(), e)
        }
    }

    /**
     * 轮询 hello 直到容器 READY。
     *
     * hello 只报告状态；失败原因从 status 的 lastError 读取，
     * 避免 hello 在每次轮询时携带完整堆栈。
     */
    private fun awaitReady(
        client: DebugRpcClient,
        process: Process,
        logs: DebugLogBuffer,
        timeoutMillis: Long
    ): DebugResponse {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var lastState = DebugProtocol.STATE_STARTING
        while (System.currentTimeMillis() < deadline) {
            // 启动器异常退出时 RPC 线程可能仍能短暂读到 STARTING；
            // 必须立即失败，否则界面会一直停在“Starting container”。
            if (!process.isAlive) {
                val exitCode = runCatching { process.exitValue() }.getOrNull()
                throw ContainerStartException(
                    "launcher 进程已退出（exit code ${exitCode ?: "unknown"}），详见日志",
                    logs.tail()
                )
            }
            val hello = try {
                client.request(
                    DebugRequest(DebugRpcClient.newId(), DebugProtocol.OP_HELLO),
                    HELLO_TIMEOUT_MILLIS
                )
            } catch (e: DebugTimeoutException) {
                sleepQuietly()
                continue
            }
            if (!hello.ok) {
                throw ContainerStartException(
                    "launcher 拒绝 hello：${hello.errorCode} ${hello.errorMessage ?: ""}",
                    emptyList()
                )
            }
            lastState = hello.payloadString("containerState") ?: DebugProtocol.STATE_STARTING
            when (lastState) {
                DebugProtocol.STATE_READY -> return hello
                DebugProtocol.STATE_FAILED -> {
                    val reason = lastError(client) ?: "容器启动失败，详见日志"
                    throw ContainerStartException(reason, emptyList())
                }
                else -> sleepQuietly()
            }
        }
        throw ContainerStartException("等待容器就绪超时（${timeoutMillis}ms），最后状态：$lastState", emptyList())
    }

    private fun lastError(client: DebugRpcClient): String? = runCatching {
        val response = client.request(
            DebugRequest(DebugRpcClient.newId(), DebugProtocol.OP_STATUS),
            HELLO_TIMEOUT_MILLIS
        )
        response.payloadString("lastError")?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun drain(reader: BufferedReader, logs: DebugLogBuffer, name: String, stderr: Boolean) {
        thread(name = name, isDaemon = true) {
            try {
                while (true) {
                    val line = reader.readLine() ?: return@thread
                    logs.append(line, stderr)
                }
            } catch (e: Throwable) {
                // 进程退出时流被关闭，属于正常路径
            }
        }
    }

    private fun logListener(logs: DebugLogBuffer) = object : DebugRpcClient.Listener {
        override fun onDisconnected(cause: Throwable?) {
            // 主动关闭不再触发回调；这里只记录 launcher 侧的异常断开
            logs.append("RPC 连接断开：${cause?.message ?: "对端关闭"}", stderr = true)
        }
    }

    private fun sleepQuietly() {
        try {
            Thread.sleep(POLL_INTERVAL_MILLIS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun destroy(process: Process) {
        runCatching { process.destroyForcibly() }
    }

    companion object {
        const val LAUNCHER_MAIN_CLASS = "com.luckyframework.debug.LuckyDebugLauncher"

        const val DEFAULT_HANDSHAKE_TIMEOUT_MILLIS = 30_000L
        const val DEFAULT_READY_TIMEOUT_MILLIS = 180_000L
        const val SHUTDOWN_TIMEOUT_MILLIS = 8_000L
        const val FORCE_KILL_TIMEOUT_MILLIS = 3_000L
        const val HELLO_TIMEOUT_MILLIS = 10_000L
        const val POLL_INTERVAL_MILLIS = 250L

        private const val LOOPBACK = "127.0.0.1"
    }
}

/** 子进程未能启动或容器启动失败；[logTail] 是失败时最后一段输出。 */
class ContainerStartException(
    message: String,
    val logTail: List<String>,
    cause: Throwable? = null
) : RuntimeException(message, cause)
