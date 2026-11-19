package com.luckyframework.idea.debug.rpc

import com.google.gson.JsonObject
import com.luckyframework.idea.debug.protocol.DebugProtocol
import com.luckyframework.idea.debug.protocol.DebugRequest
import com.luckyframework.idea.debug.protocol.DebugResponse
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 调试 RPC 客户端。
 *
 * 单连接、按 id 多路复用；读线程只做分发，调用方各自限时等待。
 * 超时只放弃等待，目标方法仍可能在子进程中继续执行。
 */
class DebugRpcClient(
    host: String,
    port: Int,
    private val listener: Listener? = null
) : AutoCloseable {

    interface Listener {
        /** launcher 侧主动断开时回调；本端调用 [close] 不会触发。 */
        fun onDisconnected(cause: Throwable?)
    }

    private val socket = Socket()
    private val pending = ConcurrentHashMap<String, CompletableFuture<DebugResponse>>()
    private val writeLock = Any()
    private val reader: BufferedReader
    private val writer: BufferedWriter

    @Volatile
    private var closed = false

    init {
        socket.connect(InetSocketAddress(host, port), DebugProtocol.CONNECT_TIMEOUT_MILLIS)
        socket.tcpNoDelay = true
        reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
        Thread({ readLoop() }, "LuckyDebugRpcReader").apply {
            isDaemon = true
            start()
        }
    }

    fun request(
        request: DebugRequest,
        timeoutMillis: Long = DebugProtocol.DEFAULT_TIMEOUT_MILLIS
    ): DebugResponse {
        check(!closed) { "调试连接已关闭" }
        val future = CompletableFuture<DebugResponse>()
        pending[request.id] = future
        return try {
            write(request)
            future.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            throw DebugTimeoutException(request.id, timeoutMillis)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("等待响应被中断", e)
        } catch (e: java.util.concurrent.ExecutionException) {
            throw (e.cause as? RuntimeException) ?: IllegalStateException(e.cause ?: e)
        } finally {
            pending.remove(request.id)
        }
    }

    /** 请求 launcher 中断目标调用；只设置中断标志，不保证方法立即停止。 */
    fun cancel(targetId: String) {
        if (closed) {
            return
        }
        val payload = JsonObject().apply { addProperty("targetId", targetId) }
        runCatching { write(DebugRequest(newId(), DebugProtocol.OP_CANCEL, payload)) }
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        runCatching { socket.close() }
        failPending("调试连接已关闭")
    }

    private fun write(request: DebugRequest) {
        val line = request.toJson().toString()
        synchronized(writeLock) {
            writer.write(line)
            writer.write("\n")
            writer.flush()
        }
    }

    private fun readLoop() {
        var cause: Throwable? = null
        try {
            while (!closed) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) {
                    continue
                }
                val response = runCatching { DebugResponse.parse(line) }.getOrNull() ?: continue
                pending.remove(response.id)?.complete(response)
            }
        } catch (e: Throwable) {
            cause = e
        } finally {
            val wasClosed = closed
            closed = true
            failPending("调试连接已断开")
            if (!wasClosed) {
                listener?.onDisconnected(cause)
            }
        }
    }

    private fun failPending(message: String) {
        val error = IllegalStateException(message)
        pending.values.forEach { it.completeExceptionally(error) }
        pending.clear()
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

/** 请求在等待窗口内没有收到响应。 */
class DebugTimeoutException(
    val requestId: String,
    val timeoutMillis: Long
) : RuntimeException("请求超时（${timeoutMillis}ms）：$requestId")
