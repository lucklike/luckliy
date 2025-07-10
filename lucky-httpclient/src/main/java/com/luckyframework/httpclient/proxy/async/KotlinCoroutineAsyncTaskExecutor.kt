package com.luckyframework.httpclient.proxy.async

import kotlinx.coroutines.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

/**
 * 基于Kotlin协程模型实现的异步任务执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/6 23:30
 */
class KotlinCoroutineAsyncTaskExecutor(private val coroutineScope: CoroutineScope) : AsyncTaskExecutor {

    companion object {

        private const val COROUTINE_NAME: String = "co"

        /**
         * 使用线程池初始化
         */
        @JvmStatic
        @OptIn(ExperimentalCoroutinesApi::class)
        fun createByExecutor(executor: Executor, concurrency: Int): KotlinCoroutineAsyncTaskExecutor {
            var asCoroutineDispatcher = executor.asCoroutineDispatcher()
            if (concurrency > 0) {
                asCoroutineDispatcher = asCoroutineDispatcher.limitedParallelism(concurrency)
            }
            return KotlinCoroutineAsyncTaskExecutor(
                CoroutineScope(
                    asCoroutineDispatcher + SupervisorJob() + CoroutineName(COROUTINE_NAME)
                )
            )
        }

        /**
         * 使用线程池初始化
         */
        @JvmStatic
        fun createByExecutor(executor: Executor): KotlinCoroutineAsyncTaskExecutor {
            return createByExecutor(executor, -1)
        }

        /**
         * 使用默认方式进行初始化
         */
        @JvmStatic
        @OptIn(ExperimentalCoroutinesApi::class)
        fun createDefault(concurrency: Int): KotlinCoroutineAsyncTaskExecutor {
            val dispatcher: CoroutineDispatcher =
                if (concurrency < 0) Dispatchers.IO else Dispatchers.IO.limitedParallelism(concurrency)
            return KotlinCoroutineAsyncTaskExecutor(
                CoroutineScope(
                    dispatcher + CoroutineName(COROUTINE_NAME)
                )
            )
        }

        /**
         * 使用默认方式进行初始化
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
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getExecutor(): Executor? {
        val currentDispatcher = coroutineScope.coroutineContext[CoroutineDispatcher]
        if (currentDispatcher is ExecutorCoroutineDispatcher) {
            return currentDispatcher.executor
        }
        return null
    }
}