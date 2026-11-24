package com.luckyframework.idea.spel.environment

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.reference.SpelDeclaration
import com.luckyframework.idea.spel.reference.SpelReference

/**
 * `${key}` 的跳转信息：键名引用 → 配置文件中的键位置。
 *
 * 同一键可能出现在多个 profile 文件里，引用返回全部可导航目标；
 * 系统属性/环境变量命中时额外给一个不可导航的虚拟声明，用来展示当前值来源。
 */
class SpringPlaceholderReferenceProvider : PsiReferenceProvider() {

    private val resolver = SpelAnnotationResolver()

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val literal = element as? PsiLiteralExpression ?: return PsiReference.EMPTY_ARRAY
        if (!literal.text.contains("\${") || !resolver.isSpelLiteral(literal)) {
            return PsiReference.EMPTY_ARRAY
        }
        if (DumbService.isDumb(literal.project)) {
            return PsiReference.EMPTY_ARRAY
        }
        val service = SpringEnvironmentService.getInstance(literal.project)
        if (!service.isEnabledFor(literal)) {
            return PsiReference.EMPTY_ARRAY
        }
        val model = service.modelFor(literal) ?: return PsiReference.EMPTY_ARRAY
        return SpringPlaceholderReferences.forLiteral(literal, model)
    }

    companion object {

        /** 无状态可共享：构造参数只是无状态的注解解析器。 */
        private val SHARED = SpringPlaceholderReferenceProvider()

        /**
         * 直接收取本提供器为字面量生成的 `${key}` 引用。
         *
         * 引用搜索中不能调用 `literal.references`：该入口会执行所有
         * contributor，其中 IntelliLang 针对 `@Language` 变量的拼接注入会在内部
         * 再次发起 `ReferencesSearch`，重入引用搜索器后无限递归
         * （StackOverflowError）。
         */
        internal fun referencesOf(literal: PsiLiteralExpression): Array<PsiReference> =
            SHARED.getReferencesByElement(literal, ProcessingContext())
    }
}

/** Java 与 YAML 共用的引用构建逻辑。 */
internal object SpringPlaceholderReferences {

    fun referencesOf(
        anchor: PsiElement,
        text: String,
        model: SpringEnvironmentModel
    ): Array<PsiReference> {
        val placeholders = SpringPlaceholderParser.parse(text)
            .filter { it.closed && it.key.isNotEmpty() }
        if (placeholders.isEmpty()) {
            return PsiReference.EMPTY_ARRAY
        }
        return placeholders.map { placeholder ->
            SpelReference(
                anchor,
                TextRange(placeholder.keyStartOffset, placeholder.keyEndOffset),
                renameMode = renameModeOf(model, placeholder.key)
            ) {
                declarationsOf(anchor, model, placeholder.key)
            }
        }.toTypedArray()
    }

    /**
     * 重命名参数格式由配置来源决定：properties 传完整键名，YAML 只传叶子键名。
     * 同一键同时存在于两类文件时按 properties 处理（完整键名不受父路径影响）。
     */
    private fun renameModeOf(
        model: SpringEnvironmentModel,
        key: String
    ): SpelReference.RenameMode {
        val definitions = model.definitionsOf(key)
        val yamlOnly = definitions.isNotEmpty() && definitions.all { it.isYaml }
        return if (yamlOnly) {
            SpelReference.RenameMode.PROPERTY_LEAF
        } else {
            SpelReference.RenameMode.PROPERTY_FULL
        }
    }

    fun forLiteral(
        literal: PsiLiteralExpression,
        model: SpringEnvironmentModel
    ): Array<PsiReference> = referencesOf(literal, literal.text, model)

    private fun declarationsOf(
        anchor: PsiElement,
        model: SpringEnvironmentModel,
        key: String
    ): List<SpelDeclaration> {
        val result = ArrayList<SpelDeclaration>()
        // 当前生效值来自系统属性/环境变量时，目标不可导航，但保留来源用于跳转弹窗
        val resolved = model.resolve(key)
        if (resolved != null && resolved.definition == null) {
            result += SpelDeclaration(
                project = anchor.project,
                name = key,
                kindLabel = "环境变量",
                typeLabel = resolved.label,
                description = resolved.value?.let { "值：${previewOf(it)}" },
                anchor = anchor
            )
        }
        for (definition in model.definitionsOf(key)) {
            val target = targetElement(anchor, definition) ?: continue
            val value = model.resolveText(definition.value)
            result += SpelDeclaration(
                project = anchor.project,
                name = key,
                kindLabel = "环境变量",
                typeLabel = labelOf(definition),
                description = value?.takeIf { it.isNotBlank() }?.let { "值：${previewOf(it)}" },
                target = target,
                anchor = anchor
            )
        }
        return result
    }

    private fun targetElement(anchor: PsiElement, definition: SpringPropertyDefinition): PsiElement? {
        val file = PsiManager.getInstance(anchor.project).findFile(definition.file)
            ?: return null
        return file.findElementAt(definition.keyOffset.coerceIn(0, file.textLength))
    }

    private fun labelOf(definition: SpringPropertyDefinition): String = buildString {
        append(definition.fileName)
        definition.profileLabel?.let { append(" (").append(it).append(')') }
        if (definition.rank == null) {
            append(" · 未激活的 profile")
        }
    }

    private fun previewOf(value: String): String {
        val singleLine = value.replace('\n', ' ').replace('\r', ' ').trim()
        return if (singleLine.length <= PREVIEW_LIMIT) singleLine else singleLine.take(PREVIEW_LIMIT) + "…"
    }

    private const val PREVIEW_LIMIT = 80
}
