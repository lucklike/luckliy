package com.luckyframework.idea.spel.environment

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.metadata.SpelInjectionScope
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

    /** 折叠占位文本：单行化并按 [PREVIEW_LIMIT] 截断；@Env 折叠复用同一规则。 */
    internal fun previewOf(value: String): String {
        val singleLine = value.replace('\n', ' ').replace('\r', ' ').trim()
        return if (singleLine.length <= PREVIEW_LIMIT) {
            singleLine
        } else {
            singleLine.take(PREVIEW_LIMIT) + "…"
        }
    }

    private const val PREVIEW_LIMIT = 80
}

/**
 * `@Env("key")` / `@Var(type = ENVIRONMENT, value = "key")` 的真实值折叠。
 *
 * 运行期 `Context#getMethodParamObject` 把注入名拼成 `${key}` 求值，
 * 语义与占位符折叠一致：键命中显示配置值，未命中按 `key:default`
 * 语法回退默认值；都拿不到时保持原文，避免把不完整结果当成确定值。
 * 只折叠名字本身（引号内整段），拼接表达式中的子字面量不参与。
 */
internal object EnvInjectionFoldRegions {

    fun regionFor(
        literal: PsiLiteralExpression,
        service: SpringEnvironmentService
    ): FoldingDescriptor? {
        if (!LuckyHttpClientSettings.getInstance().isEnvironmentPreviewEnabled) {
            return null
        }
        val injection = RESOLVER.resolveInjection(literal) ?: return null
        if (injection.scope != SpelInjectionScope.ENV || !injection.explicitName) {
            return null
        }
        if (injection.referenceTarget != literal) {
            return null
        }
        val name = injection.referenceName
        val separator = name.indexOf(':')
        val key = if (separator < 0) name else name.substring(0, separator)
        val defaultText = if (separator < 0) null else name.substring(separator + 1)
        if (key.isEmpty()) {
            return null
        }
        val model = service.modelFor(literal) ?: return null
        val value = injectedValue(key, defaultText, model) ?: return null
        if (value.isEmpty()) {
            return null
        }
        val range = ElementManipulators.getValueTextRange(literal)
            .shiftRight(literal.textRange.startOffset)
        return FoldingDescriptor(
            literal.node,
            range,
            null,
            SpringPlaceholderFoldRegions.previewOf(value)
        )
    }

    /** 键命中取配置值，未命中回退 `key:default` 的默认值；默认值内可嵌套占位符。 */
    private fun injectedValue(
        key: String,
        defaultText: String?,
        model: SpringEnvironmentModel
    ): String? {
        val resolved = model.resolve(key)
        if (resolved != null) {
            return resolved.value
        }
        return defaultText?.let { model.resolveText(it) }
    }

    private val RESOLVER = SpelAnnotationResolver()
}

/**
 * Java 注解属性中的 `${key}` 折叠预览。
 *
 * Spring `@Value`、`@StdHttpClient#path` 与 Lucky 的 `@SpelAttribute` 属性
 * 在运行期都会被替换成环境值，因此注解属性中的字面量统一参与折叠；
 * 不依赖框架 jar 是否携带 `@SpelAttribute` 元数据，旧版本依赖同样生效。
 */
class SpringPlaceholderFoldingBuilder : FoldingBuilder {

    private val log = Logger.getInstance(SpringPlaceholderFoldingBuilder::class.java)

    override fun buildFoldRegions(
        root: ASTNode,
        document: Document
    ): Array<FoldingDescriptor> {
        val file = root.psi.containingFile ?: return FoldingDescriptor.EMPTY_ARRAY
        // 文件级预筛：占位符与 @Env 注入名都没有时不必扫描字面量；
        // `@Var(type = VarType.ENVIRONMENT)` 写的是全大写枚举名
        val hasPlaceholder = document.text.contains("\${")
        val hasEnvInjection = document.text.contains("Env") || document.text.contains("ENVIRONMENT")
        if (!hasPlaceholder && !hasEnvInjection) {
            return FoldingDescriptor.EMPTY_ARRAY
        }
        val service = SpringEnvironmentService.getInstance(file.project)
        log.info("[LuckyFold] build file=${file.name} plugin=${pluginVersion()} ${service.debugReport(file)}")
        if (!service.isEnabledFor(file)) {
            return FoldingDescriptor.EMPTY_ARRAY
        }
        val result = ArrayList<FoldingDescriptor>()
        for (literal in PsiTreeUtil.findChildrenOfType(root.psi, PsiLiteralExpression::class.java)) {
            if (!isAnnotationAttribute(literal)) {
                continue
            }
            if (hasPlaceholder && literal.text.contains("\${")) {
                val model = service.modelFor(literal)
                val regions = if (model == null) {
                    emptyList()
                } else {
                    SpringPlaceholderFoldRegions.regionsFor(
                        literal,
                        literal.text,
                        literal.textRange.startOffset,
                        model
                    )
                }
                log.info("[LuckyFold] literal=${literal.text} model=${model != null} regions=${regions.size} ${describe(literal, model)}")
                result += regions
            }
            if (hasEnvInjection) {
                EnvInjectionFoldRegions.regionFor(literal, service)?.let { region ->
                    log.info("[LuckyFold] env literal=${literal.text} preview=${region.placeholderText}")
                    result += region
                }
            }
        }
        log.info("[LuckyFold] done file=${file.name} regions=${result.size} ${result.joinToString { "'${it.placeholderText}'@${it.range}" }}")
        return result.toTypedArray()
    }

    /** 每个占位符的解析结果，直接体现在日志里。 */
    private fun describe(literal: PsiLiteralExpression, model: SpringEnvironmentModel?): String {
        if (model == null) {
            return "resolved=<no model>"
        }
        return SpringPlaceholderParser.parse(literal.text)
            .filter { it.closed && it.key.isNotEmpty() }
            .joinToString { "key=${it.key} value=${model.resolvePlaceholder(it) ?: "<unresolved>"}" }
    }

    /** 字面量是否位于注解属性中：`@Value("${x}")` 与 `path = "${x}"` 都算。 */
    private fun isAnnotationAttribute(literal: PsiLiteralExpression): Boolean {
        val pair = PsiTreeUtil.getParentOfType(literal, PsiNameValuePair::class.java, false)
            ?: return false
        return PsiTreeUtil.getParentOfType(pair, PsiAnnotation::class.java, false) != null
    }

    override fun getPlaceholderText(node: ASTNode): String? = null

    override fun isCollapsedByDefault(node: ASTNode): Boolean = true

    private companion object {
        /** 插件版本：日志中出现该值才能确认 IDE 运行的是本次构建的代码。 */
        fun pluginVersion(): String =
            PluginManagerCore.getPlugin(PluginId.getId("com.luckyframework.idea.httpclient"))?.version
                ?: "<unknown>"
    }
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
