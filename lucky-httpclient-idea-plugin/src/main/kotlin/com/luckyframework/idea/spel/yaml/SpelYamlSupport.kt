package com.luckyframework.idea.spel.yaml

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * YAML 侧 SpEL 表达式识别（specs spel-yaml-configuration-support）。
 *
 * 只有「值位置」的标量才可能是表达式：键名、注释、数字/布尔/null 等
 * 非字符串标量一律排除。
 */
object SpelYamlSupport {

    fun scalarAt(element: PsiElement): YAMLScalar? = when (element) {
        is YAMLScalar -> element
        else -> PsiTreeUtil.getParentOfType(element, YAMLScalar::class.java, false)
    }

    fun isExpressionValue(scalar: YAMLScalar): Boolean {
        if (!scalar.isValid || !scalar.text.contains("#{")) {
            return false
        }
        return isValuePosition(scalar)
    }

    /**
     * 只含 `${...}` 环境变量占位符的值位置（可以不含 `#{}`）。
     *
     * 与 SpEL 表达式共用同一套位置判定；纯 `${...}` 由环境变量组件处理，
     * 是否激活由调用方按 starter 依赖再判断。
     */
    fun isPlaceholderValue(scalar: YAMLScalar): Boolean {
        if (!scalar.isValid || !scalar.text.contains("\${")) {
            return false
        }
        return isValuePosition(scalar)
    }

    private fun isValuePosition(scalar: YAMLScalar): Boolean {
        if (scalar.parent is PsiComment) {
            return false
        }
        val keyValue = scalar.parent as? YAMLKeyValue
        if (keyValue != null && keyValue.key === scalar) {
            return false
        }
        return isStringValue(scalar)
    }

    /** 带引号的标量都是字符串；plain 标量排除数字、布尔与 null。 */
    fun isStringValue(scalar: YAMLScalar): Boolean {
        val text = scalar.text.trim()
        if (text.length >= 2 &&
            ((text.first() == '"' && text.last() == '"') ||
                (text.first() == '\'' && text.last() == '\''))
        ) {
            return true
        }
        return !NON_STRING_PLAIN.matches(text)
    }

    fun expressionScalars(file: PsiFile): List<YAMLScalar> =
        PsiTreeUtil.findChildrenOfType(file, YAMLScalar::class.java)
            .filter { isExpressionValue(it) }

    private val NON_STRING_PLAIN = Regex(
        "(?i)^(null|true|false|yes|no|on|off|~|[-+]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][-+]?\\d+)?)$"
    )
}
