package com.luckyframework.idea.spel.environment

import com.intellij.lang.properties.IProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair as PlatformPair
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Collections

/**
 * 可选的 Spring Boot 插件桥接。
 *
 * Ultimate 安装了 Spring Boot 插件时，借用官方
 * `SpringBootModelConfigFileContributor` 解析文件内的键值与
 * `spring.config.import` 语句；社区版没有该插件，或 Spring Boot 版本
 * 不提供对应方法时返回 null，由 [SpringPropertyIndex] 回退到内置纯文本解析。
 *
 * 这里刻意只通过反射访问 Spring Boot 类：主 classpath 不出现这些类型，
 * 社区版缺少插件时不会触发 [NoClassDefFoundError]，插件可以正常加载。
 */
internal object SpringBootConfigFileBridge {

    private const val CONTRIBUTOR_CLASS =
        "com.intellij.spring.boot.model.SpringBootModelConfigFileContributor"

    private val contributorClass: Class<*>? = SpringBootPluginAccess.loadClass(CONTRIBUTOR_CLASS)

    private val getContributorMethod: Method? = contributorClass
        ?.let { runCatching { it.getMethod("getContributor", VirtualFile::class.java) }.getOrNull() }
        ?.takeIf { Modifier.isStatic(it.modifiers) }

    private val processAllKeysMethod: Method? = contributorClass
        ?.let { it.getMethodOrNull("processAllKeys", PsiFile::class.java, Processor::class.java) }

    private val getImportStatementsMethod: Method? = contributorClass
        ?.let { it.getMethodOrNull("getImportStatements", Project::class.java, VirtualFile::class.java) }

    /** Spring Boot 插件与 contributor 入口都可用时才算启用。 */
    val isAvailable: Boolean
        get() = getContributorMethod != null

    /**
     * 解析单个配置文件。
     *
     * 返回 null 表示桥接不可用，调用方应整体回退内置解析；
     * [BridgeDocument.definitions] / [BridgeDocument.imports] 为 null 表示
     * 该 Spring Boot 版本缺少单项能力，调用方只需回退对应部分。
     */
    fun parse(project: Project, file: VirtualFile): BridgeDocument? {
        if (!isAvailable || file.extension?.lowercase() !in CONFIG_EXTENSIONS) {
            return null
        }
        return runCatching {
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@runCatching null
            val contributor = getContributorMethod!!.invoke(null, file) ?: return@runCatching null
            val definitions = parseDefinitions(contributor, psiFile)
            val imports = parseImports(contributor, project, file)
            if (definitions == null && imports == null) {
                null
            } else {
                BridgeDocument(definitions, imports)
            }
        }.getOrNull()
    }

    /**
     * Spring Boot 的 contributor 返回 `Pair<key, value>`，YAML 的 key 是
     * [YAMLKeyValue]，properties 的 key 是 `PropertyKeyImpl`。统一转换成
     * 与内置解析一致的扁平键、值和文件内偏移。
     */
    private fun parseDefinitions(contributor: Any, psiFile: PsiFile): List<BridgeDefinition>? {
        val method = processAllKeysMethod ?: return null
        val result = ArrayList<BridgeDefinition>()
        val processor = Processor<Any?> { pair ->
            runCatching { appendPair(pair, result) }
            true
        }
        val invoked = runCatching { method.invoke(contributor, psiFile, processor) }.getOrNull()
            ?: return null
        if (invoked !is Boolean) {
            return null
        }
        // contributor 倒序遍历，恢复正序，保证同一文件内重复键与内置解析一致
        Collections.reverse(result)
        return result
    }

    private fun parseImports(contributor: Any, project: Project, file: VirtualFile): List<String>? {
        val method = getImportStatementsMethod ?: return null
        val statements = runCatching { method.invoke(contributor, project, file) as? List<*> }
            .getOrNull() ?: return null
        return statements.mapNotNull { statement ->
            runCatching {
                val type = statement?.javaClass ?: return@runCatching null
                val text = type.getMethodOrNull("filePointerText", "getFilePointerText")
                    ?.invoke(statement) as? String
                text?.takeIf { it.isNotBlank() }
            }.getOrNull()
        }.flatMap { splitLocations(it) }.distinct()
    }

