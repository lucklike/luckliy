package com.luckyframework.idea.spel.environment

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.JBColor
import com.intellij.util.IconUtil
import com.intellij.util.ProcessingContext
import com.luckyframework.idea.spel.completion.SpelOrderedElement
import com.luckyframework.idea.spel.completion.SpelPrefixMatcher
import com.luckyframework.idea.spel.completion.currentWordProvider
import com.luckyframework.idea.spel.completion.spelSorter
import com.luckyframework.idea.spel.documentation.SpelDocumentation
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.reference.SpelDeclaration
import javax.swing.Icon

/**
 * 环境变量键补全的统一候选填充。
 *
 * 每个配置文件定义一条候选，展示为：
 * `[属性图标] key=value  [文件图标]application-dev.yaml`。
 * 同一键在多个 profile 文件中定义时逐条列出，与 Spring 配置文件
 * 实际上多处存在的事实一致，客户端选择时可以看清每个文件的取值。
 *
 * Ultimate 安装 Spring Boot 插件时，候选复用原生 `MetaConfigKey` 的图标、
 * 默认值、简短说明与弃用样式；社区版回退到内置的属性和文件图标。
 *
 * 排序与 SpEL 符号候选保持一致：以输入开头的键在前（组 1），仅包含输入的
 * 键在后（组 2），同组内按不区分大小写的字母序。过滤规则与原生
 * `@Value("${...}")` 相同：键名不包含输入的候选直接丢弃。
 */
internal object SpringPlaceholderCompletionSupport {

    fun addCandidates(
        model: SpringEnvironmentModel,
        keyPrefix: String,
        keyStart: Int,
        result: CompletionResultSet,
        editor: Editor? = null,
        anchor: PsiElement? = null
    ) {
        // 与原生 `@Value("${...}")` 一致：只保留键名包含输入的候选（不区分大小写），
        // 输入 `ser` 时 `server.port` 与 `...service...` 命中，而完全不含 `ser`
        // 的键不出现。`spring.config.import` 导入的键同样参与匹配。
        val matcher = SpelPrefixMatcher(SpelPrefixMatcher.wordOf(keyPrefix), keyPrefix)
        val target = result
            .withPrefixMatcher(matcher)
            .withRelevanceSorter(spelSorter(currentWordProvider(editor)))
        // 键名中的 `.` 也是输入的一部分，出现新字符时必须重新过滤候选
        target.restartCompletionOnAnyPrefixChange()
        for (key in model.allKeys()) {
            if (key.isEmpty()) {
                continue
            }
            val metadata = model.metadataOf(key)
            for (definition in model.definitionsOf(key)) {
                target.addElement(
                    SpelOrderedElement(
                        SpringPlaceholderLookupElement(
                            lookupOf(model, key, definition, keyStart, anchor, metadata),
                            definition.file.path
                        ),
                        key,
                        layerOf(metadata, definition),
                        layerFirst = true
                    )
                )
            }
        }
    }

    /**
     * 候选分层：有 Spring 元数据的标准键在前，项目主配置里的自定义键
     * 其次，`spring.config.import` 导入文件里的键最后。
     *
     * 输入非空时 [SpelOrderedElement.layerFirst] 仍保留分层，避免
     * `lucky.http-client.*` 这类导入配置的键与 `server.*` 标准键混在
     * 一起；同层内仍按「前缀命中优先、字母序」排序。
     */
    private fun layerOf(
        metadata: SpringBootConfigKeyMetadata?,
        definition: SpringPropertyDefinition
    ): String = when {
        metadata != null -> STANDARD_LAYER
        MAIN_CONFIG_FILE.matches(definition.file.name) -> MAIN_CONFIG_LAYER
        else -> IMPORTED_LAYER
    }

    private fun lookupOf(
        model: SpringEnvironmentModel,
        key: String,
        definition: SpringPropertyDefinition,
        keyStart: Int,
        anchor: PsiElement?,
        metadata: SpringBootConfigKeyMetadata?
    ): LookupElement {
        val value = valuePreviewOf(model, definition)
        val fileIcon = fileIconOf(anchor, definition.file)
        var builder = LookupElementBuilder.create(key)
            // 平台只替换光标前的标识符片段，键名含 `.` 时 `${api.` 选择 `api.host`
            // 会得到 `api.api.host`；这里按 `${` 与 `}`/`:` 之间的完整键名重写
            .withInsertHandler { context, _ ->
                val document = context.document
                val end = keyEndIn(document.charsSequence, keyStart, context.tailOffset)
                document.replaceString(keyStart, end, key)
                context.editor.caretModel.moveToOffset(keyStart + key.length)
            }
        builder = if (metadata != null) {
            builder.withRenderer(
                SpringConfigKeyLookupRenderer(metadata, value, definition.fileName, fileIcon)
            )
        } else {
            builder.withIcon(AllIcons.Nodes.Property)
                .withTailText(value?.let { "=$it" }, true)
                .withTypeText(definition.fileName, fileIcon, true)
        }
        documentationElementOf(anchor, key, definition, metadata, value, model.moduleName)?.let {
            builder = builder.withPsiElement(it)
        }
        return SpringPlaceholderLookupElement(builder, definition.file.path)
    }

