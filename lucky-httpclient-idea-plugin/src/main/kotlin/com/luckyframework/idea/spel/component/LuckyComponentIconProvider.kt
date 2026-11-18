package com.luckyframework.idea.spel.component

import com.intellij.ide.IconProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import javax.swing.Icon

/**
 * `@LuckyComponent` 组件类在导航、结构视图、查找类等位置显示品牌图标。
 *
 * 组件身份来自 [SpelRuntimeSymbols] 的注解扫描，与补全/检查同源；扫描结果
 * 按 PSI 修改计数缓存，dumb 模式下返回空列表，不阻塞索引构建。
 *
 * 图标本身由 [ComponentIconResolver] 按设置解析：可在设置页关闭替换或指定
 * 自定义图标文件，未配置时使用插件内置图标。
 */
class LuckyComponentIconProvider : IconProvider() {

    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        val psiClass = element as? PsiClass ?: return null
        return if (isLuckyComponentClass(psiClass)) ComponentIconResolver.icon() else null
    }
}

/**
 * 类是否被声明为 `@LuckyComponent` 组件。
 *
 * 组件身份统一来自 [SpelRuntimeSymbols] 的注解扫描，PSI 级图标
 * （项目树、结构视图）与文件级图标（编辑器标签页）共用同一判定，
 * 保证同一组件在不同位置的图标一致。
 */
internal fun isLuckyComponentClass(psiClass: PsiClass): Boolean {
    val qualifiedName = psiClass.qualifiedName ?: return false
    return SpelRuntimeSymbols.components(psiClass.project)
        .any { it.className == qualifiedName }
}
