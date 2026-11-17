package com.luckyframework.idea.spel.documentation

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.luckyframework.idea.spel.reference.SpelDeclaration
import java.lang.reflect.Proxy

/**
 * 补全右侧文档的强兜底：直接为 lookup 元素提供文档目标。
 *
 * 2026.3 的补全文档先询问 `lookupElementTargetProvider` 扩展点，只有它返回 null
 * 才回退到 `DocumentationManager.getElementFromLookup`；后者要求虚拟声明能解析出
 * 物理文件，任一环节失效都会让右侧空白。这里用动态代理注册该扩展点，命中
 * [SpelDeclaration] 时真实 PSI 声明交给平台原生文档目标，虚拟声明回退
 * [SpelCompletionDocumentationTarget]。
 *
 * 扩展点只在较新的平台存在；查不到时静默跳过，不影响旧版本。
 */
internal object SpelLookupDocumentationBridge {

    private const val EP_NAME = "com.intellij.platform.backend.documentation.lookupElementTargetProvider"
    private const val EP_INTERFACE = "com.intellij.platform.backend.documentation.LookupElementDocumentationTargetProvider"

    private val log = Logger.getInstance("com.luckyframework.idea.spel.doc")

    fun install(project: Project) {
        val point = try {
            ExtensionPointName.create<Any>(EP_NAME).point
        } catch (e: Throwable) {
            log.debug("[SpelDoc] 平台无 ${EP_NAME}，跳过补全文档桥接")
            return
        }
        val interfaceClass = try {
            Class.forName(EP_INTERFACE)
        } catch (e: Throwable) {
            return
        }
        val provider = Proxy.newProxyInstance(interfaceClass.classLoader, arrayOf(interfaceClass)) { proxy, method, args ->
            when (method.name) {
                "documentationTarget" ->
                    declarationOf(args?.getOrNull(1))?.let { declaration ->
                        SpelNativeDocumentation.targetOf(declaration)
                            ?: SpelCompletionDocumentationTarget(declaration)
                    }

                "equals" -> proxy === args?.getOrNull(0)
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "SpelLookupDocumentationTargetProvider"
                else -> null
            }
        }
        try {
            point.registerExtension(provider, project)
            log.debug("[SpelDoc] 已注册补全文档目标桥接")
        } catch (e: Throwable) {
            log.debug("[SpelDoc] 注册补全文档目标桥接失败: ${e.message}")
        }
    }

    /** 候选元素同时携带 object 与 psiElement，任一处命中虚拟声明即可。 */
    private fun declarationOf(value: Any?): SpelDeclaration? {
        val element = value as? LookupElement ?: return null
        return (element.`object` as? SpelDeclaration) ?: (element.psiElement as? SpelDeclaration)
    }
}
