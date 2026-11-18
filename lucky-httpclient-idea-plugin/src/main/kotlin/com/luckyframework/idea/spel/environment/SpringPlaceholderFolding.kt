package com.luckyframework.idea.spel.environment

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.spel.yaml.SpelYamlSupport
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * `${key}` 的解析值预览：默认折叠，占位文本显示当前环境的值。
 *
 * 点击折叠区域即可展开原表达式，与 Spring `@Value` 的“看到最终值”体验一致。
 * 只有完整解析出的值才折叠；未解析的键保持原文，避免误导。
 */
internal object SpringPlaceholderFoldRegions {

    fun regionsFor(
        anchor: PsiElement,
        text: String,
        baseOffset: Int,
        model: SpringEnvironmentModel
    ): List<FoldingDescriptor> {
        if (!LuckyHttpClientSettings.getInstance().isEnvironmentPreviewEnabled) {
            return emptyList()
        }
        return SpringPlaceholderParser.parse(text)
            .filter { it.closed && it.key.isNotEmpty() }
            .mapNotNull { placeholder ->
                val value = model.resolvePlaceholder(placeholder) ?: return@mapNotNull null
                if (value.isEmpty()) {
                    return@mapNotNull null
                }
                FoldingDescriptor(
                    anchor.node,
                    TextRange(baseOffset + placeholder.startOffset, baseOffset + placeholder.endOffset),
                    null,
                    previewOf(value)
                )
            }
    }

    private fun previewOf(value: String): String {
        val singleLine = value.replace('\n', ' ').replace('\r', ' ').trim()
        return if (singleLine.length <= PREVIEW_LIMIT) {
            singleLine
        } else {
            singleLine.take(PREVIEW_LIMIT) + "…"
        }
    }

    private const val PREVIEW_LIMIT = 80
}

/** Java 注解属性中的 `${key}` 折叠预览。 */
class SpringPlaceholderFoldingBuilder : FoldingBuilder {

    private val resolver = SpelAnnotationResolver()

    override fun buildFoldRegions(
        root: ASTNode,
        document: Document
    ): Array<FoldingDescriptor> {
        val file = root.psi.containingFile ?: return FoldingDescriptor.EMPTY_ARRAY
        val service = SpringEnvironmentService.getInstance(file.project)
        if (!service.isEnabledFor(file)) {
            return FoldingDescriptor.EMPTY_ARRAY
        }
        val result = ArrayList<FoldingDescriptor>()
        for (literal in PsiTreeUtil.findChildrenOfType(root.psi, PsiLiteralExpression::class.java)) {
            if (!literal.text.contains("\${") || !resolver.isSpelLiteral(literal)) {
                continue
            }
            val model = service.modelFor(literal) ?: continue
            result += SpringPlaceholderFoldRegions.regionsFor(
                literal,
                literal.text,
                literal.textRange.startOffset,
                model
            )
        }
        return result.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? = null

    override fun isCollapsedByDefault(node: ASTNode): Boolean = true
}

/** YAML 值中的 `${key}` 折叠预览（YAML 插件存在时加载）。 */
class SpringPlaceholderYamlFoldingBuilder : FoldingBuilder {

    override fun buildFoldRegions(
        root: ASTNode,
        document: Document
    ): Array<FoldingDescriptor> {
        val file = root.psi.containingFile ?: return FoldingDescriptor.EMPTY_ARRAY
        val service = SpringEnvironmentService.getInstance(file.project)
        if (!service.isEnabledFor(file)) {
            return FoldingDescriptor.EMPTY_ARRAY
        }
        val result = ArrayList<FoldingDescriptor>()
        for (scalar in PsiTreeUtil.findChildrenOfType(root.psi, YAMLScalar::class.java)) {
            if (!SpelYamlSupport.isPlaceholderValue(scalar)) {
                continue
            }
            val model = service.modelFor(scalar) ?: continue
            result += SpringPlaceholderFoldRegions.regionsFor(
                scalar,
                scalar.text,
                scalar.textRange.startOffset,
                model
            )
        }
        return result.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? = null

    override fun isCollapsedByDefault(node: ASTNode): Boolean = true
}
