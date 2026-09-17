package com.luckyframework.httpclient.proxy.async

import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.function.Supplier

/**
 * 基于Kotlin协程模型实现的异步任务执行器
 *
 * 取消语义说明：
 * 1. 取消返回的 [CompletableFuture] 时会尝试取消对应的协程
 * 2. 协程的取消只在挂起点生效，对于纯阻塞的任务（如阻塞式HTTP请求）无法立即中断，任务会继续执行直到结束，最终结果会被丢弃
 * 3. 协程作用域被取消（shutdown/shutdownNow）时，尚未开始执行的任务将直接被取消，其对应的 [CompletableFuture] 会以异常的方式完成，不会永久处于未完成状态
 *
 * @author fukang
 * @version 2.0.0
 * @date 2025/03/07
 */
class KotlinCoroutineAsyncTaskExecutor private constructor(
    private val coroutineScope: CoroutineScope, private val executor: Executor? = null
) : AsyncTaskExecutor {

    companion object {

        private val log: Logger = LoggerFactory.getLogger(KotlinCoroutineAsyncTaskExecutor::class.java)

        private const val COROUTINE_NAME: String = "lucky::coroutine"

        /**
         * 协程异常处理器，统一使用日志记录未捕获的异常
         */
        private val EXCEPTION_HANDLER: CoroutineExceptionHandler = CoroutineExceptionHandler { context, throwable ->
            log.error("An exception occurred while executing the kotlin coroutine async task, coroutine name: '{}'", context[CoroutineName]?.name, throwable)
        }

        /**
         * 使用用户自定义线程池创建执行器
         * @param executor 用户提供的线程池
         * @param concurrency 并发限制（>0 生效）
         */
        @JvmStatic
        @OptIn(ExperimentalCoroutinesApi::class)
        fun createByExecutor(executor: Executor, concurrency: Int): KotlinCoroutineAsyncTaskExecutor {
            var dispatcher = executor.asCoroutineDispatcher()
            if (concurrency > 0) {
                dispatcher = dispatcher.limitedParallelism(concurrency)
            }
            val scope = CoroutineScope(
                dispatcher + SupervisorJob() + CoroutineName(COROUTINE_NAME) + EXCEPTION_HANDLER
            )
            // 存储用户传入的 Executor 引用，用于后续可能的关闭操作
            return KotlinCoroutineAsyncTaskExecutor(scope, executor)
        }

        /**
         * 使用用户自定义线程池创建执行器（无并发限制）
         */
        @JvmStatic
        fun createByExecutor(executor: Executor): KotlinCoroutineAsyncTaskExecutor {
            return createByExecutor(executor, -1)
        }

        /**
         * 使用默认方式（Dispatchers.IO）初始化
         * 注意：Dispatchers.IO 是全局共享的，无法由本实例关闭，
         * 因此 getExecutor() 将返回 null，shutdown()/shutdownNow() 不会关闭线程池，
         * 而是通过取消协程作用域来阻止后续新任务的提交与执行
         */

        @JvmStatic
        @OptIn(ExperimentalCoroutinesApi::class)
        fun createDefault(concurrency: Int): KotlinCoroutineAsyncTaskExecutor {
            val dispatcher = if (concurrency > 0) {
                Dispatchers.IO.limitedParallelism(concurrency)
            } else {
                Dispatchers.IO
            }
            // 使用 SupervisorJob 保证单个任务的失败不会导致整个执行器瘫痪
            val scope = CoroutineScope(
                dispatcher + SupervisorJob() + CoroutineName(COROUTINE_NAME) + EXCEPTION_HANDLER
            )
            // 全局调度器无法安全关闭，返回 null
            return KotlinCoroutineAsyncTaskExecutor(scope, null)
        }

        /**
         * 使用默认方式（Dispatchers.IO）初始化，无并发限制
         */
        @JvmStatic
        fun createDefault(): KotlinCoroutineAsyncTaskExecutor {
            return createDefault(-1)
        }
    }

    override fun execute(command: Runnable) {
        coroutineScope.launch { command.run() }
    }

    override fun <R : Any?> supplyAsync(supplier: Supplier<R>): CompletableFuture<R> {
        val future = CompletableFuture<R>()
        val job = coroutineScope.launch {
            try {
                future.complete(supplier.get())
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }
        // 仅当 future 被外部取消时，才取消对应的协程
        future.whenComplete { _, _ -> if (future.isCancelled) job.cancel() }
        // 兜底处理：协程被取消且任务未来得及执行时，确保 future 不会永久处于未完成状态
        job.invokeOnCompletion { cause ->
            if (cause != null && !future.isDone) {
                future.completeExceptionally(cause)
            }
        }
        return future
    }

    /**
     * 获取底层 Executor
     * - 如果使用 createByExecutor 创建，返回用户提供的线程池
     * - 如果使用 createDefault 创建（基于 Dispatchers.IO），返回 null
     */
    override fun getExecutor(): Executor? {
        return executor
    }

    override fun shutdown() {
        coroutineScope.cancel()
        (executor as? ExecutorService)?.shutdown()
    }

    override fun shutdownNow() {
        coroutineScope.cancel()
        (executor as? ExecutorService)?.shutdownNow()
    }


}