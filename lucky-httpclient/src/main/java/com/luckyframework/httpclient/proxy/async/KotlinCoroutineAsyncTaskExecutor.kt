package com.luckyframework.httpclient.proxy.async

import kotlinx.coroutines.*
import java.lang.Runnable
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
class KotlinCoroutineAsyncTaskExecutor(private val executor: Executor) : AsyncTaskExecutor {

    private val coroutineScope: CoroutineScope = CoroutineScope(executor.asCoroutineDispatcher() + SupervisorJob() + CoroutineName("co"))

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

    override fun getExecutor(): Executor {
        return this.executor
    }
}