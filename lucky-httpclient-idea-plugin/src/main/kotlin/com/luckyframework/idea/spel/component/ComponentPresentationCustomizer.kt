package com.luckyframework.idea.spel.component

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.project.Project
import javax.swing.Icon

/**
 * 补全列表渲染期的组件图标定制。
 *
 * 2026.3 起 `JavaPsiClassReferenceElement` 在构造时就把默认图标缓存进
 * `myIcon`，渲染时直接使用缓存，不再读取 `LookupItem.ICON_ATTR`；因此
 * 后置注入属性对类名补全无效。平台为此提供了渲染前回调
 * （`LookupImpl.addPresentationCustomizer`），这里在每次渲染时直接改写
 * 展示数据，不依赖候选对象身份、注入时机与 `refreshUi`。
 *
 * 渲染器缓存中的 presentation 在回调前已经 `freeze()`，冻结后的对象拒绝
 * 任何写入（`setIcon` 会抛 `IllegalStateException`），因此改动只能发生在
 * 复制出来的新对象上，原对象保持不动。
 *
 * 组件清单每次渲染动态读取：渲染早于注解扫描预热完成时保持默认图标，
 * 预热完成后的下一次重绘自然生效。名称索引见 [ComponentNames]，它按清单
 * 引用缓存，不会每个可见项重建集合。
 *
 * 整个回调**不允许访问 PSI**：2026.3 的绘制路径在
 * `RwLockHolder.runWithoutImplicitRead` 下执行，读取
 * `PsiClass.getQualifiedName()` 会直接抛 `Read access is allowed from inside
 * read-action only`，整行渲染随之失败。
 */
internal class ComponentPresentationCustomizer(private val project: Project) {

    fun customizePresentation(
        item: LookupElement,
        presentation: LookupElementPresentation
    ): LookupElementPresentation {
        if (!isComponent(item)) {
            return presentation
        }
        val icon = ComponentIconResolver.icon() ?: return presentation
        val customized = LookupElementPresentation()
        customized.copyFrom(presentation)
        customized.setIcon(icon)
        return customized
    }

    fun customizeEmptyIcon(emptyIcon: Icon): Icon = emptyIcon

    private fun isComponent(item: LookupElement): Boolean {
        val names = ComponentNameCache.names(project)
        return !names.isEmpty && names.matches(item)
    }
}
