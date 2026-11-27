package com.luckyframework.idea.spel.environment

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiType
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import javax.swing.Icon

/**
 * 补全项使用的 Spring Boot 配置元数据。
 *
 * 与 IDEA 原生 application.yml 补全一致：图标、类型、默认值、简短说明、
 * 弃用标记与替代键都来自 Spring Boot 插件的 `MetaConfigKey`；
 * [fullDescriptionHtml] 是元数据 JSON 里的完整说明（HTML），用于补全文档。
 */
internal class SpringBootConfigKeyMetadata(
    val icon: Icon?,
    val typeText: String?,
    val defaultValue: String?,
    val shortDescription: String?,
    val fullDescriptionHtml: String?,
    val deprecated: Boolean,
    val deprecationError: Boolean,
    val deprecationReason: String?,
    val replacement: String?
)

/**
 * Spring Boot 配置元数据桥接（可选 Ultimate 能力）。
 *
 * Ultimate 安装了 Spring Boot 插件时，通过官方
 * `SpringBootApplicationMetaConfigKeyManager#getAllMetaConfigKeys(Module)`
 * 取得当前模块可见的全部 `MetaConfigKey`，按键名读取原生补全信息；
 * 社区版没有该插件时 [isAvailable] 为 false，补全保持内置样式。
 *
 * 与 [SpringBootConfigFileBridge] 一样只通过反射访问 Spring Boot 类型，
 * 主 classpath 不出现这些类型。
 */
internal object SpringBootConfigKeyMetadataBridge {

    private const val MANAGER_CLASS =
        "com.intellij.spring.boot.application.metadata.SpringBootApplicationMetaConfigKeyManager"

    private const val KEY_CLASS = "com.intellij.microservices.jvm.config.MetaConfigKey"

    private val managerClass: Class<*>? = SpringBootPluginAccess.loadClass(MANAGER_CLASS)

    private val keyClass: Class<*>? = SpringBootPluginAccess.loadClass(KEY_CLASS)

    private val instanceMethod: Method? = managerClass
        ?.let { runCatching { it.getMethod("getInstance") }.getOrNull() }
        ?.takeIf { Modifier.isStatic(it.modifiers) }

    private val allKeysMethod: Method? = managerClass
        ?.let { runCatching { it.getMethod("getAllMetaConfigKeys", Module::class.java) }.getOrNull() }

    private val nameMethod: Method? = keyClass?.getMethodOrNull("getName")

    private val presentationMethod: Method? = keyClass?.getMethodOrNull("getPresentation")

    private val typeMethod: Method? = keyClass?.getMethodOrNull("getType")

    private val defaultValueMethod: Method? = keyClass?.getMethodOrNull("getDefaultValue")

    private val descriptionMethod: Method? = keyClass?.getMethodOrNull("getDescriptionText")

    private val deprecationMethod: Method? = keyClass?.getMethodOrNull("getDeprecation")

    private val iconMethod: Method? =
        SpringBootPluginAccess.loadClass("$KEY_CLASS\$MetaConfigKeyPresentation")
            ?.getMethodOrNull("getIcon")

    /**
     * Spring 插件里的属性图标（绿色叶子）。
     *
     * `MetaConfigKeyPresentation#getIcon` 返回的是普通属性读/写图标（蓝色 P），
     * 不是用户期望的 Spring 图标；Spring Boot 插件依赖 Spring 插件，
     * 该图标在 Ultimate 一定可用，社区版取不到时回退元数据图标。
     */
    private val springPropertyIcon: Icon? = runCatching {
        SpringBootPluginAccess.loadClass("com.intellij.spring.SpringApiIcons")
            ?.getField("SpringProperty")
            ?.get(null) as? Icon
    }.getOrNull()

    private val descriptionClass: Class<*>? =
        SpringBootPluginAccess.loadClass("$KEY_CLASS\$DescriptionText")

    private val fullTextMethod: Method? = descriptionClass?.getMethodOrNull("getFullText")

    private val shortTextMethod: Method? = descriptionClass?.getMethodOrNull("getShortText")

    private val deprecationClass: Class<*>? =
        SpringBootPluginAccess.loadClass("$KEY_CLASS\$Deprecation")

