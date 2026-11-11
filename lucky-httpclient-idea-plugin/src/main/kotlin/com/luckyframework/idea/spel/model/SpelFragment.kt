package com.luckyframework.idea.spel.model

import com.intellij.openapi.util.TextRange

/**
 * 一个 `#{...}` 片段。
 *
 * 所有偏移均相对于所在字符串字面量的源码文本（包含起始引号），
 * 加上字面量自身的起始偏移即可得到文件内的绝对偏移。
 *
 * @property textRange 片段整体范围，从 `#` 开始，到 `}` 之后结束
 * @property expressionRange 表达式内容范围，不含 `#{` 与 `}`
 * @property content 去除 Java 转义后的表达式文本
 * @property closed 是否存在匹配的 `}`
 * @property conditional 是否处于 `@if(...):` 前缀之后
 * @property maxDepth `@max(n):` 声明的最大嵌套次数，未声明时为 null
 * @property nested 是否被双反引号包裹（需要嵌套解析）
 * @property children 内容中嵌套的 `#{...}` 片段
 * @property problems 扫描阶段发现的语法问题
 */
data class SpelFragment(
    val textRange: TextRange,
    val expressionRange: TextRange,
    val content: String,
    val closed: Boolean,
    val conditional: Boolean = false,
    val maxDepth: Int? = null,
    val nested: Boolean = false,
    val children: List<SpelFragment> = emptyList(),
    val problems: List<SpelProblem> = emptyList()
)