    /**
     * 补全项的右侧文档：正文直接用 Spring Boot 元数据里的完整说明（HTML），
     * 表格区补上默认值、当前值、来源文件与弃用信息，贴近原生 application.yml。
     */
    private fun documentationElementOf(
        anchor: PsiElement?,
        key: String,
        definition: SpringPropertyDefinition,
        metadata: SpringBootConfigKeyMetadata?,
        value: String?,
        moduleName: String?
    ): SpelDeclaration? {
        if (anchor == null || metadata == null) {
            return null
        }
        val html = metadata.fullDescriptionHtml?.takeIf { it.isNotBlank() }
        val short = plainTextOf(metadata.shortDescription)
        val rows = ArrayList<Pair<String, String>>()
        metadata.defaultValue?.let { rows += "默认值" to it }
        value?.let { rows += "当前值" to it }
        rows += "定义于" to labelOf(definition)
        moduleName?.let { rows += "模块" to it }
        if (metadata.deprecated) {
            rows += "已废弃" to (plainTextOf(metadata.deprecationReason) ?: "该配置键已废弃")
            metadata.replacement?.let { rows += "替代键" to it }
        }
        return SpelDeclaration(
            project = anchor.project,
            name = key,
            kindLabel = "Spring 配置属性",
            typeLabel = metadata.typeText,
            description = value,
            documentation = SpelDocumentation(
                name = key,
                category = "Spring 配置属性",
                type = metadata.typeText,
                scope = labelOf(definition),
                description = short,
                htmlDescription = html,
                extraRows = rows
            ),
            anchor = anchor
        )
    }

    /**
     * 与原生 `@Value("${...}")` 一致，用 `PsiFile#getIcon` 取配置文件图标：
     * Spring Boot 插件的 `IconProvider` 会给 `application*.yml/properties`
     * 加 Spring 图标，`VirtualFile` 只能拿到普通 YAML/Properties 图标。
     */
    private fun fileIconOf(anchor: PsiElement?, file: VirtualFile): Icon? =
        anchor?.let {
            runCatching { PsiManager.getInstance(it.project).findFile(file)?.getIcon(0) }
                .getOrNull()
        } ?: runCatching { IconUtil.getIcon(file, 0, null) }.getOrNull()

    private fun labelOf(definition: SpringPropertyDefinition): String = buildString {
        append(definition.fileName)
        definition.profileLabel?.let { append(" (").append(it).append(')') }
        if (definition.rank == null) {
            append(" · 未激活的 profile")
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

    /** 元数据说明可能是 HTML，补全列表与文档只展示压平后的单行文本。 */
    private fun plainTextOf(text: String?): String? = configPlainTextOf(text)

    private const val PREVIEW_LIMIT = 60

    /** 空输入时的分层键：配置键排在导入符号之后、内置符号之前。 */
    private const val STANDARD_LAYER = "010"

    private const val MAIN_CONFIG_LAYER = "011"

    private const val IMPORTED_LAYER = "012"

    /** 与 [SpringPropertyIndex] 的配置文件名规则一致：项目主配置文件。 */
    private val MAIN_CONFIG_FILE =
        Regex("(?i)^(application|bootstrap)(-[^.]+)?\\.(properties|yml|yaml)$")
}

/**
 * 与原生 `@Value("${...}")` 相同的候选结构：属性图标 + `key=value` + 简短说明，
 * 右侧是定义所在配置文件的图标与文件名（原生用 `PsiFile#getIcon` 渲染），
 * 不再占位显示 Java 类型；类型仍保留在补全文档里。
 */
private class SpringConfigKeyLookupRenderer(
    private val metadata: SpringBootConfigKeyMetadata,
    private val value: String?,
    private val fileName: String,
    private val fileIcon: Icon?
) : LookupElementRenderer<LookupElement>() {

    override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
        presentation.icon = metadata.icon ?: AllIcons.Nodes.Property
        presentation.itemText = element.lookupString
        if (metadata.deprecated) {
            presentation.isStrikeout = true
            if (metadata.deprecationError) {
                presentation.itemTextForeground = JBColor.RED
            }
        }
        (value ?: metadata.defaultValue)?.let {
            presentation.setTailText("=$it", null)
        }
        summaryOf(metadata.shortDescription)?.let {
            presentation.appendTailText("  $it", true)
        }
        presentation.setTypeText(fileName, fileIcon)
    }
}

/** 配置元数据说明可能是 HTML：去标签、反转义并压平为单行文本。 */
private fun configPlainTextOf(text: String?): String? = text
    ?.replace(CONFIG_HTML_TAG, " ")
    ?.let { StringUtil.unescapeXmlEntities(it) }
    ?.replace(CONFIG_WHITESPACE, " ")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }

/** 配置元数据摘要：单行文本超过阈值时截断。 */
private fun summaryOf(text: String?): String? {
    val plain = configPlainTextOf(text) ?: return null
    return if (plain.length <= CONFIG_SUMMARY_LIMIT) plain else plain.take(CONFIG_SUMMARY_LIMIT) + "…"
}

private val CONFIG_HTML_TAG = Regex("<[^>]*>")

private val CONFIG_WHITESPACE = Regex("\\s+")

private const val CONFIG_SUMMARY_LIMIT = 80

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
        // `${...}` 位于 Lucky 注解属性中时键由本插件负责：即使当前模块
        // 没有可用配置模型，也不能让 Spring 的占位符补全再插一套候选。
        result.stopHere()
        val model = service.modelForCopy(literal, parameters.originalFile) ?: return
        val keyPrefix = literal.text.substring(placeholder.keyStartOffset, offsetInLiteral)
        SpringPlaceholderCompletionSupport.addCandidates(
            model,
            keyPrefix,
            literal.textRange.startOffset + placeholder.keyStartOffset,
            result,
            parameters.editor,
            literal
        )
    }
}
