package com.luckyframework.idea.spel.environment

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.TextOccurenceProcessor
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.reference.SpelDeclaration
import com.luckyframework.idea.spel.reference.SpelReference

/**
 * `${key}` 在 Java 注解属性中的使用位置。
 *
 * 配置侧的反向跳转（配置键 → 注解）与配置键重命名共用这段逻辑：
 * 平台的词索引把 `api.host` 拆成多个词，无法整段命中，这里取键名中
 * 最长的一段作为搜索词，再逐条解析字面量里的占位符做精确匹配。
 *
 * 只处理 Java 字面量：需求中的反向跳转与重命名目标都是注解里的 `${...}`，
 * YAML 值中的占位符不参与，避免在未安装 YAML 插件时加载相关 PSI。
 */
object SpringPlaceholderUsageSearch {

    /** 配置键在注解中的全部使用位置，按搜索顺序返回。 */
    fun declarationsOf(project: Project, key: String): List<SpelDeclaration> {
        val result = ArrayList<SpelDeclaration>()
        processReferences(project, key, GlobalSearchScope.projectScope(project)) { reference ->
            val literal = reference.element as? PsiLiteralExpression ?: return@processReferences true
            val file = literal.containingFile
            val line = StringUtil.offsetToLineNumber(file.text, literal.textOffset) + 1
            result += SpelDeclaration(
                project = project,
                name = key,
                kindLabel = "注解属性",
                typeLabel = "${file.name}:$line",
                description = "值：${previewOf(literal.text)}",
                target = literal,
                sourceRange = reference.rangeInElement,
                anchor = literal
            )
            true
        }
        return result
    }

    /**
     * 按配置键精确匹配 `${key}` 引用。
     *
     * 搜索词只是粗筛，`placeholderKeyOf` 负责排除同名的 SpEL `#{}` 引用
     * 与包含该片段的其他键（`${api.host.name}`）。
     */
    fun processReferences(
        project: Project,
        key: String,
        scope: SearchScope,
        consumer: (SpelReference) -> Boolean
    ): Boolean {
        if (key.isEmpty() || !project.isInitialized) {
            return true
        }
        val word = searchWordOf(key)
        if (word.isEmpty()) {
            return true
        }
        val processor = object : TextOccurenceProcessor {
            // 同一个字面量可能因词索引按上下文重复命中，按字面量去重
            private val processed = HashSet<PsiElement>()

            override fun execute(element: PsiElement, offsetInElement: Int): Boolean {
                val literal = element as? PsiLiteralExpression
                    ?: PsiTreeUtil.getParentOfType(element, PsiLiteralExpression::class.java, false)
                    ?: return true
                if (!processed.add(literal)) {
                    return true
                }
                for (reference in literal.references) {
                    if (reference !is SpelReference || placeholderKeyOf(reference) != key) {
                        continue
                    }
                    if (!consumer(reference)) {
                        return false
                    }
                }
                return true
            }
        }
        return processUnderProgress {
            PsiSearchHelper.getInstance(project).processElementsWithWord(
                processor,
                scope,
                word,
                UsageSearchContext.ANY,
                true
            )
        }
    }

    /**
     * 词索引搜索要求在 progress indicator 下执行，而跳转处理器（Ctrl+hover/Ctrl+click）
     * 运行在裸 read action 中，平台会报 "Must be executed under progress indicator"。
     * 缺失时补一个空 indicator；已有 indicator 时直接执行，保留外层任务的取消能力。
     */
    private fun processUnderProgress(search: () -> Boolean): Boolean {
        val manager = ProgressManager.getInstance()
        if (manager.progressIndicator != null) {
            return search()
        }
        var result = true
        manager.runProcess({ result = search() }, EmptyProgressIndicator())
        return result
    }

    /**
     * 引用文本属于 `${...}` 时返回键名，否则返回 null。
     *
     * SpEL 的 `#name` 引用文本可能恰好与配置键同名，必须按前导 `${` 排除。
     */
    fun placeholderKeyOf(reference: PsiReference): String? {
        val element = reference.element
        val text = element.text
        val range = reference.rangeInElement
        if (range.startOffset < 2 || range.endOffset > text.length) {
            return null
        }
        if (text[range.startOffset - 1] != '{' || text[range.startOffset - 2] != '$') {
            return null
        }
        return text.substring(range.startOffset, range.endOffset)
    }

    /** 按词索引检索时使用的片段：点分键整体不是词，取最长的一段降低误命中。 */
    fun searchWordOf(key: String): String =
        key.split('.', '-', '_', '/', ':')
            .filter { it.isNotEmpty() }
            .maxByOrNull { it.length }
            ?: key

    private fun previewOf(value: String): String {
        val singleLine = value.replace('\n', ' ').replace('\r', ' ').trim()
        return if (singleLine.length <= PREVIEW_LIMIT) {
            singleLine
        } else {
            singleLine.take(PREVIEW_LIMIT) + "…"
        }
    }

    private const val PREVIEW_LIMIT = 60
}
