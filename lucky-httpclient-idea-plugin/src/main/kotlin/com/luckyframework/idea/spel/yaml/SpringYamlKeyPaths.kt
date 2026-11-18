package com.luckyframework.idea.spel.yaml

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLSequenceItem

/**
 * YAML 键的完整点分路径。
 *
 * 嵌套写法的叶子键（`server:` 下的 `port:`）与单键写法的完整路径
 * （`server.port: 8080`）都得到 `server.port`，与 [com.luckyframework.idea.spel.environment.SpringPropertyIndex]
 * 扁平化后的键名保持一致。
 */
object SpringYamlKeyPaths {

    /**
     * 键在配置文件中的完整路径；列表元素里的键不是可配置项，返回 null。
     */
    fun fullKeyOf(keyValue: YAMLKeyValue): String? {
        val segments = ArrayList<String>()
        var current: PsiElement? = keyValue
        while (current != null) {
            when (current) {
                is YAMLKeyValue -> {
                    val key = current.keyText
                    if (key.isEmpty()) {
                        return null
                    }
                    segments += key
                }

                is YAMLSequenceItem -> return null

                is PsiFile -> break
            }
            current = current.parent
        }
        if (segments.isEmpty()) {
            return null
        }
        return segments.asReversed().joinToString(".")
    }
}
