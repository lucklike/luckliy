package com.luckyframework.idea.debug.invoke

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.psi.PsiFile

/**
 * JSON 参数编辑器的悬停文档入口（2026.3 悬停与 Quick Doc 的主路径）。
 *
 * 只处理 [DebugJsonDocumentation.attach] 挂过参数索引的虚拟文件；
 * 普通 JSON 文件与其他编辑器不会命中。
 */
class DebugJsonDocumentationTargetProvider : DocumentationTargetProvider {

    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        val parameter = DebugJsonDocumentation.parameterAt(file, offset) ?: return emptyList()
        return listOf(DebugJsonDocumentationTarget(parameter))
    }
}
