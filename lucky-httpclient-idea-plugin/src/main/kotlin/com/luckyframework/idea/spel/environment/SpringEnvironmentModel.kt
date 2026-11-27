package com.luckyframework.idea.spel.environment

import com.intellij.openapi.module.Module
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * 一个键的解析结果。
 *
 * [value] 是完整解析后的值（含嵌套占位符），无法完全解析时为 null；
 * [label] 用于跳转弹窗与文档来源说明；[definition] 是可导航的配置文件定义。
 */
data class ResolvedSpringProperty(
    val key: String,
    val value: String?,
    val label: String,
    val definition: SpringPropertyDefinition? = null
)

/**
 * 在配置快照之上解析 `${key:default}`。
 *
 * 查找顺序与 Spring Boot 默认属性源一致：
 * System properties → 环境变量 → 配置文件（profile 文件优先）→ 占位符默认值。
 * 值内部的 `${...}` 递归解析；出现循环引用或未解析的嵌套占位符时返回 null，
 * 调用方不应把半成品当成“确定值”展示。
 */
class SpringEnvironmentModel(
    private val snapshot: SpringPropertySnapshot,
    private val module: Module? = null
) {

    /** 当前模型所属模块名，补全文档的“模块”行使用。 */
    val moduleName: String?
        get() = module?.name

    /** Spring Boot 元数据索引按需建立：折叠、跳转等路径不触发元数据读取。 */
    private val metadataKeys: Map<String, Any> by lazy {
        module?.let { SpringBootConfigKeyMetadataBridge.keysFor(it) }.orEmpty()
    }

    private val metadata = ConcurrentHashMap<String, Optional<SpringBootConfigKeyMetadata>>()

    /**
     * 键的 Spring Boot 元数据（Ultimate）：图标、类型、默认值、说明与弃用信息。
     *
     * 社区版、未安装 Spring Boot 插件或键不在元数据中时返回 null，
     * 补全回退到内置样式。结果按键缓存，避免同一轮补全重复反射。
     */
    internal fun metadataOf(key: String): SpringBootConfigKeyMetadata? {
        if (key.isEmpty() || metadataKeys.isEmpty()) {
            return null
        }
        return metadata.computeIfAbsent(key) {
            val metaKey = metadataKeys[key] ?: metadataKeys[key.lowercase()]
            Optional.ofNullable(
                metaKey?.let { SpringBootConfigKeyMetadataBridge.metadataOf(it, key) }
            )
        }.orElse(null)
    }

    fun keys(): List<String> = snapshot.keys()

    /**
     * 全部键（含未激活 profile 的定义），用于补全候选与「是否为完整键」检查。
     *
     * [keys] 只包含当前生效的键；未激活 profile 文件中的键在配置文件里
     * 真实存在，仍需参与补全与警告判断。
     */
    fun allKeys(): List<String> = snapshot.definitions.keys.sorted()

    /** 以 [key] 为前缀的子配置项（`spring.datasource` → `spring.datasource.url`）。 */
    fun childKeysOf(key: String): List<String> =
        snapshot.definitions.keys.filter { it.startsWith("$key.") }.sorted()

    /** 某个键的全部配置文件定义（含未激活 profile），用于跳转选择。 */
    fun definitionsOf(key: String): List<SpringPropertyDefinition> = snapshot.definitionsOf(key)

    /** 键的最终值；键不存在时返回 null。 */
    fun resolveValue(key: String, visiting: MutableSet<String> = HashSet()): String? {
        if (key.isEmpty() || !visiting.add(key)) {
            return null
        }
        try {
            System.getProperty(key)?.let { return resolveText(it, visiting) }
            System.getenv(key)?.let { return resolveText(it, visiting) }
            val definition = snapshot.effective[key] ?: return null
            return resolveText(definition.value, visiting)
        } finally {
            visiting.remove(key)
        }
    }

    /** 占位符的值：键存在时用键值，否则用默认值；默认值内还可以嵌套占位符。 */
    fun resolvePlaceholder(
        placeholder: SpringPlaceholder,
        visiting: MutableSet<String> = HashSet()
    ): String? {
        val value = resolveValue(placeholder.key, visiting)
        if (value != null) {
            return value
        }
        val defaultText = placeholder.defaultText ?: return null
        return resolveText(defaultText, visiting)
    }

    /** 完整解析一段文本；任意占位符无法解析时返回 null。 */
    fun resolveText(text: String, visiting: MutableSet<String> = HashSet()): String? {
        val placeholders = SpringPlaceholderParser.parse(text)
        if (placeholders.isEmpty()) {
            return text
        }
        val result = StringBuilder()
        var cursor = 0
        for (placeholder in placeholders) {
            if (!placeholder.closed) {
                return null
            }
            result.append(text, cursor, placeholder.startOffset)
            val value = resolvePlaceholder(placeholder, visiting) ?: return null
            result.append(value)
            cursor = placeholder.endOffset
        }
        result.append(text, cursor, text.length)
        return result.toString()
    }

    /**
     * 键的解析结果与来源。
     *
     * 系统属性/环境变量命中时同样返回配置文件的定义：值展示系统值，
     * 跳转仍可定位到项目里的声明。
     */
    fun resolve(key: String): ResolvedSpringProperty? {
        val definition = snapshot.effective[key]
        if (key.isEmpty()) {
            return null
        }
        System.getProperty(key)?.let { raw ->
            return ResolvedSpringProperty(key, resolveText(raw), "系统属性", definition)
        }
        System.getenv(key)?.let { raw ->
            return ResolvedSpringProperty(key, resolveText(raw), "环境变量", definition)
        }
        if (definition != null) {
            return ResolvedSpringProperty(
                key = key,
                value = resolveText(definition.value),
                label = labelOf(definition),
                definition = definition
            )
        }
        return null
    }

    private fun labelOf(definition: SpringPropertyDefinition): String =
        definition.profileLabel?.let { "${definition.fileName} ($it)" } ?: definition.fileName
}
