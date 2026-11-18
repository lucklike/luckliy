package com.luckyframework.idea.spel.yaml

import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * 收集 YAML 元素所在的键路径。
 *
 * 列表项不产生路径段：`condition-body: [{body: ...}]` 里 `body` 的路径是
 * `condition-body.body`，与 `@SpelConfig` 模型的字段层级一致。
 * 键名统一去掉引号，与 `@SpelAttribute#key` 的写法对齐。
 */
object SpelYamlConfigPath {

    fun of(element: PsiElement): List<String> {
        val segments = ArrayList<String>()
        var current: PsiElement? = element
        while (current != null) {
            if (current is YAMLKeyValue) {
                val key = keyOf(current)
                if (key.isNotEmpty()) {
                    segments.add(0, key)
                }
            }
            current = current.parent
        }
        return segments
    }

    private fun keyOf(keyValue: YAMLKeyValue): String {
        val scalar = keyValue.key as? YAMLScalar
        return (scalar?.textValue ?: keyValue.keyText).trim()
    }
}
