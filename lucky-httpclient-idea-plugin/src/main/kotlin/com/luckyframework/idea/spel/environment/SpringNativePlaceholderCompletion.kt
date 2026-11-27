package com.luckyframework.idea.spel.environment

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * `@Env` 候选的 Spring 原生桥接。
 *
 * `@Value("${...}")` 的配置键候选由 Spring 插件的
 * `PlaceholderPropertyCompletionContributor` 生成：PropertiesFile、Spring 模型
 * 属性与 `SpringPlaceholderReferenceResolver`（Spring Boot 的 YAML/properties
 * 配置键）汇总后统一渲染。为了让 `@Env` 与 `@Value` 完全一致，这里构造一个
 * 临时 `PlaceholderPropertyReference`，直接调用同一个 `getVariants`。
 *
 * 只通过反射访问 Spring 类型：这些类在新旧版本换了包名，`create`/`getVariants`
 * 也不是 public；主 classpath 不出现 Spring 类型，社区版缺少插件时仍能正常加载。
 * 返回 null 表示桥接不可用（未安装插件、版本不匹配、项目未启用 Spring 支持或
 * 调用异常），调用方回退到本插件的配置索引。
 */
internal object SpringNativePlaceholderCompletion {

    private const val SPRING_PLUGIN_ID = "com.intellij.spring"

    /** 字面量开引号之后，与 `@Env` 引用范围的起点一致。 */
    private const val KEY_START = 1

    private const val DEFAULT_PREFIX = "\${"

    private const val DEFAULT_SUFFIX = "}"

    private class NativeApi(
        val infoType: Class<*>,
        val createReference: Method,
        val getVariants: Method
    )

    /** 新版本把占位符模型移到了 analysis.xml.services，保留旧包名兼容 241/242。 */
    private val API_CANDIDATES = listOf(
        Triple(
            "com.intellij.spring.core.analysis.xml.services.references.PlaceholderPropertyReference",
            "com.intellij.spring.core.analysis.xml.services.references.PlaceholderInfo",
            "com.intellij.spring.core.analysis.xml.services.completion.PlaceholderPropertyCompletionContributor"
        ),
        Triple(
            "com.intellij.spring.model.values.PlaceholderPropertyReference",
            "com.intellij.spring.model.values.PlaceholderInfo",
            "com.intellij.spring.model.values.PlaceholderPropertyCompletionContributor"
        )
    )

    private val api: NativeApi? by lazy { resolveApi() }

    private val isSpringConfigured: Method? by lazy {
        loadClass("com.intellij.spring.model.utils.SpringCommonUtils")
            ?.let { runCatching { it.getMethod("isSpringConfigured", Project::class.java) }.getOrNull() }
            ?.takeIf { Modifier.isStatic(it.modifiers) }
    }

    private val springPluginClassLoader: ClassLoader? by lazy {
        runCatching {
            val descriptor = PluginManagerCore.getPlugin(PluginId.getId(SPRING_PLUGIN_ID))
                ?: return@runCatching null
            descriptor.javaClass.methods
                .firstOrNull { it.name == "getPluginClassLoader" && it.parameterCount == 0 }
                ?.invoke(descriptor) as? ClassLoader
        }.getOrNull()
    }

    /**
     * 按 Spring 为 `@Value` 生成占位符候选的方式计算 `@Env` 的 key 候选。
     *
     * 返回 null 表示桥接不可用，调用方应回退本插件索引；返回空数组表示
     * Spring 正常参与但当前没有候选（与 `@Value` 一致）。
     */
    fun variants(literal: PsiLiteralExpression, offsetInLiteral: Int): Array<LookupElement>? {
        val api = api ?: return null
        val configured = isSpringConfigured ?: return null
        val text = literal.text
        // 光标必须已在 key 内，且字面量以闭引号结尾（补全副本一定是完整字面量）
        if (offsetInLiteral <= KEY_START || !text.endsWith("\"")) {
            return null
        }
        return runCatching {
            // 与原生一致：项目未启用 Spring 支持时 @Value 不产生占位符候选
            if (configured.invoke(null, literal.project) != true) {
                return@runCatching null
            }
            // 与原生注入的 PlaceholderInfo 相同：text 含 key:default，范围是引号内整段
            val contentRange = TextRange(KEY_START, text.length - 1)
            val placeholderText = contentRange.substring(text)
            if (placeholderText.isEmpty()) {
                return@runCatching null
            }
            val info = api.infoType
                .getDeclaredConstructor(String::class.java, Pair::class.java, String::class.java, TextRange::class.java)
                .apply { isAccessible = true }
                .newInstance(
                    placeholderText,
                    Pair.create(DEFAULT_PREFIX, DEFAULT_SUFFIX),
                    text,
                    contentRange
                )
            val reference = api.createReference.invoke(null, literal, contentRange, info, true)
            val variants = api.getVariants.invoke(null, reference) as? Array<*> ?: return@runCatching null
            variants.filterIsInstance<LookupElement>().toTypedArray()
        }.getOrNull()
    }

    private fun resolveApi(): NativeApi? {
        for ((referenceName, infoName, contributorName) in API_CANDIDATES) {
            val referenceType = loadClass(referenceName) ?: continue
            val infoType = loadClass(infoName) ?: continue
            val contributorType = loadClass(contributorName) ?: continue
            val create = runCatching {
                referenceType.getDeclaredMethod(
                    "create",
                    PsiElement::class.java,
                    TextRange::class.java,
                    infoType,
                    java.lang.Boolean.TYPE
                ).apply { isAccessible = true }
            }.getOrNull() ?: continue
            val getVariants = runCatching {
                contributorType.getDeclaredMethod("getVariants", referenceType).apply { isAccessible = true }
            }.getOrNull() ?: continue
            if (Modifier.isStatic(create.modifiers) && Modifier.isStatic(getVariants.modifiers)) {
                return NativeApi(infoType, create, getVariants)
            }
        }
        return null
    }

    private fun loadClass(name: String): Class<*>? {
        val loaders = listOfNotNull(
            SpringNativePlaceholderCompletion::class.java.classLoader,
            springPluginClassLoader
        ).distinct()
        for (loader in loaders) {
            val type = runCatching { Class.forName(name, false, loader) }.getOrNull()
            if (type != null) {
                return type
            }
        }
        return null
    }
}
