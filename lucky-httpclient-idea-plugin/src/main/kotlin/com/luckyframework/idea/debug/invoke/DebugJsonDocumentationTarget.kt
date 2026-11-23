package com.luckyframework.idea.debug.invoke

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation

/**
 * JSON 参数字段的文档目标。
 *
 * 2026.3 起悬停与 Quick Doc 走 `DocumentationTarget` 体系；虚拟 JSON 文件没有
 * 真实的 Java 声明，需要由 [DebugJsonDocumentationTargetProvider] 产出本类实例，
 * 平台才会在 key 上展示字段的类型与 Javadoc 说明。
 */
class DebugJsonDocumentationTarget(private val parameter: DebugParameter) : DocumentationTarget {

    override fun createPointer(): Pointer<out DocumentationTarget> = Pointer.hardPointer(this)

    override fun computePresentation(): TargetPresentation =
        TargetPresentation.builder(parameter.name).presentation()

    /** 悬停预览只展示一行摘要，完整内容由 [computeDocumentation] 提供。 */
    override fun computeDocumentationHint(): String = DebugJsonDocumentation.hintOf(parameter)

    override fun computeDocumentation(): DocumentationResult =
        DocumentationResult.documentation(DebugJsonDocumentation.htmlOf(parameter))
}
