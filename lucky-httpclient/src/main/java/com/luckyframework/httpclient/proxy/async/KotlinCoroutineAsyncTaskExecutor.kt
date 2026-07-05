package com.luckyframework.httpclient.proxy.async

import kotlinx.coroutines.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

/**
 * 基于Kotlin协程模型实现的异步任务执行器
 *
 * @author fukang
 * @version 2.0.0
 * @date 2025/03/07
 */
class KotlinCoroutineAsyncTaskExecutor private constructor(
    private val coroutineScope: CoroutineScope,
    private val executor: Executor? = null
) : AsyncTaskExecutor {

    companion object {

        private const val COROUTINE_NAME: String = "co"

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
                dispatcher + SupervisorJob() + CoroutineName(COROUTINE_NAME)
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
         * 注意：Dispatchers.IO 是全局共享的，无法由本实例关闭
         * 因此 getExecutor() 将返回 null，shutdown() 调用将不执行任何操作
         */

        @JvmStatic
        @OptIn(ExperimentalCoroutinesApi::class)
        fun createDefault(concurrency: Int): KotlinCoroutineAsyncTaskExecutor {
            val dispatcher = if (concurrency > 0) {
                Dispatchers.IO.limitedParallelism(concurrency)
            } else {
                Dispatchers.IO
            }
            val scope = CoroutineScope(
                dispatcher + CoroutineName(COROUTINE_NAME)
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

    override fun execute(command: Runnable?) {
        coroutineScope.launch { command?.run() }
    }

    override fun <R : Any?> supplyAsync(supplier: Supplier<R>?): CompletableFuture<R> {
        val future = CompletableFuture<R>()
        coroutineScope.launch {
            try {
                val result = supplier?.get()
                future.complete(result)
            } catch (e: Throwable) {  // 修复：改为捕获 Throwable，避免 Error 导致 future 永久挂起
                future.completeExceptionally(e)
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

}