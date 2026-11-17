package com.luckyframework.idea.spel.symbol

import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * 生命周期可见性映射（design D6）。
 *
 * 「注解属性 → 阶段」直接使用注解元数据表；这里维护「内置变量 → 可用阶段」，
 * 以及阶段比较规则。`METHOD`/`UNKNOWN` 视为不参与过滤，避免把合法表达式误报。
 */
object LifecycleTable {

    /** 响应相关变量，从 `RESPONSE_INIT` 起可用。 */
    private val RESPONSE_VARIABLES = setOf(
        "\$resp\$",
        "\$streamBody\$",
        "\$byteBody\$",
        "\$stringBody\$",
        "\$jsonBody\$",
        "\$xmlBody\$",
        "\$javaBody\$",
        "\$body\$",
        "\$status\$",
        "\$contentType\$",
        "\$contentLength\$",
        "\$respHeader\$",
        "\$respCookie\$",
        "\$redirectChain\$"
    )

    /** 异常阶段变量。 */
    private val THROWABLE_VARIABLES = setOf("\$err\$", "\$rootErr\$")

    /** 方法返回值阶段变量。 */
    private val RESULT_VARIABLES = setOf("\$result\$")

    /**
     * 内置变量从哪个阶段开始可用；未登记的内置变量按请求阶段处理。
     */
    fun availableFrom(variableName: String): SpelLifecycle = when (variableName) {
        in THROWABLE_VARIABLES -> SpelLifecycle.THROWABLE
        in RESULT_VARIABLES -> SpelLifecycle.METHOD_RESULT
        in RESPONSE_VARIABLES -> SpelLifecycle.RESPONSE_INIT
        else -> SpelLifecycle.REQUEST_INIT
    }

    /**
     * `METHOD`（未声明阶段）与 `UNKNOWN` 不参与阶段过滤。
     */
    fun isStageFilterable(stage: SpelLifecycle): Boolean =
        stage != SpelLifecycle.METHOD && stage != SpelLifecycle.UNKNOWN

    /**
     * 当前阶段 [current] 是否已经能看到从 [availableFrom] 起可用的符号。
     */
    fun isAvailable(current: SpelLifecycle, availableFrom: SpelLifecycle): Boolean {
        if (!isStageFilterable(current) || !isStageFilterable(availableFrom)) {
            return true
        }
        return rank(availableFrom) <= rank(current)
    }

    /**
     * 生命周期先后顺序：以框架 16 个阶段的声明顺序为基础，只把
     * `RETRY` 移到 `RESPONSE` 之后——重试发生在拿到响应/抛出异常之后，
     * 重试条件可以读取响应变量，但看不到异常与返回值。
     */
    private fun rank(stage: SpelLifecycle): Int = when (stage) {
        SpelLifecycle.RETRY -> SpelLifecycle.RESPONSE.ordinal + 1
        else -> stage.ordinal
    }
}
