package com.luckyframework.idea.spel.yaml

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.environment.SpringPlaceholderUsageSearch
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * YAML 配置键反向跳转到 Java 注解里的 `${key}` 使用位置。
 *
 * 只在光标位于键上时提供目标，值位置交给 YAML/SpEL 原有跳转；
 * 嵌套键按完整点分路径匹配注解中的占位符。
 *
 * 在 `yaml-spel-support.xml` 中注册，主 jar 不依赖 YAML PSI。
 */
class SpringYamlPropertyGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        val element = sourceElement ?: return null
        val keyValue = element as? YAMLKeyValue
            ?: PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java, false)
            ?: return null
        val keyElement = keyValue.key ?: return null
        if (offset < keyElement.textRange.startOffset || offset > keyElement.textRange.endOffset) {
            return null
        }
        val key = SpringYamlKeyPaths.fullKeyOf(keyValue) ?: return null
        return SpringPlaceholderUsageSearch.declarationsOf(keyValue.project, key)
            .takeIf { it.isNotEmpty() }
            ?.toTypedArray()
    }
}
