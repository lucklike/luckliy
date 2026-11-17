package com.luckyframework.idea.spel.documentation

import com.intellij.lang.documentation.psi.PsiElementDocumentationTarget
import com.intellij.lang.documentation.psi.createPsiDocumentationTarget
import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.luckyframework.idea.spel.reference.SpelDeclaration

/**
 * 把 SpEL 声明背后的真实 PSI 交给平台原生文档目标渲染。
 *
 * 插件此前用自制 HTML 表格展示文档，与 IDEA 内置 Java 文档差距很大：
 * 缺少注解、完整 Javadoc、Returns/See Also 与版本来源。真实声明存在时
 * 直接构造 [PsiElementDocumentationTarget]，由平台的
 * `DocumentationManager.getProviderFromElement` 选择 Java 文档提供器生成
 * 与编辑器内完全一致的内容；未解析符号、`#this`/`#root` 等虚拟声明以及
 * `@Param` 注解属性值等没有文档的 PSI 仍走 [SpelDocumentationRenderer]。
 */
internal object SpelNativeDocumentation {

    private val log = Logger.getInstance("com.luckyframework.idea.spel.doc")

    /** 声明可用于原生文档时返回平台目标，否则返回 null 由调用方回退自制渲染。 */
    fun targetOf(declaration: SpelDeclaration): DocumentationTarget? {
        val element = nativeElementOf(declaration) ?: return null
        return try {
            // 平台目标类构造器是 internal，统一走公开工厂；第二参为原元素，
            // 不传时平台按目标元素自身推导。
            createPsiDocumentationTarget(element, null)
        } catch (e: Throwable) {
            log.debug("[SpelDoc] 原生文档目标创建失败: ${e.message}")
            null
        }
    }

    /**
     * 文档渲染元素：只取显式声明的 [SpelDeclaration.documentationTarget]。
     *
     * 不直接回退跳转目标：内置变量（如 `$body$`）的声明是框架内部的
     * 变量名常量字段，原生文档只会显示字段签名，反而丢失变量语义；
     * 这类符号继续由自制渲染器展示说明。
     */
    fun nativeElementOf(declaration: SpelDeclaration): PsiElement? {
        val element = declaration.documentationTarget ?: return null
        return element.takeIf(::isDocumentable)
    }

    private fun isDocumentable(element: PsiElement): Boolean =
        element is PsiMethod || element is PsiField || element is PsiClass || element is PsiParameter
}
