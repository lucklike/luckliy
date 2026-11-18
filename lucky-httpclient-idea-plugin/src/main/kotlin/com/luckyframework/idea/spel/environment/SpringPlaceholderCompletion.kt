package com.luckyframework.idea.spel.environment

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IconUtil
import com.intellij.util.ProcessingContext
import com.luckyframework.idea.spel.completion.SpelOrderedElement
import com.luckyframework.idea.spel.completion.SpelPrefixMatcher
import com.luckyframework.idea.spel.completion.currentWordProvider
import com.luckyframework.idea.spel.completion.spelSorter
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver

/**
 * 环境变量键补全的统一候选填充。
 *
 * 每个配置文件定义一条候选，展示为：
 * `[属性图标] key=value  [文件图标]application-dev.yaml`。
 * 同一键在多个 profile 文件中定义时逐条列出，与 Spring 配置文件
 * 实际上多处存在的事实一致，客户端选择时可以看清每个文件的取值。
 *
 * 排序与 SpEL 符号候选保持一致：以输入开头的键在前（组 1），仅包含输入的
 * 键在后（组 2），同组内按不区分大小写的字母序；前缀匹配器放宽为包含匹配，
 * 否则「仅包含输入」的键会被平台直接丢弃。
 */
internal object SpringPlaceholderCompletionSupport {

    fun addCandidates(
        model: SpringEnvironmentModel,
        keyPrefix: String,
        keyStart: Int,
        result: CompletionResultSet,
        editor: Editor? = null
    ) {
        val target = result
            .withPrefixMatcher(SpelPrefixMatcher(SpelPrefixMatcher.wordOf(keyPrefix), keyPrefix))
            .withRelevanceSorter(spelSorter(currentWordProvider(editor)))
        // 键名中的 `.` 也是输入的一部分，出现新字符时必须重新过滤候选
        target.restartCompletionOnAnyPrefixChange()
        for (key in model.allKeys()) {
            if (key.isEmpty()) {
                continue
            }
            for (definition in model.definitionsOf(key)) {
                target.addElement(
                    SpelOrderedElement(
                        SpringPlaceholderLookupElement(
                            lookupOf(model, key, definition, keyStart),
                            definition.file.path
                        ),
                        key,
                        CONFIG_LAYER
                    )
                )
            }
        }
    }

    private fun lookupOf(
        model: SpringEnvironmentModel,
        key: String,
        definition: SpringPropertyDefinition,
        keyStart: Int
    ): LookupElement {
        val value = valuePreviewOf(model, definition)
        return LookupElementBuilder.create(key)
            .withIcon(AllIcons.Nodes.Property)
            .withTailText(value?.let { "=$it" }, true)
            .withTypeText(
                definition.fileName,
                IconUtil.getIcon(definition.file, 0, null),
                true
            )
            // 平台只替换光标前的标识符片段，键名含 `.` 时 `${api.` 选择 `api.host`
            // 会得到 `api.api.host`；这里按 `${` 与 `}`/`:` 之间的完整键名重写
            .withInsertHandler { context, _ ->
                val document = context.document
                val end = keyEndIn(document.charsSequence, keyStart, context.tailOffset)
                document.replaceString(keyStart, end, key)
                context.editor.caretModel.moveToOffset(keyStart + key.length)
            }
    }

    /** 值预览：字面值直接展示，含嵌套占位符时展示解析后的值。 */
    private fun valuePreviewOf(
        model: SpringEnvironmentModel,
        definition: SpringPropertyDefinition
    ): String? {
        val value = if (definition.value.contains("\${")) {
            model.resolveText(definition.value) ?: definition.value
        } else {
            definition.value
        }
        return value.takeIf { it.isNotBlank() }?.let { previewOf(it) }
    }

    /**
     * 从键名起点找到键名终点：`}`、`:`、引号或行尾之前。
     *
     * 补全副本的光标处会插入占位标识符，因此不能用副本里的键名长度计算终点。
     */
    private fun keyEndIn(text: CharSequence, keyStart: Int, insertedEnd: Int): Int {
        var index = keyStart
        while (index < text.length) {
            when (text[index]) {
                '}', ':', '"', '\'', '\n', '\r' -> break
            }
            index++
        }
        // 插入文本本身属于待替换范围
        return maxOf(index, insertedEnd.coerceAtMost(text.length))
    }

    private fun previewOf(value: String): String {
        val singleLine = value.replace('\n', ' ').replace('\r', ' ').trim()
        return if (singleLine.length <= PREVIEW_LIMIT) {
            singleLine
        } else {
            singleLine.take(PREVIEW_LIMIT) + "…"
        }
    }

    private const val PREVIEW_LIMIT = 60

    /** 空输入时的分层键：配置键排在导入符号之后、内置符号之前。 */
    private const val CONFIG_LAYER = "01"
}

/**
 * 标记环境变量候选，让 [com.luckyframework.idea.spel.completion.SpelCharFilter] 识别。
 *
 * 同一键在多个 profile 文件中定义时会产生多条候选，平台默认按
 * `LookupElementBuilder` 的相等性（只看键名与插入器）去重，必须把文件路径
 * 纳入相等性判断，否则列表只留一条并被自动插入。
 */
class SpringPlaceholderLookupElement(
    delegate: LookupElement,
    private val source: String
) : LookupElementDecorator<LookupElement>(delegate) {

    override fun equals(other: Any?): Boolean =
        other is SpringPlaceholderLookupElement &&
            source == other.source &&
            lookupString == other.lookupString

    override fun hashCode(): Int = 31 * lookupString.hashCode() + source.hashCode()
}

/**
 * Java 字符串字面量中的 `${key}` 补全。
 *
 * 只处理标注了 `@SpelAttribute` 的属性；光标必须位于键名范围内，
 * 默认值位置交给普通文本输入。
 */
class SpringPlaceholderCompletionProvider : CompletionProvider<CompletionParameters>() {

    private val resolver = SpelAnnotationResolver()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val literal = PsiTreeUtil.getParentOfType(
            parameters.position,
            PsiLiteralExpression::class.java,
            false
        ) ?: return
        if (!literal.text.contains("\${") || !resolver.isSpelLiteral(literal)) {
            return
        }
        val service = SpringEnvironmentService.getInstance(literal.project)
        if (!service.isEnabledForCopy(literal, parameters.originalFile)) {
            return
        }
        val offsetInLiteral = parameters.offset - literal.textRange.startOffset
        val placeholder = SpringPlaceholderParser.placeholderAt(literal.text, offsetInLiteral) ?: return
        val model = service.modelForCopy(literal, parameters.originalFile) ?: return
        val keyPrefix = literal.text.substring(placeholder.keyStartOffset, offsetInLiteral)
        SpringPlaceholderCompletionSupport.addCandidates(
            model,
            keyPrefix,
            literal.textRange.startOffset + placeholder.keyStartOffset,
            result,
            parameters.editor
        )
    }
}
