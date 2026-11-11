package com.luckyframework.idea.spel.evaluator

import com.intellij.openapi.util.TextRange
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.model.SpelFragment
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 受控求值入口（design D11，spec 15.3/15.4）。
 *
 * 负责三件事：
 * - 语法预检与未提供变量的识别（用插件自身的扫描器/tokenizer，不依赖 Spring）；
 * - 在后台 daemon 线程执行并限制时间，超时返回 [EvaluatorResult.Timeout]；
 * - 把引擎异常归类为 [EvaluatorResult.Forbidden] 或 [EvaluatorResult.Failure]。
 *
 * 本类不接触 PSI/文档，天然保证「失败不修改源文件」。
 */
class SpelEvaluator(
    private val engine: EvaluatorEngine,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
) {

    fun evaluate(text: String, variables: Map<String, Any?> = emptyMap()): EvaluatorResult {
        val expression = expressionOf(text) ?: return EvaluatorResult.Failure("未找到可求值的 SpEL 表达式")
        if (!expression.closed) {
            return EvaluatorResult.Failure("表达式未闭合，缺少匹配的 '}'")
        }
        expression.problems.firstOrNull()?.let { problem ->
            return EvaluatorResult.Failure(problem.message)
        }

        val missing = SpelEvaluationInput.referencedVariables(expression.content)
            .firstOrNull { it !in variables }
        if (missing != null) {
            return EvaluatorResult.MissingVariable("#$missing")
        }

        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "SpelEvaluator").apply { isDaemon = true }
        }
        val future = executor.submit<Any?> { engine.evaluate(expression.content, variables) }
        return try {
            val value = future.get(timeoutMillis, TimeUnit.MILLISECONDS)
            EvaluatorResult.Success(SpelEvaluationFormatter.value(value), typeNameOf(value))
        } catch (e: TimeoutException) {
            future.cancel(true)
            EvaluatorResult.Timeout((timeoutMillis + 999) / 1000)
        } catch (e: ExecutionException) {
            classify(e.cause ?: e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            EvaluatorResult.Failure("求值被中断")
        } finally {
            executor.shutdownNow()
        }
    }

    /**
     * 字面量文本（可含引号与多个片段）优先按 `#{...}` 扫描；
     * 不含定界符时按裸表达式处理，方便测试与后续复用。
     */
    private fun expressionOf(text: String): SpelFragment? {
        val fragments = SpelScanner().scanLiteral(text)
        if (fragments.isNotEmpty()) {
            return fragments.maxByOrNull { it.content.length }
        }
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        return SpelFragment(
            textRange = TextRange(0, trimmed.length),
            expressionRange = TextRange(0, trimmed.length),
            content = trimmed,
            closed = true
        )
    }

    private fun classify(error: Throwable): EvaluatorResult {
        findCause<EvaluatorForbiddenException>(error)?.let { forbidden ->
            return EvaluatorResult.Forbidden(forbidden.message ?: "操作被禁止")
        }
        findCause<EvaluatorUnavailableException>(error)?.let { unavailable ->
            return EvaluatorResult.Unavailable(unavailable.message ?: "无法加载 spring-expression")
        }
        val message = messageOf(error)
        val position = positionOf(error)
        val text = if (position != null && !message.contains(position.toString())) {
            "$message（位置 $position）"
        } else {
            message
        }
        return EvaluatorResult.Failure(text)
    }

    private fun messageOf(error: Throwable): String {
        var current: Throwable? = error
        while (current != null) {
            if (current !is InvocationTargetException &&
                current !is ExecutionException &&
                !current.message.isNullOrBlank()
            ) {
                return current.message!!
            }
            current = current.cause
        }
        return error.javaClass.simpleName
    }

    /** Spring 的解析异常带 `getPosition()`；缺失时沿 cause 链继续找。 */
    private fun positionOf(error: Throwable): Int? {
        var current: Throwable? = error
        while (current != null) {
            val position = runCatching {
                current.javaClass.getMethod("getPosition").invoke(current) as? Int
            }.getOrNull()
            if (position != null && position > 0) {
                return position
            }
            current = current.cause?.takeIf { it !== current }
        }
        return null
    }

    private inline fun <reified T : Throwable> findCause(error: Throwable): T? {
        var current: Throwable? = error
        while (current != null) {
            if (current is T) {
                return current
            }
            val cause = current.cause
            current = if (cause === current) null else cause
        }
        return null
    }

    private fun typeNameOf(value: Any?): String = value?.javaClass?.simpleName ?: "null"

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 3000L
    }
}

/** 引擎加载失败（类缺失/版本不兼容）时抛出，由 [SpelEvaluator] 归类。 */
class EvaluatorUnavailableException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
