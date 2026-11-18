package com.luckyframework.idea.std.yaml

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.std.StdConfigNode
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequenceItem

/**
 * YAML 侧标准客户端配置路径解析（YAML 插件存在时加载）。
 *
 * 运行期绑定路径固定为：
 * `lucky.http-client.standard-client-configs.<configId>[.method-configs.<apiId>]`。
 *
 * 固定属性名按 Spring Boot 的宽松绑定比较（忽略大小写、`-`、`_`），
 * `configId` / `apiId` 是 Map 的 key，按运行期语义精确匹配，仅去除引号与空白。
 * 多文档 YAML 通过遍历全部 [YAMLKeyValue] 并按祖先链拼接路径天然支持。
 */
object StdConfigLocator {

    private const val LUCKY = "lucky"
    private const val HTTP_CLIENT = "http-client"
    private const val STANDARD_CLIENT_CONFIGS = "standard-client-configs"
    private const val METHOD_CONFIGS = "method-configs"
    private const val URL = "url"
    private const val PATH = "path"
    private const val METHOD = "method"
    private const val NON = "NON"

    /**
     * 解析文件中的全部标准客户端配置节点。
     *
     * 只接受根级 4 段路径；出现在列表项中的键不是配置节点，直接忽略。
     */
    fun locate(file: PsiFile): List<StdConfigNode> {
        val result = mutableListOf<StdConfigNode>()
        for (keyValue in PsiTreeUtil.findChildrenOfType(file, YAMLKeyValue::class.java)) {
            val segments = segmentsOf(keyValue)
            if (segments.size != 4 || !isStdConfigPath(segments)) {
                continue
            }
            val configId = segments[3].trim()
            if (configId.isEmpty()) {
                continue
            }
            result.add(
                StdConfigNode(
                    keyValue,
                    configId,
                    methodConfigsOf(keyValue),
                    file,
                    baseUrlOf(keyValue),
                    methodPathsOf(keyValue),
                    httpMethodOf(keyValue),
                    apiMethodsOf(keyValue)
                )
            )
        }
        return result
    }

    /**
     * 从根到 [keyValue] 的原始路径段（已去引号与空白）。
     *
     * 祖先链中出现 [YAMLSequenceItem] 时返回空列表：列表项不是配置节点，
     * 不能把列表中的同名键误认为 `method-configs` 下的方法配置。
     */
    fun segmentsOf(keyValue: YAMLKeyValue): List<String> {
        val segments = ArrayDeque<String>()
        var current: PsiElement? = keyValue
        while (current != null) {
            if (current !is YAMLKeyValue) {
                return emptyList()
            }
            segments.addFirst(current.keyText)
            var parent: PsiElement? = current.parent
            while (parent != null && parent !is YAMLKeyValue && parent !is YAMLSequenceItem) {
                parent = parent.parent
            }
            if (parent is YAMLSequenceItem) {
                return emptyList()
            }
            current = parent
        }
        return segments.toList()
    }

    /** 路径前三段是否 `lucky.http-client.standard-client-configs`（宽松比较）。 */
    fun isStdConfigPath(segments: List<String>): Boolean =
        segments.size >= 3 &&
            isFixed(segments[0], LUCKY) &&
            isFixed(segments[1], HTTP_CLIENT) &&
            isFixed(segments[2], STANDARD_CLIENT_CONFIGS)

    /** 路径段是否是 `method-configs`（宽松比较）。 */
    fun isMethodConfigs(segment: String): Boolean = isFixed(segment, METHOD_CONFIGS)

    /**
     * 路径段归一化：去引号/空白、去 `-`/`_`、转小写。
     *
     * 只用于固定属性名比较，不能用于 `configId` / `apiId`。
     */
    fun normalize(segment: String): String =
        segment.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .replace("-", "")
            .replace("_", "")
            .lowercase()

    private fun isFixed(segment: String, expected: String): Boolean =
        normalize(segment) == normalize(expected)

