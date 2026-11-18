package com.luckyframework.idea.spel.environment

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil

/**
 * properties 配置键反向跳转到 Java 注解里的 `${key}` 使用位置。
 *
 * 只在光标位于键名上时提供目标：值位置可能同时是 Properties 插件自己的
 * 引用，交给原有跳转处理。多个使用位置由平台弹出目标列表。
 *
 * 类在 `properties-spel-support.xml` 中注册，只有安装 Properties 插件
 * （IDEA 自带并默认启用）时才加载，避免硬依赖。
 */
class SpringPropertyGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        val element = sourceElement ?: return null
        val property = element as? Property
            ?: PsiTreeUtil.getParentOfType(element, Property::class.java, false)
            ?: return null
        val key = property.unescapedKey?.takeIf { it.isNotBlank() } ?: return null
        val identifier = (property as? PsiNameIdentifierOwner)?.nameIdentifier ?: return null
        if (offset < identifier.textRange.startOffset || offset > identifier.textRange.endOffset) {
            return null
        }
        return SpringPlaceholderUsageSearch.declarationsOf(property.project, key)
            .takeIf { it.isNotEmpty() }
            ?.toTypedArray()
    }
}
