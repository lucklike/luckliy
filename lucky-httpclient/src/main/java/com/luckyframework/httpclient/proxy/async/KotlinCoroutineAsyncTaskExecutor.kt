package com.luckyframework.httpclient.proxy.async

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

class KotlinCoroutineAsyncTaskExecutor(private val executor: Executor) : AsyncTaskExecutor {

    private val coroutineScope: CoroutineScope = CoroutineScope(executor.asCoroutineDispatcher() + SupervisorJob());

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