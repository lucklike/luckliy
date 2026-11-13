package com.luckyframework.idea.spel.completion

import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator

/**
 * SpEL 补全列表的字符过滤器。
 *
 * 平台的 `DefaultCharFilter` 会把 `,`、`;`、`:`、`=`、空格固定当成
 * [CharFilter.Result.SELECT_ITEM_AND_FINISH_LOOKUP]：`#{fn(a[0` 弹出
 * `$0`/`$$0` 等简写候选后继续输入 `,`，高亮候选会被直接插入，文本变成
 * `#{fn(a[$$0,]}`，而用户只是想写分隔符。
 *
 * 这些字符在 SpEL 中只表达分隔/终结语义，不表示接受候选，因此这里关闭
 * 列表并让字符照常输入；`.` 与 `(`、`[` 保留平台的「接受并继续」体验
 * （成员访问、函数调用、索引）。
 */
class SpelCharFilter : CharFilter() {

    override fun acceptChar(charTyped: Char, prefixLength: Int, lookup: Lookup): Result? {
        if (charTyped !in TERMINATORS) {
            return null
        }
        return if (lookup.contributesSpel()) Result.HIDE_LOOKUP else null
    }

    /** 列表中只有混入 SpEL 候选时才接管，不影响 Java 自身的补全行为。 */
    private fun Lookup.contributesSpel(): Boolean =
        items.any { item ->
            var current: LookupElement? = item
            while (current != null) {
                if (current is SpelOrderedElement) {
                    return true
                }
                current = (current as? LookupElementDecorator<*>)?.delegate
            }
            false
        }

    private companion object {
        /**
         * 只表示分隔/终结的字符；`.`、`(`、`[` 仍交给平台处理，
         * 保持成员访问、函数与索引补全的连续输入体验。
         */
        val TERMINATORS = setOf(',', ';', ':', ')', ']', '}', '=', '*', ' ')
    }
}