    private val deprecationReasonMethod: Method? = deprecationClass?.getMethodOrNull("getReason")

    private val deprecationLevelMethod: Method? = deprecationClass?.getMethodOrNull("getLevel")

    private val deprecationReplacementMethod: Method? =
        deprecationClass?.getMethodOrNull("getReplacement")

    /** 未废弃的哨兵对象，原生补全按对象身份比较；它的说明文本是占位字符串，不能当说明用。 */
    private val notDeprecated: Any? = deprecationClass
        ?.let { runCatching { it.getField("NOT_DEPRECATED").get(null) }.getOrNull() }

    val isAvailable: Boolean
        get() = instanceMethod != null && allKeysMethod != null && nameMethod != null

    /**
     * 当前模块可见的全部元数据键：键名 → `MetaConfigKey` 实例。
     *
     * 只建立一次索引，具体键的图标/类型/说明在 [metadataOf] 中按需读取：
     * 原生 `findAllApplicationMetaConfigKeys` 每次都重建整个键列表，
     * 补全遍历数十上百个键时开销不可接受。
     */
    fun keysFor(module: Module): Map<String, Any> {
        if (!isAvailable) {
            return emptyMap()
        }
        return runCatching {
            val manager = instanceMethod!!.invoke(null) ?: return@runCatching emptyMap()
            val keys = allKeysMethod!!.invoke(manager, module) as? List<*> ?: return@runCatching emptyMap()
            val result = HashMap<String, Any>(keys.size)
            for (key in keys) {
                val name = nameMethod!!.invokeValue(key) as? String ?: continue
                if (key != null) {
                    result.putIfAbsent(name, key)
                }
            }
            result
        }.getOrElse { emptyMap() }
    }

    /** 读取单个键的补全信息；元数据不可用或读取失败时返回 null。 */
    fun metadataOf(metaKey: Any, key: String): SpringBootConfigKeyMetadata? {
        if (!isAvailable) {
            return null
        }
        return runCatching {
            val presentation = presentationMethod?.invokeValue(metaKey)
            val description = descriptionMethod?.invokeValue(metaKey)
            val deprecation = deprecationMethod?.invokeValue(metaKey)
            val deprecated = deprecation != null && deprecation !== notDeprecated
            val reason = if (deprecated) {
                deprecationReasonMethod?.invokeValue(deprecation)
            } else {
                null
            }
            SpringBootConfigKeyMetadata(
                // 与原生 `@Value("${key}")` 一致：优先用元数据里的属性图标
                // （PropertyRead/Write），拿不到时才回退 Spring 属性图标。
                icon = presentation?.let { iconMethod?.invokeValue(it) as? Icon }
                    ?: springPropertyIcon,
                typeText = (typeMethod?.invokeValue(metaKey) as? PsiType)?.presentableText,
                defaultValue = (defaultValueMethod?.invokeValue(metaKey) as? String)
                    ?.takeIf { it.isNotBlank() },
                shortDescription = (shortTextMethod?.invokeValue(description) as? String)
                    ?.takeIf { it.isNotBlank() },
                fullDescriptionHtml = (fullTextMethod?.invokeValue(description) as? String)
                    ?.takeIf { it.isNotBlank() },
                deprecated = deprecated,
                deprecationError = deprecated &&
                    deprecationLevelMethod?.invokeValue(deprecation)?.toString() == "ERROR",
                deprecationReason = (fullTextMethod?.invokeValue(reason) as? String)
                    ?.takeIf { it.isNotBlank() },
                replacement = deprecationReplacementMethod?.invokeValue(deprecation) as? String
            )
        }.getOrNull()
    }

    /** 便捷入口：按键名直接读取元数据（测试与单键场景）。 */
    fun metadataFor(module: Module, key: String): SpringBootConfigKeyMetadata? {
        val metaKey = keysFor(module)[key] ?: return null
        return metadataOf(metaKey, key)
    }

    private fun Method.invokeValue(target: Any?): Any? =
        runCatching { invoke(target) }.getOrNull()

    private fun Class<*>.getMethodOrNull(name: String): Method? =
        runCatching { getMethod(name) }.getOrNull()
}
