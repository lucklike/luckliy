package com.luckyframework.idea.std.yaml

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.luckyframework.idea.std.StdHttpClientIndex
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * YAML 键引用：
 *
 * - `standard-client-configs.<configId>` → 接口；
 * - `...<configId>.method-configs.<apiId>` → 接口方法。
 *
 * 只在目标确实存在时返回引用，解析失败不会在编辑器中留下错误标记。
 */
class StdYamlReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val keyValue = element as? YAMLKeyValue ?: return PsiReference.EMPTY_ARRAY
        val key = keyValue.key ?: return PsiReference.EMPTY_ARRAY
        val file = keyValue.containingFile ?: return PsiReference.EMPTY_ARRAY
        val snapshot = StdHttpClientIndex.getInstance(file.project).snapshot()
        if (!snapshot.active) {
            return PsiReference.EMPTY_ARRAY
        }
        val range = rangeOf(keyValue, key)
        val segments = StdConfigLocator.segmentsOf(keyValue)
        if (segments.size == 4 && StdConfigLocator.isStdConfigPath(segments)) {
            val configId = segments[3]
            if (!snapshot.byConfigId.containsKey(configId)) {
                return PsiReference.EMPTY_ARRAY
            }
            return arrayOf(StdConfigIdReference(keyValue, range, configId))
        }
        if (segments.size == 6 &&
            StdConfigLocator.isStdConfigPath(segments) &&
            StdConfigLocator.isMethodConfigs(segments[4])
        ) {
            val configId = segments[3]
            val apiId = segments[5]
            val entries = snapshot.byConfigId[configId] ?: return PsiReference.EMPTY_ARRAY
            if (entries.none { entry -> entry.methods.any { it.apiId == apiId } }) {
                return PsiReference.EMPTY_ARRAY
            }
            return arrayOf(StdApiIdReference(keyValue, range, configId, apiId))
        }
        return PsiReference.EMPTY_ARRAY
    }

    /** 引用范围相对 [YAMLKeyValue]，只覆盖键文本并排除两侧引号。 */
    private fun rangeOf(keyValue: YAMLKeyValue, key: PsiElement): TextRange {
        val start = key.textRange.startOffset - keyValue.textRange.startOffset
        val text = key.text
        val quoted = text.length >= 2 &&
            ((text.first() == '"' && text.last() == '"') ||
                (text.first() == '\'' && text.last() == '\''))
        return if (quoted) {
            TextRange(start + 1, start + text.length - 1)
        } else {
            TextRange(start, start + text.length)
        }
    }
}
