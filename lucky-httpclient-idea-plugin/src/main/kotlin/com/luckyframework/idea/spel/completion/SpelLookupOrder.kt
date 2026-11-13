package com.luckyframework.idea.spel.completion

import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementWeigher

/**
 * 携带排序键的补全元素。
 *
 * 排序键直接决定列表顺序，例如 `0` 前缀表示「以输入开头」组、
 * `1` 前缀表示「仅包含输入」组；平台自带的匹配度、历史统计等
 * 权重不再参与排序（见 [SPEL_SORTER]）。
 */
class SpelOrderedElement(
    delegate: LookupElement,
    private val sortKey: String
) : LookupElementDecorator<LookupElement>(delegate) {

    fun sortKey(): String = sortKey
}

private object SpelOrderWeigher : LookupElementWeigher("spelOrder") {

    override fun weigh(element: LookupElement): Comparable<*> {
        var current: LookupElement? = element
        while (current != null) {
            if (current is SpelOrderedElement) {
                return current.sortKey()
            }
            current = (current as? LookupElementDecorator<*>)?.delegate
        }
        return ""
    }
}

/**
 * 只按 [SpelOrderedElement] 排序键排序的 sorter。
 *
 * 默认 sorter 混入匹配度、最近使用等权重，无法保证输入字符后
 * 「前缀命中在前、仅包含在后、同组按字母序」的确定性顺序。
 */
val SPEL_SORTER: CompletionSorter = CompletionSorter.emptySorter().weigh(SpelOrderWeigher)

/**
 * 只保留「名字包含输入」的候选的前缀匹配器。
 *
 * 平台在 `CompletionResult.wrap` 中会用 PrefixMatcher 直接丢弃不匹配的候选，
 * 默认的 camel-hump 匹配只认前缀，导致「包含但不开头」的候选无法进入列表。
 * 这里把过滤放宽为包含匹配（不区分大小写），前缀命中与包含命中的先后顺序
 * 完全由 [SPEL_SORTER] 的排序键决定。
 *
 * @param word 用户输入的单词（不含 `#`）
 * @param prefix 传给平台的 lookup 前缀（`#` 模式带 `#`，其余与 [word] 相同）
 */
class SpelPrefixMatcher(
    private val word: String,
    prefix: String
) : PrefixMatcher(prefix) {

    override fun prefixMatches(name: String): Boolean =
        word.isEmpty() || bareName(name).contains(word, ignoreCase = true)

    override fun isStartMatch(name: String): Boolean =
        bareName(name).startsWith(word, ignoreCase = true)

    override fun cloneWithPrefix(prefix: String): PrefixMatcher =
        SpelPrefixMatcher(wordOf(prefix), prefix)

    companion object {
        /** `#` 模式下的 lookup 文本带 `#`，判断包含关系前先去掉。 */
        fun wordOf(prefix: String): String = prefix.removePrefix("#")

        private fun bareName(name: String): String = name.removePrefix("#")
    }
}
