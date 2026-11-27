package com.luckyframework.idea.spel.completion

import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.codeInsight.lookup.WeighingContext
import com.intellij.openapi.editor.Editor

/**
 * 携带排序依据的补全元素。
 *
 * 名称与作用域分层在添加候选时固定，排序键在每次权重计算时按「当前输入」
 * 重新生成：输入变化后平台只会重算排序权重，不会重新调用补全提供器，
 * 排序必须自己跟随输入，否则列表会停留在旧前缀的分组与顺序上。
 */
class SpelOrderedElement(
    delegate: LookupElement,
    private val name: String,
    private val layer: String,
    /** 输入非空时是否仍按 [layer] 分层：配置键用，标准键优先于导入的自定义键。 */
    private val layerFirst: Boolean = false
) : LookupElementDecorator<LookupElement>(delegate) {

    /**
     * 输入为空时按「作用域分层 + 名称」（就近作用域优先）；
     * 输入非空时按用户规则：以输入开头的（组 0）在前，仅包含输入的（组 1）
     * 在后，同组内按不区分大小写的字母序排列。
     */
    fun sortKey(word: String): String {
        val lower = name.lowercase()
        if (word.isEmpty()) {
            return layer + lower
        }
        val group = if (name.startsWith(word, true)) "0" else "1"
        return if (layerFirst) layer + group + lower else group + lower
    }
}

/**
 * 按 [SpelOrderedElement] 的排序键排序的权重。
 *
 * 输入单词由 [wordProvider] 从编辑器文档实时读取。不能依赖
 * [WeighingContext.itemMatcher]：平台返回的是「添加候选时」注册的前缀匹配器，
 * 不随输入更新，拿到的永远是旧单词，列表会停留在刚弹出时的分层顺序上，
 * 这正是「输入太快时候选顺序不准确」的根因。
 *
 * [isPrefixDependent] 必须为 true：平台默认缓存候选项第一次算出的权重，
 * 输入变化后不再重算；声明前缀相关后，平台在输入变化时丢弃权重缓存并重新
 * 调用双参数 [weigh]，分组与顺序始终跟随最新输入。
 */
private class SpelOrderWeigher(
    private val wordProvider: () -> String
) : LookupElementWeigher("spelOrder") {

    override fun weigh(element: LookupElement): Comparable<*> = keyOf(element)

    override fun weigh(element: LookupElement, context: WeighingContext): Comparable<*> = keyOf(element)

    override fun isPrefixDependent(): Boolean = true

    private fun keyOf(element: LookupElement): Comparable<*> =
        orderedOf(element)?.sortKey(wordProvider()) ?: ""

    private fun orderedOf(element: LookupElement): SpelOrderedElement? {
        var current: LookupElement? = element
        while (current != null) {
            if (current is SpelOrderedElement) {
                return current
            }
            current = (current as? LookupElementDecorator<*>)?.delegate
        }
        return null
    }
}

/**
 * 只按 [SpelOrderedElement] 排序键排序的 sorter。
 *
 * 排序依赖当前输入，必须按补全位置新建：默认 sorter 混入匹配度、最近使用
 * 等权重，无法保证输入字符后「前缀命中在前、仅包含在后、同组按字母序」的
 * 确定性顺序。
 */
fun spelSorter(wordProvider: () -> String): CompletionSorter =
    CompletionSorter.emptySorter().weigh(SpelOrderWeigher(wordProvider))

/**
 * 从编辑器读取光标前的标识符片段，作为排序与分组依据。
 *
 * 只扫描文档，不做完整表达式解析：单词边界与 [SpelCompletionAnalyzer]
 * 一致（`#`/`.`/`[`/`(` 等非标识符字符都是边界）。文档或光标未变化时复用
 * 上次结果，一次排序（数百个候选）只扫描一次。
 */
fun currentWordProvider(editor: Editor?): () -> String {
    var stamp = -1L
    var offset = -1
    var word = ""
    return {
        if (editor == null || editor.isDisposed) {
            ""
        } else {
            val document = editor.document
            val caret = editor.caretModel.offset
            if (document.modificationStamp != stamp || caret != offset) {
                stamp = document.modificationStamp
                offset = caret
                word = wordBefore(document.immutableCharSequence, caret)
            }
            word
        }
    }
}

/** 光标前的标识符片段；`$` 与下划线属于标识符，`#`/`.`/`[`/`@` 是边界。 */
private fun wordBefore(text: CharSequence, caret: Int): String {
    val end = minOf(caret, text.length)
    var start = end
    while (start > 0 && Character.isJavaIdentifierPart(text[start - 1])) {
        start--
    }
    return text.subSequence(start, end).toString()
}

/**
 * 只保留「名字包含输入」的候选的前缀匹配器。
 *
 * 平台在 `CompletionResult.wrap` 中会用 PrefixMatcher 直接丢弃不匹配的候选，
 * 默认的 camel-hump 匹配只认前缀，导致「包含但不开头」的候选无法进入列表。
 * 这里把过滤放宽为包含匹配（不区分大小写），前缀命中与包含命中的先后顺序
 * 完全由 [spelSorter] 的排序键决定。
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