    private fun methodConfigsOf(keyValue: YAMLKeyValue): Map<String, PsiElement> {
        val mapping = keyValue.value as? YAMLMapping ?: return emptyMap()
        val result = linkedMapOf<String, PsiElement>()
        for (child in mapping.keyValues) {
            if (!isMethodConfigs(child.keyText)) {
                continue
            }
            val methods = child.value as? YAMLMapping ?: continue
            for (method in methods.keyValues) {
                val apiId = method.keyText.trim()
                if (apiId.isNotEmpty()) {
                    result[apiId] = method
                }
            }
        }
        return result
    }

    /**
     * 各 apiId 的 `path` 配置（去引号）。
     *
     * `method-configs.<apiId>` 下没有 `path` 或不是标量时该 apiId 不出现。
     */
    private fun methodPathsOf(keyValue: YAMLKeyValue): Map<String, String> {
        val mapping = keyValue.value as? YAMLMapping ?: return emptyMap()
        val result = linkedMapOf<String, String>()
        for (child in mapping.keyValues) {
            if (!isMethodConfigs(child.keyText)) {
                continue
            }
            val methods = child.value as? YAMLMapping ?: continue
            for (method in methods.keyValues) {
                val apiId = method.keyText.trim()
                if (apiId.isEmpty()) {
                    continue
                }
                val config = method.value as? YAMLMapping ?: continue
                val path = config.keyValues
                    .firstOrNull { normalize(it.keyText) == PATH }
                    ?.value as? YAMLScalar ?: continue
                path.textValue.trim().takeIf { it.isNotEmpty() }?.let { result[apiId] = it }
            }
        }
        return result
    }

    /**
     * 各 apiId 的 `method` 配置（大写，`NON` 视为未配置）。
     *
     * `method-configs.<apiId>.method` 在运行期覆盖方法注解上的请求方法
     * （`StdHttpClientFunction#mergeConfig`），侧边栏需要展示实际生效的方法。
     */
    private fun apiMethodsOf(keyValue: YAMLKeyValue): Map<String, String> {
        val mapping = keyValue.value as? YAMLMapping ?: return emptyMap()
        val result = linkedMapOf<String, String>()
        for (child in mapping.keyValues) {
            if (!isMethodConfigs(child.keyText)) {
                continue
            }
            val methods = child.value as? YAMLMapping ?: continue
            for (method in methods.keyValues) {
                val apiId = method.keyText.trim()
                if (apiId.isEmpty()) {
                    continue
                }
                val config = method.value as? YAMLMapping ?: continue
                methodValueOf(config)?.let { result[apiId] = it }
            }
        }
        return result
    }

    /** 节点下的类级 `method` 配置；未配置或为 `NON` 时返回 null。 */
    private fun httpMethodOf(keyValue: YAMLKeyValue): String? =
        (keyValue.value as? YAMLMapping)?.let { methodValueOf(it) }

    /** 映射中的 `method` 属性：宽松匹配属性名，值统一为大写枚举名。 */
    private fun methodValueOf(mapping: YAMLMapping): String? {
        val child = mapping.keyValues.firstOrNull { normalize(it.keyText) == METHOD } ?: return null
        val value = child.value as? YAMLScalar ?: return null
        return value.textValue.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .trim()
            .uppercase()
            .takeIf { it.isNotEmpty() && it != NON }
    }

    /**
     * 节点下 `url` 属性的值：宽松匹配固定属性名，去除首尾引号与空白。
     *
     * 未配置或不是标量时返回 null（`service` 引用不一定是 URL，不参与）。
     */
    private fun baseUrlOf(keyValue: YAMLKeyValue): String? {
        val mapping = keyValue.value as? YAMLMapping ?: return null
        val child = mapping.keyValues.firstOrNull { normalize(it.keyText) == URL } ?: return null
        val value = child.value as? YAMLScalar ?: return null
        return value.textValue.trim().takeIf { it.isNotEmpty() }
    }
}
