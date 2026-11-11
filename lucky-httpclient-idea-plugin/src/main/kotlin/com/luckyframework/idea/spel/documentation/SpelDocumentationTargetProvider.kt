package com.luckyframework.idea.spel.documentation

import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.psi.PsiFile

/**
 * 向平台文档目标体系提供 SpEL 符号文档（2026.3 悬停与 Quick Doc 的主路径）。
 *
 * 平台先收集所有 [DocumentationTargetProvider] 的结果，只有全部为空时才回退到
 * `DocumentationProvider.getCustomDocumentationElement`；这里按偏移精确解析，
 * 保证虚拟声明在两条链路上都能展示文档。
 */
class SpelDocumentationTargetProvider : DocumentationTargetProvider {

    private val log = Logger.getInstance("com.luckyframework.idea.spel.doc")

    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        val declaration = SpelDocumentationResolver.declarationAt(file, offset, null)
        log.debug("[SpelDoc] fileTargets file=${file.name} offset=$offset hit=${declaration != null}")
        return declaration?.let { listOf(SpelDocumentationTarget(it)) } ?: emptyList()
    }
}