    private fun appendPair(pair: Any?, result: MutableList<BridgeDefinition>) {
        val platformPair = pair as? PlatformPair<*, *> ?: return
        val key = platformPair.first as? PsiElement ?: return
        val value = platformPair.second as? PsiElement
        when (key) {
            is YAMLKeyValue -> appendYamlKey(key, result)
            else -> appendPropertyKey(key, value, result)
        }
    }

    /**
     * YAML 键路径由 PSI 父链拼接。列表项（`- name: x`）不是配置键，
     * 块映射/块列表的父键也不产生可预览值，与内置解析保持一致。
     */
    private fun appendYamlKey(keyValue: YAMLKeyValue, result: MutableList<BridgeDefinition>) {
        if (PsiTreeUtil.getParentOfType(keyValue, YAMLSequenceItem::class.java) != null) {
            return
        }
        val key = yamlPath(keyValue) ?: return
        val keyElement = keyValue.key ?: return
        val valueElement = keyValue.value ?: return
        val value = yamlValueText(valueElement) ?: return
        result += BridgeDefinition(
            key = key,
            value = value,
            keyOffset = keyElement.textRange.startOffset,
            valueOffset = valueElement.textRange.startOffset
        )
    }

    private fun yamlPath(keyValue: YAMLKeyValue): String? {
        val parts = ArrayDeque<String>()
        var current: PsiElement? = keyValue
        while (current is YAMLKeyValue) {
            parts.addFirst(current.keyText)
            current = current.parent?.parent
        }
        return parts.joinToString(".").takeIf { it.isNotEmpty() }
    }

    private fun yamlValueText(value: PsiElement): String? = when (value) {
        is YAMLScalar -> value.text
            .takeUnless { it.startsWith("|") || it.startsWith(">") }
            ?.let { value.textValue }
        is YAMLMapping, is YAMLSequence -> value.text.takeUnless { it.contains('\n') }
        else -> value.text
    }

    private fun appendPropertyKey(
        key: PsiElement,
        value: PsiElement?,
        result: MutableList<BridgeDefinition>
    ) {
        if (key is IProperty) {
            val keyText = key.key ?: return
            val element = key.psiElement
            result += BridgeDefinition(
                key = keyText,
                value = key.value.orEmpty(),
                keyOffset = element.textRange.startOffset,
                valueOffset = element.textRange.endOffset
            )
            return
        }
        val keyText = key.text
        if (keyText.isBlank()) {
            return
        }
        result += BridgeDefinition(
            key = keyText,
            value = value?.text.orEmpty(),
            keyOffset = key.textRange.startOffset,
            valueOffset = value?.textRange?.startOffset ?: key.textRange.endOffset
        )
    }

    /** `spring.config.import` 的完整值可能包含逗号或 flow 列表。 */
    private fun splitLocations(value: String): List<String> =
        value.trim()
            .removePrefix("[")
            .removeSuffix("]")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun Class<*>.getMethodOrNull(name: String, vararg parameterTypes: Class<*>): Method? =
        runCatching { getMethod(name, *parameterTypes) }.getOrNull()

    private fun Class<*>.getMethodOrNull(vararg names: String): Method? =
        names.firstNotNullOfOrNull { name -> runCatching { getMethod(name) }.getOrNull() }

    private val CONFIG_EXTENSIONS = setOf("properties", "yml", "yaml")
}

/** 桥接解析出的单个键值定义；偏移相对于 [SpringPropertyIndex] 读取的 PSI 文本。 */
internal class BridgeDefinition(
    val key: String,
    val value: String,
    val keyOffset: Int,
    val valueOffset: Int
)

/**
 * 桥接结果。[definitions] / [imports] 为 null 表示当前 Spring Boot 版本
 * 不提供对应能力，调用方只需回退该部分。
 */
internal class BridgeDocument(
    val definitions: List<BridgeDefinition>?,
    val imports: List<String>?
)
